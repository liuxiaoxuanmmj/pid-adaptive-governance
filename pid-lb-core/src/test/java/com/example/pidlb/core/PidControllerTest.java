package com.example.pidlb.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PID 控制器基本测试：阶跃响应与抗积分饱和。
 */
public class PidControllerTest {
    @Test
    void stepResponseConverges() {
        PidController pid = new PidController(0.3, 0.05, 0.1,
                AntiWindupMode.BACK_CALCULATION, 0.5, -1.0, 1.0);
        double w = 0.1;
        double set = 0.7;
        double load = 0.2;
        double dt = 0.5;
        for (int k = 0; k < 50; k++) {
            double e = set - load;
            double u = pid.compute(e, dt, true);
            double wUnsat = w + u;
            double wSat = clamp(wUnsat, 0.02, 0.8);
            pid.applyAntiWindup(wUnsat, wSat);
            w = wSat;
            // 简单负载模型：权重增加会提高负载（近似），用于测试收敛趋势
            load = Math.min(1.0, Math.max(0.0, load + 0.3 * (w - 0.1)));
        }
        assertTrue(w > 0.1);
    }

    @Test
    void antiWindupPreventsIntegratorBlowup() {
        PidController pid = new PidController(0.0, 1.0, 0.0,
                AntiWindupMode.BACK_CALCULATION, 0.5, -0.5, 0.5);
        double w = 0.5;
        double dt = 0.5;
        // 长期大误差导致输出饱和时，积分项应被限制
        for (int k = 0; k < 100; k++) {
            double e = 1.0; // 大误差
            double u = pid.compute(e, dt, true);
            double wUnsat = w + u;
            double wSat = clamp(wUnsat, 0.02, 0.3); // 强限制
            pid.applyAntiWindup(wUnsat, wSat);
            w = wSat;
        }
        assertTrue(Math.abs(pid.getI()) <= 0.5);
    }

    private static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }
}

