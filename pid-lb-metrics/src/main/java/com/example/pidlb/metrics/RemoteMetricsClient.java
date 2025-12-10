package com.example.pidlb.metrics;

import com.example.pidlb.core.NodeMetrics;

import java.util.Optional;

/**
 * 远程度量拉取客户端接口：从实例公开的端点拉取 cpu/mem/rt 指标。
 */
public interface RemoteMetricsClient {
    Optional<NodeMetrics> fetch(String instanceId, String host, int port);
}

