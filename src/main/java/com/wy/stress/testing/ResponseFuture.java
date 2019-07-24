package com.wy.stress.testing;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.netty.handler.codec.http.HttpResponse;

public class ResponseFuture implements Future<HttpResponse> {

    private boolean done = false;
    private HttpResponse response;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public HttpResponse get() throws InterruptedException, ExecutionException {
        return response;
    }

    @Override
    public HttpResponse get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public void done(HttpResponse response) {
        this.response = response;
        this.done = true;
    }
}
