package com.example.pidlb.core;

/**
 * 积分分离策略：根据冷启动状态或误差阈值决定是否启用积分。
 */
public class IntegralSeparationPolicy {
    private final double errorThreshold;
    private final long softStartDurationMs;

    public IntegralSeparationPolicy(double errorThreshold, long softStartDurationMs) {
        this.errorThreshold = errorThreshold;
        this.softStartDurationMs = softStartDurationMs;
    }

    public boolean shouldDisableIntegral(double error, long uptimeMs) {
        if (uptimeMs < softStartDurationMs) {
            return true;
        }
        return Math.abs(error) > errorThreshold;
    }
}

