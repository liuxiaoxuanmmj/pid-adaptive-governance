package com.example.pidlb.spring;

import com.example.pidlb.metrics.MicrometerRtRecorder;
import feign.Client;
import feign.Request;
import feign.Response;
import feign.Request.Options;

import java.io.IOException;

/**
 * Feign Client 包装器：记录请求往返耗时。
 */
public class RtFeignClient implements Client {
    private final Client delegate;
    private final MicrometerRtRecorder recorder;

    public RtFeignClient(Client delegate, MicrometerRtRecorder recorder) {
        this.delegate = delegate;
        this.recorder = recorder;
    }

    @Override
    public Response execute(Request request, Options options) throws IOException {
        long start = System.nanoTime();
        Response resp = delegate.execute(request, options);
        long end = System.nanoTime();
        long ms = (end - start) / 1_000_000L;
        recorder.record(ms);
        return resp;
    }
}
