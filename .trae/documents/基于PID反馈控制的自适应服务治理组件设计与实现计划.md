## 目标与范围
- 实现一个可插拔的 Java 组件，通过 PID 控制理论对各服务实例的流量权重进行动态调度，输入为 CPU/内存/RT 指标，输出为实例权重。
- 支持积分分离解决冷启动过载、软启动线性爬坡、抗积分饱和，保障鲁棒性与可控性。
- 提供 Spring 生态集成（Spring Cloud LoadBalancer / Gateway / Feign / RestTemplate），以及度量采集（Micrometer/Actuator 或自定义端点）。

## 架构总览
- pid-lb-core：控制算法与权重引擎（PID、积分分离、抗积分饱和、软启动、权重归一化）。
- pid-lb-metrics：指标采集与归一化（CPU/内存/RT），支持本地 MXBean/OSHI、远程 Actuator、自定义 /lb/metrics。
- pid-lb-spring：Spring Cloud LoadBalancer 集成，提供 ReactorLoadBalancer 与 ServiceInstanceListSupplier 扩展。
- pid-lb-gateway（可选）：Spring Cloud Gateway 侧的 GlobalFilter/GatewayFilterFactory 实现服务端路由权重。
- pid-lb-demo：示例与基准测试，含仿真节点、指标回放、参数调优脚本。

## 控制算法设计
- 负载指标归一化：
  - cpu ∈ [0,1]，mem ∈ [0,1]
  - rt_norm = min(1, rt_ms / rt_slo_ms)
  - load = w_cpu * cpu + w_mem * mem + w_rt * rt_norm（w_* 为可配置权重，和为 1）
- 设定值：`load_setpoint`（如 0.7），期望各实例负载不超过设定值且尽量均衡。
- 误差与控制：对每实例 i，e_i = load_setpoint − load_i；输出为权重增量 Δw_i。
- PID（带采样周期 Ts）：
  - p = Kp * e_i
  - i = i_prev + Ki * e_i * Ts（积分分离与抗饱和后）
  - d = Kd * (e_i − e_prev) / Ts（对误差应用 EMA 滤波）
  - u = p + i + d
  - w_i = clamp(w_i_prev + u, w_min, w_max)
  - 归一化：所有 w_i ≥ w_min 后执行 normalize，使 Σw_i = 1

## 积分分离与软启动
- 冷启动判定：实例启动时间 < soft_start_duration 或 |e_i| > integral_separation_threshold。
- 冷启动阶段：禁用积分项，仅使用比例项，w_i 从 `soft_start_initial_weight` 线性爬坡至正常区间。
- 退出条件：满足时间与误差双条件后恢复积分项。

## 抗积分饱和（Anti-windup）
- 输出限幅：w_i ∈ [w_min, w_max]，且全局归一化。
- 积分夹持：i ∈ [i_min, i_max]，防止过度积累。
- 反算回馈：若 u 超出输出限幅，执行 i ← i + K_aw * (w_sat − u) 将饱和误差回馈到积分器。

## 指标采集与平滑
- 采集通道：
  - 本地：`OperatingSystemMXBean`/`MemoryMXBean`/OSHI 获取 CPU/内存；Micrometer Timer 记录 RT。
  - 远程：拉取各实例 Actuator 或自定义 `/lb/metrics`（返回 cpu/mem/rt 的滑窗统计）。
- 平滑与降噪：对每通道应用 EMA（α 可配）；导数项对 e_i 使用 EMA 导数以抑制 RT 抖动。

## 权重选择与路由
- 选择器：加权轮询/轮盘赌（Roulette）按 w_i 选择实例；支持强制熔断实例权重为 0。
- 更新周期：`update_interval_ms` 定时重新计算权重；选择逻辑读取最近稳定的权重快照，避免竞争。

## 并发与一致性
- NodeState 持有 {metrics, pid_state, weight}，更新在单线程调度器执行。
- 路由读取走快照（AtomicReference<Map<id, weight>>），更新采用 CAS 替换整体映射，避免读写竞争。

## 降级与回退策略
- 指标不可用：节点数据缺失时按最近权重保持或降权至 `degrade_weight`，并记录告警。
- 集群异常：全部指标不可用时回退至平滑的 round-robin，并维持限流上限。
- 熔断：失败率/RT 爆表触发熔断窗口，权重暂置 0，逐步探测恢复（半开，权重小幅提升）。

## 配置项（建议默认）
- pid: {kp: 0.25, ki: 0.05, kd: 0.1, sample_ms: 500}
- setpoint: {load: 0.7}
- weights: {min: 0.02, max: 0.8, normalize: true}
- integral_separation: {threshold: 0.3}
- soft_start: {initial_weight: 0.02, duration_ms: 30000}
- anti_windup: {method: backcalc, k_aw: 0.5, i_min: −1.0, i_max: 1.0}
- metrics: {rt_slo_ms: 200, ema_alpha: 0.3, poll_ms: 500}
- degrade: {weight: 0.05, mode: hold_last_or_rr}

## Spring 集成点
- Spring Cloud LoadBalancer：
  - 自定义 `ReactorLoadBalancer<ServiceInstance>`：读取权重快照执行加权选择。
  - 自定义 `ServiceInstanceListSupplier`：注入实例度量采集与可用性判断。
  - 通过 `@ConfigurationProperties` 暴露所有参数，支持热更新。
- Feign/RestTemplate：复用上层负载均衡器；提供 `RequestInterceptor` 以打点 RT。
- Spring Cloud Gateway（可选）：在 `GlobalFilter` 内按权重选择下游实例或设置 `FilterArgs`。

## 关键类设计
- PidController：`compute(e, Ts)` 返回 u，并维护 p/i/d 与 anti-windup。
- IntegralSeparationPolicy：判定是否启用积分、输出软启动权重目标。
- NodeMetrics：`cpu, mem, rt_ms` + EMA 缓存；NodeState：`metrics, pidState, weight, coldStartFlag`。
- WeightEngine：调度周期内迭代所有节点，计算 Δw_i、限幅与归一化，输出快照。
- MetricsCollector：本地/远程双实现；RtTimer：Micrometer `Timer`/`DistributionSummary`。
- LoadBalancerSelector：轮盘赌选择器，支持熔断与降级。

## 伪代码（核心逻辑）
```java
u = pid.compute(e_i, Ts);
if (isColdStart(i) || abs(e_i) > sepThreshold) {
  u = Kp * e_i; // disable integral, derivative可保留
}
w_i = clamp(w_i_prev + u, w_min, w_max);
// anti-windup back-calculation
if (w_i != w_i_prev + u) { pid.integral += K_aw * (w_i - (w_i_prev + u)); }
normalize(weights);
```

## 测试与验证
- 单元测试：
  - 阶跃响应测试：不同 Kp/Ki/Kd 下的收敛速度与过冲。
  - 饱和与反算回馈：输出限幅场景下积分器不再发散。
  - 积分分离：冷启动/大误差阶段积分项被切除，软启动权重线性增长。
- 集成测试：
  - 仿真 3–10 个节点的指标流（CPU/内存/RT 随时间波动），观察权重轨迹与负载均衡效果。
  - Micrometer 采集与加权选择链路打通（Feign/RestTemplate/Gateway）。

## 交付物结构
- 源码模块与示例工程。
- `application.yml` 配置模板与参数说明。
- 指标端点规范 `/lb/metrics`（JSON：cpu/mem/rt，含窗口大小与时间戳）。
- 指标回放脚本与图表（JMH/JavaFX/CSV+Python 绘图均可）。

## 实施步骤
1. 搭建 pid-lb-core 与 PidController，完成积分分离与抗饱和。
2. 实现 MetricsCollector（本地与远程）与归一化逻辑，打通 Micrometer。
3. 编写 WeightEngine 与快照机制，完成加权选择器。
4. 接入 Spring Cloud LoadBalancer（客户端），完成 Feign/RestTemplate 集成与 RT 打点。
5. 可选：实现 Gateway 侧过滤器，服务端统一路由权重。
6. 编写单元与集成测试，提供参数调优指引与默认配置。

## 参数调优建议
- 先固定 Ki=0、Kd=0，仅调 Kp 获得稳定比例控制；再逐步引入 Ki 以消除静差，最后引入小幅 Kd 抑制过冲。
- 负载权重 w_cpu/w_mem/w_rt 基于瓶颈资源设定；RT 建议按 SLO 归一化后纳入。
