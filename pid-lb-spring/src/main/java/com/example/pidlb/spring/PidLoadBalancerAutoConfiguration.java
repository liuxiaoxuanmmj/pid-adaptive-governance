package com.example.pidlb.spring;

import com.example.pidlb.core.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import io.micrometer.core.instrument.MeterRegistry;
import com.example.pidlb.metrics.MicrometerRtRecorder;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;

/**
 * 自动配置：提供 WeightEngine 与 PidReactorLoadBalancer 基本实例。
 */
@AutoConfiguration
@EnableConfigurationProperties(PidLbProperties.class)
public class PidLoadBalancerAutoConfiguration {

    @Bean
    public WeightEngine weightEngine(PidLbProperties props) {
        return new WeightEngine(
                props.wCpu, props.wMem, props.wRt,
                props.loadSetpoint,
                props.rtSloMs,
                props.sampleSeconds,
                props.wMin,
                props.wMax,
                true);
    }

    @Bean
    public ReactorLoadBalancer<?> pidReactorLoadBalancer(ServiceInstanceListSupplier supplier,
            WeightEngine engine,
            PidLbProperties props) {
        return new PidReactorLoadBalancer(props.serviceId, supplier, engine);
    }

    @Bean
    @Primary
    public MicrometerRtRecorder micrometerRtRecorder(MeterRegistry registry) {
        return new MicrometerRtRecorder(registry, "pid.lb.rt");
    }
}
