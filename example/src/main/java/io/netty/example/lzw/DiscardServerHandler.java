package io.netty.example.lzw;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class DiscardServerHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        // discard
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Close the connection when en exception is raised
        cause.printStackTrace();
        ctx.close();
    }
}
