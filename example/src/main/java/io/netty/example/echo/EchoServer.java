/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.echo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Echoes back any received data from a client.
 */
public final class EchoServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EchoServerHandler serverHandler = new EchoServerHandler();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
            // Here,we specify to use the NioServerSocketChannel class which is used to instantiate a new Channel to accept incoming connections.
             .channel(NioServerSocketChannel.class)
            /**
             * You can also set the parameters which are specified to the Channel implementation. We are writing a TCP/IP server,so we are allowed
             * to set the socket options such as tcpNoDelay and keepAlive.Please refer to the apidocs of ChannelOption and the specific ChannelConfig
             * implementations to get an overview about the support ChannelOptions
              */
             .option(ChannelOption.SO_BACKLOG, 100)
            /**
             * Did you notice option() and childOption?option() is for the NioServerSocketChannel that accepts incoming connections.
             * childOption() is for the Channels accepted by the parent ServerChannel,which is NioServerSocketChannel in this case.
             */
             .childOption(ChannelOption.SO_KEEPALIVE,true)
             .handler(new LoggingHandler(LogLevel.INFO))
            /**
             * The handler specified here will always be evaluated by a newly accepted Channel.The ChannelInitializer is a special
             * handler that is purposed to help a user configure a new Channel.It is most likely that you want to configure the ChannelPipeline
             * of the new Channel by adding some handlers such as DiscardServerHandler to implement your network application.As the application
             * gets compilcated,it is likely that you will add more handlers to the pipeline and extract this  anonymous class into a top-level
             * class eventually.
             */
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     if (sslCtx != null) {
                         p.addLast(sslCtx.newHandler(ch.alloc()));
                     }
                     //p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(serverHandler);
                 }
             });

            // Start the server.
            /**
             * We are ready to go now.What‘s left is to bind to the port and to start the server.Here,we bind to the port 8080 of all NICs(network
             * interface cards) in the machine.You can now call the bind() methods as many times as you want(with different bind addresses.)
             */
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
