package com.example.pidlb.core;

import java.time.Instant;

/**
 * 节点状态：包含最新度量、PID状态、权重与冷启动标记。
 */
public class NodeState {
    private final String id;
    private NodeMetrics metrics;
    private double weight;
    private final PidController pid;
    private final IntegralSeparationPolicy sepPolicy;
    private final long startEpochMs;

    public NodeState(String id,
                     NodeMetrics metrics,
                     double initialWeight,
                     PidController pid,
                     IntegralSeparationPolicy sepPolicy) {
        this.id = id;
        this.metrics = metrics;
        this.weight = initialWeight;
        this.pid = pid;
        this.sepPolicy = sepPolicy;
        this.startEpochMs = Instant.now().toEpochMilli();
    }

    public String getId() { return id; }
    public NodeMetrics getMetrics() { return metrics; }
    public void setMetrics(NodeMetrics m) { this.metrics = m; }
    public double getWeight() { return weight; }
    public void setWeight(double w) { this.weight = w; }

    public boolean shouldDisableIntegral(double error) {
        long uptime = Instant.now().toEpochMilli() - startEpochMs;
        return sepPolicy.shouldDisableIntegral(error, uptime);
    }

    public PidController getPid() { return pid; }
}

