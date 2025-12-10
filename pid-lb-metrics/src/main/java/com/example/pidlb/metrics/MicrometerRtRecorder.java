package com.example.pidlb.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * Micrometer RT 记录器：用于记录请求耗时并在采集时提供最近RT统计。
 */
public class MicrometerRtRecorder {
    private final Timer rtTimer;

    public MicrometerRtRecorder(MeterRegistry registry, String name) {
        this.rtTimer = Timer.builder(name).register(registry);
    }

    public void record(long durationMs) {
        rtTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public double recentRtMs() {
        double count = rtTimer.count();
        if (count <= 0) return 0.0;
        return rtTimer.mean(TimeUnit.MILLISECONDS);
    }
}

