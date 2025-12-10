package com.example.pidlb.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 权重调度引擎：周期性根据负载计算权重增量，并输出快照。
 */
public class WeightEngine {
    private final Map<String, NodeState> nodes = new LinkedHashMap<>();
    private final AtomicReference<Map<String, Double>> weightSnapshot = new AtomicReference<>(Collections.emptyMap());

    private final double wCpu;
    private final double wMem;
    private final double wRt;
    private final double loadSetpoint;
    private final double rtSloMs;
    private final double dtSec;
    private final double wMin;
    private final double wMax;
    private final boolean normalize;

    public WeightEngine(double wCpu, double wMem, double wRt,
                        double loadSetpoint,
                        double rtSloMs,
                        double dtSec,
                        double wMin, double wMax,
                        boolean normalize) {
        if (Math.abs((wCpu + wMem + wRt) - 1.0) > 1e-6) {
            throw new IllegalArgumentException("wCpu + wMem + wRt must equal 1.0");
        }
        this.wCpu = wCpu; this.wMem = wMem; this.wRt = wRt;
        this.loadSetpoint = loadSetpoint;
        this.rtSloMs = rtSloMs;
        this.dtSec = dtSec;
        this.wMin = wMin; this.wMax = wMax; this.normalize = normalize;
    }

    public void upsertNode(NodeState node) {
        nodes.put(node.getId(), node);
    }

    public void removeNode(String id) { nodes.remove(id); }

    /**
     * 执行一次权重计算并更新快照。
     */
    public void step() {
        double sumWeights = 0.0;
        for (NodeState ns : nodes.values()) {
            double load = wCpu * ns.getMetrics().getCpu()
                    + wMem * ns.getMetrics().getMem()
                    + wRt * ns.getMetrics().rtNorm(rtSloMs);
            double e = loadSetpoint - load;
            boolean integralEnabled = !ns.shouldDisableIntegral(e);
            double u = ns.getPid().compute(e, dtSec, integralEnabled);
            double wUnsat = ns.getWeight() + u;
            double wSat = clamp(wUnsat, wMin, wMax);
            ns.getPid().applyAntiWindup(wUnsat, wSat);
            ns.setWeight(wSat);
            sumWeights += ns.getWeight();
        }

        if (normalize && sumWeights > 0) {
            for (NodeState ns : nodes.values()) {
                ns.setWeight(ns.getWeight() / sumWeights);
            }
        }

        Map<String, Double> snap = new LinkedHashMap<>();
        for (NodeState ns : nodes.values()) {
            snap.put(ns.getId(), ns.getWeight());
        }
        weightSnapshot.set(Collections.unmodifiableMap(snap));
    }

    public Map<String, Double> getSnapshot() {
        return weightSnapshot.get();
    }

    private static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(max, x));
    }
}

