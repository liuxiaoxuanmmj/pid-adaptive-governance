package com.example.pidlb.metrics;

import com.example.pidlb.core.NodeMetrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * 使用 MXBean 采集本地 CPU/内存 的粗略指标并归一化为 [0,1]。
 */
public class SystemMetricsCollector {
    public NodeMetrics collect(double rtMs) {
        double cpu = readCpuLoad();
        double mem = readMemLoad();
        return new NodeMetrics(cpu, mem, rtMs);
    }

    private double readMemLoad() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        long used = heap.getUsed();
        long max = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
        if (max <= 0) return 0.0;
        double ratio = (double) used / (double) max;
        return ratio < 0 ? 0.0 : Math.min(ratio, 1.0);
    }

    @SuppressWarnings("restriction")
    private double readCpuLoad() {
        try {
            com.sun.management.OperatingSystemMXBean os =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double load = os.getSystemCpuLoad();
            if (Double.isNaN(load)) return 0.0;
            return Math.max(0.0, Math.min(load, 1.0));
        } catch (Throwable t) {
            return 0.0;
        }
    }
}

