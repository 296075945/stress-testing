package com.wy.stress.testing.report;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public interface Report {

    void report(HttpRequest request, HttpResponse response, long rtt, boolean success);

}
