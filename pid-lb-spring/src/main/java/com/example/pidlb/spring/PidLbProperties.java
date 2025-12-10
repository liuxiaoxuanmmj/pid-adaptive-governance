package com.example.pidlb.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 负载均衡器配置属性。
 */
@ConfigurationProperties(prefix = "pid.lb")
public class PidLbProperties {
    public String serviceId = "service";
    public double wCpu = 0.34;
    public double wMem = 0.33;
    public double wRt = 0.33;
    public double loadSetpoint = 0.7;
    public double rtSloMs = 200.0;
    public double sampleSeconds = 0.5;
    public double wMin = 0.02;
    public double wMax = 0.8;
}

