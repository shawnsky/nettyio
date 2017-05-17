package com.xt.hello;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Created by admin on 2017/5/17.
 */
public class DiscardServerHandler extends ChannelInboundHandlerAdapter {


    //每当收到客户端的数据时，就会调用这个方法
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf)msg;
        try{
            while (in.isReadable()){
                System.out.println((char)in.readByte());
                System.out.flush();
            }
        }
        finally {
            ReferenceCountUtil.release(msg);//这是必要的，处理器必须释放这个对象。
        }

    }

    //
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
