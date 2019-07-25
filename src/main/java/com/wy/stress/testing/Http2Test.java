package com.wy.stress.testing;

import java.net.URI;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;

public class Http2Test {

    public static void main(String[] args) throws Exception {

        Bootstrap bootstrap = new Bootstrap();

        EventLoopGroup group = new NioEventLoopGroup(4);

        bootstrap.group(group);

        bootstrap.channel(NioSocketChannel.class);

        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        HttpToHttp2ConnectionHandlerBuilder builder = new HttpToHttp2ConnectionHandlerBuilder();
        builder.frameListener(new Http2FrameAdapter());

        bootstrap.handler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {

                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast(builder.build());
                pipeline.addLast(Http2FrameCodecBuilder.forClient().build());
//                pipeline.addLast(builder.build());

                pipeline.addLast(new Http2MultiplexHandler(new ChannelInboundHandlerAdapter() {

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        // TODO Auto-generated method stub
                        super.channelRead(ctx, msg);
                        System.out.println(msg);
                    }

                }));

//                pipeline.addLast()

            }
        });

        Channel channel = bootstrap.connect("localhost", 8080).sync().channel();
        URI uri = new URI("http://localhost:8080/test2?id=1");
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString());
        request.headers().set(HttpHeaderNames.HOST, uri.getHost() + ":" + uri.getPort());
//        request.headers().set(HttpHeaderNames.UPGRADE, "h2c");
//        request.headers().set(HttpHeaderNames.CONNECTION, "Upgrade,HTTP2-Settings");
//
//        request.headers().set("HTTP2-Settings", "AAMAAABkAARAAAAAAAIAAAAA");
        channel.writeAndFlush(request);

//        Http2Headers headers = new DefaultHttp2Headers();
//
//        HttpConversionUtil.toHttp2Headers(request.headers(), headers);
//
//        System.out.println(headers);
//
//        Http2HeadersFrame headersFrame = new DefaultHttp2HeadersFrame(headers);
//
//        channel.writeAndFlush(headersFrame);
//
//        Http2DataWriter dataWriter = new DefaultHttp2FrameWriter();
//
//        channel.writeAndFlush(dataWriter);

//        Http2Stream 

    }
}
