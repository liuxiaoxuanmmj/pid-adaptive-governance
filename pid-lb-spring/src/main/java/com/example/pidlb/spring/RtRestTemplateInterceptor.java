package com.example.pidlb.spring;

import com.example.pidlb.metrics.MicrometerRtRecorder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate RT 拦截器：记录请求往返耗时。
 */
public class RtRestTemplateInterceptor implements ClientHttpRequestInterceptor {
    private final MicrometerRtRecorder recorder;

    public RtRestTemplateInterceptor(MicrometerRtRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long start = System.nanoTime();
        ClientHttpResponse resp = execution.execute(request, body);
        long end = System.nanoTime();
        long ms = (end - start) / 1_000_000L;
        recorder.record(ms);
        return resp;
    }
}

