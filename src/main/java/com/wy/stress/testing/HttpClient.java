package com.wy.stress.testing;

import java.net.URI;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;

public class HttpClient {

    private AtomicBoolean init = new AtomicBoolean(false);

//    private int capacity;

    private LinkedList<Channel> free;

    private Map<Channel, ChannelRequestWrapper> channelMap;
    Bootstrap bootstrap;

    final Semaphore permit;
    private String host;
    private int port;

    public HttpClient(int capacity, String host, int port) {
        permit = new Semaphore(capacity);
//        this.capacity = capacity;
        this.host = host;
        this.port = port;
        free = new LinkedList<>();

        channelMap = new ConcurrentHashMap<>((int) (capacity / 0.75));
    }

    private Channel getChannel() {

        try {
            permit.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        synchronized (free) {
            if (free.size() > 0) {
                return free.removeFirst();
            }
        }
        try {
            return bootstrap.connect(host, port).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
            permit.release();
            return null;
        }
    }

    private void freeChannel(Channel channel) {
        permit.release();
        synchronized (free) {
            free.add(channel);
        }
    }

    public Future<HttpResponse> post(String url, Map<String, Object> headers, String body) throws Exception {

        URI uri = new URI(url);
        ByteBuf content = (body != null && body.length() > 0) ? Unpooled.wrappedBuffer(body.getBytes())
                : Unpooled.buffer(0);
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri.toASCIIString(),
                content);

        request.headers().set(HttpHeaders.Names.HOST, uri.getHost() + ":" + uri.getPort());
        request.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, content.capacity());
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                request.headers().set(entry.getKey(), entry.getValue());
            }
        }
        return submit(request);

    }

    private Future<HttpResponse> submit(FullHttpRequest request) {
        if (init.compareAndSet(false, true)) {
            init();
        }
        request.getUri();

        Channel channel = getChannel();
        if (channel != null) {
            channel.writeAndFlush(request);
            ResponseFuture responseFuture = new ResponseFuture();
            channelMap.put(channel, new ChannelRequestWrapper(request, responseFuture, System.currentTimeMillis()));
            return responseFuture;
        } else {
            System.err.println("no connect");
            return null;
        }
    }

    private void init() {
        bootstrap = new Bootstrap();

        EventLoopGroup group = new NioEventLoopGroup(4);

        bootstrap.group(group);

        bootstrap.channel(NioSocketChannel.class);

        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.handler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new HttpClientCodec());

                pipeline.addLast(new HttpObjectAggregator(1024 * 10 * 1024));

                pipeline.addLast(new HttpContentDecompressor());

                pipeline.addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
                        Channel channel = ctx.channel();
//                        FullHttpRequest request = channelMap.remove(channel);
                        ChannelRequestWrapper requestWrapper = channelMap.remove(channel);
                        HttpRequest request = requestWrapper.getRequest();
                        ResponseFuture responseFuture = requestWrapper.getResponseFuture();
                        responseFuture.done(response);
                        freeChannel(channel);
                        ByteBuf bf = response.content();
                        byte[] buff = new byte[bf.capacity()];
                        bf.readBytes(buff);
                        response.getStatus();

//                        System.out.println(request);
//                        System.out.println(new String(buff));
//                        System.out.println("---------------");

                    }

                });

            }
        });

    }

    class ChannelRequestWrapper {
        private HttpRequest request;
        private ResponseFuture responseFuture;
        private long begin;

        public ChannelRequestWrapper(HttpRequest request, ResponseFuture responseFuture, long begin) {
            super();
            this.request = request;
            this.responseFuture = responseFuture;
            this.begin = begin;
        }

        public HttpRequest getRequest() {
            return request;
        }

        public void setRequest(HttpRequest request) {
            this.request = request;
        }

        public long getBegin() {
            return begin;
        }

        public void setBegin(long begin) {
            this.begin = begin;
        }

        public ResponseFuture getResponseFuture() {
            return responseFuture;
        }

        public void setResponseFuture(ResponseFuture responseFuture) {
            this.responseFuture = responseFuture;
        }

    }
}
