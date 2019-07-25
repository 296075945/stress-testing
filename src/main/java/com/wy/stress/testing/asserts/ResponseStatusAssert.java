package com.wy.stress.testing.asserts;

import java.util.Arrays;
import java.util.List;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ResponseStatusAssert implements Assert {

    private List<HttpResponseStatus> status;

    public ResponseStatusAssert(HttpResponseStatus... status) {
        this.status = Arrays.asList(status);
    }

    public ResponseStatusAssert() {
        this(HttpResponseStatus.OK);
    }

    @Override
    public boolean check(HttpRequest request, HttpResponse response) {
        HttpResponseStatus responseStatus = response.getStatus();

        if (status.contains(responseStatus)) {
            return true;
        } else {
            return false;
        }

    }

}
