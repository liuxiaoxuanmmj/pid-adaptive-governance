package com.example.pidlb.core;

/**
 * 标准 PID 控制器，支持积分分离与抗积分饱和。
 */
public class PidController {
    private final double kp;
    private final double ki;
    private final double kd;
    private final AntiWindupMode antiWindupMode;
    private final double kaw;
    private final double iMin;
    private final double iMax;

    private double p;
    private double i;
    private double d;
    private double prevError;
    private boolean prevErrorInitialized;

    public PidController(double kp, double ki, double kd,
                         AntiWindupMode antiWindupMode,
                         double kaw,
                         double iMin,
                         double iMax) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.antiWindupMode = antiWindupMode;
        this.kaw = kaw;
        this.iMin = iMin;
        this.iMax = iMax;
        this.prevErrorInitialized = false;
    }

    /**
     * 计算控制输出。
     * @param error 误差 e
     * @param dt    采样周期（秒）
     * @param integralEnabled 是否启用积分项（积分分离）
     * @return 控制量 u
     */
    public double compute(double error, double dt, boolean integralEnabled) {
        // 比例项
        p = kp * error;

        // 积分项（积分分离）
        if (integralEnabled) {
            i += ki * error * dt;
            // 积分夹持
            if (i > iMax) i = iMax;
            if (i < iMin) i = iMin;
        }

        // 微分项（误差导数）
        if (prevErrorInitialized) {
            d = kd * (error - prevError) / dt;
        } else {
            d = 0.0;
            prevErrorInitialized = true;
        }
        prevError = error;

        return p + i + d;
    }

    /**
     * 抗积分饱和反算回馈：当输出被限幅时回馈到积分器。
     * @param unsaturated 原始输出
     * @param saturated   限幅后的输出
     */
    public void applyAntiWindup(double unsaturated, double saturated) {
        if (antiWindupMode == AntiWindupMode.BACK_CALCULATION) {
            double delta = saturated - unsaturated;
            i += kaw * delta;
            if (i > iMax) i = iMax;
            if (i < iMin) i = iMin;
        }
        // CLAMP 模式已在 compute 中通过积分夹持实现
    }

    public double getP() { return p; }
    public double getI() { return i; }
    public double getD() { return d; }
}

