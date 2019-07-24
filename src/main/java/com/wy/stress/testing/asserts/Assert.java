package com.wy.stress.testing.asserts;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public interface Assert {

    boolean check(HttpRequest request, HttpResponse response);
}
