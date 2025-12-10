package com.example.pidlb.spring;

import com.example.pidlb.core.LoadBalancerSelector;
import com.example.pidlb.core.WeightEngine;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * 基于权重快照的 ReactorLoadBalancer 实现，使用轮盘赌选择实例。
 */
public class PidReactorLoadBalancer implements ReactorLoadBalancer<ServiceInstance> {
    private final String serviceId;
    private final ServiceInstanceListSupplier supplier;
    private final WeightEngine engine;
    private final LoadBalancerSelector selector = new LoadBalancerSelector();

    public PidReactorLoadBalancer(String serviceId,
                                  ServiceInstanceListSupplier supplier,
                                  WeightEngine engine) {
        this.serviceId = serviceId;
        this.supplier = supplier;
        this.engine = engine;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        return supplier.get().next().map(instances -> {
            Map<String, Double> weights = engine.getSnapshot();
            if (instances.isEmpty() || weights.isEmpty()) {
                return new EmptyResponse();
            }
            Map<String, ServiceInstance> byId = new LinkedHashMap<>();
            for (ServiceInstance si : instances) {
                String id = si.getInstanceId() != null ? si.getInstanceId() : si.getHost() + ":" + si.getPort();
                byId.put(id, si);
            }
            String chosenId = selector.select(filterWeights(weights, byId.keySet()));
            ServiceInstance chosen = chosenId != null ? byId.get(chosenId) : null;
            if (chosen == null) {
                return new EmptyResponse();
            }
            return new DefaultResponse(chosen);
        });
    }

    private Map<String, Double> filterWeights(Map<String, Double> weights, Set<String> allowed) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : weights.entrySet()) {
            if (allowed.contains(e.getKey())) {
                m.put(e.getKey(), e.getValue());
            }
        }
        return m;
    }
}

