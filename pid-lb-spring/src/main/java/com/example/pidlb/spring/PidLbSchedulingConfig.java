package com.example.pidlb.spring;

import com.example.pidlb.core.WeightEngine;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 调度配置：周期性推进权重计算。
 */
@AutoConfiguration
@EnableScheduling
public class PidLbSchedulingConfig {
    @Bean
    public WeightStepper weightStepper(WeightEngine engine) {
        return new WeightStepper(engine);
    }

    public static class WeightStepper {
        private final WeightEngine engine;
        public WeightStepper(WeightEngine engine) { this.engine = engine; }

        @Scheduled(fixedDelayString = "${pid.lb.update-ms:500}")
        public void step() { engine.step(); }
    }
}

