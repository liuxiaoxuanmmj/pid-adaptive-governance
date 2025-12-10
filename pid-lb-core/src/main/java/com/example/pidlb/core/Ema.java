package com.example.pidlb.core;

/**
 * 指数加权移动平均（EMA）滤波器，用于对指标与误差进行平滑。
 */
public class Ema {
    private final double alpha;
    private double value;
    private boolean initialized;

    public Ema(double alpha) {
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be in (0,1]");
        }
        this.alpha = alpha;
        this.initialized = false;
    }

    public double update(double x) {
        if (!initialized) {
            value = x;
            initialized = true;
        } else {
            value = alpha * x + (1 - alpha) * value;
        }
        return value;
    }

    public double get() {
        return value;
    }

    public boolean isInitialized() {
        return initialized;
    }
}

