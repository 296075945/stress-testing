package com.wy.stress.testing;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import io.netty.handler.codec.http.HttpVersion;

public class Test {

    public static void main(String[] args) throws Exception {

        ArrayBlockingQueue<FullHttpRequest> queue = new ArrayBlockingQueue<>(100);

        Bootstrap bootstrap = new Bootstrap();

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
                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
                        FullHttpRequest request = queue.take();
                        System.out.println(request.getUri());

                        ByteBuf bf = msg.content();
                        byte[] buff = new byte[bf.capacity()];
                        bf.readBytes(buff);

                        System.out.println(new String(buff, "utf-8"));
                    }

                });

            }
        });
        

        Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
        for (int i = 0; i < 10; i++) {
            URI uri = new URI("http://localhost:8080/test2?id=" + ThreadLocalRandom.current().nextInt(1000));

            FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                    uri.toASCIIString());
            request.headers().set(HttpHeaders.Names.HOST, uri.getHost() + ":" + uri.getPort());
            queue.put(request);
            channel.writeAndFlush(request);
        }

    }
}
