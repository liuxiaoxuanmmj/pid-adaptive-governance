package com.example.pidlb.core;

import java.util.Map;
import java.util.Random;

/**
 * 轮盘赌选择器：根据权重快照随机选择实例。
 */
public class LoadBalancerSelector {
    private final Random random = new Random();

    public String select(Map<String, Double> weights) {
        double sum = 0.0;
        for (double w : weights.values()) sum += w;
        if (sum <= 0.0 || weights.isEmpty()) return null;
        double r = random.nextDouble() * sum;
        double acc = 0.0;
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            acc += e.getValue();
            if (r <= acc) return e.getKey();
        }
        return weights.keySet().iterator().next();
    }
}

