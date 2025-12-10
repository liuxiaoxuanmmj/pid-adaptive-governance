package com.example.pidlb.gateway;

import com.example.pidlb.core.WeightEngine;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

/**
 * 简化的权重过滤器占位：可根据权重快照进行自定义路由选择（需结合动态路由）。
 */
public class PidWeightGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    private final WeightEngine engine;
    public PidWeightGatewayFilterFactory(WeightEngine engine) { this.engine = engine; }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            Map<String, Double> snap = engine.getSnapshot();
            // 这里可以读取权重并选择下游实例，示例中直接透传。
            return chain.filter(exchange);
        };
    }
}

