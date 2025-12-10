package com.example.pidlb.core;

/**
 * 节点度量（已归一化）：cpu/mem ∈ [0,1]，rt_ms 原始值，同时提供 rt 归一化方法。
 */
public class NodeMetrics {
    private final double cpu;      // [0,1]
    private final double mem;      // [0,1]
    private final double rtMs;     // 原始RT毫秒

    public NodeMetrics(double cpu, double mem, double rtMs) {
        this.cpu = clamp01(cpu);
        this.mem = clamp01(mem);
        this.rtMs = Math.max(0.0, rtMs);
    }

    public double getCpu() { return cpu; }
    public double getMem() { return mem; }
    public double getRtMs() { return rtMs; }

    public double rtNorm(double sloMs) {
        if (sloMs <= 0) return 1.0;
        double r = rtMs / sloMs;
        return r > 1.0 ? 1.0 : (r < 0.0 ? 0.0 : r);
    }

    private static double clamp01(double x) {
        if (x < 0.0) return 0.0;
        if (x > 1.0) return 1.0;
        return x;
    }
}

