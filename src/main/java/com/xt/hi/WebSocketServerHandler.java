package com.xt.hi;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Date;

/**
 * Created by admin on 2017/5/18.
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private WebSocketServerHandshaker handshaker;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        TextWebSocketFrame msg = new TextWebSocketFrame(new Date().toString()+ " ["
                + ctx.channel().remoteAddress()+ "]加入");
        channels.writeAndFlush(msg);
        System.out.println("["+ctx.channel().remoteAddress()+"] 加入" );
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
        TextWebSocketFrame msg = new TextWebSocketFrame(new Date().toString()+ " ["
                + ctx.channel().remoteAddress()+ "]离开");
        channels.writeAndFlush(msg);
        System.out.println("["+ctx.channel().remoteAddress()+"] 离开" );
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        if (o instanceof FullHttpRequest) {
            handleHttpRequest(channelHandlerContext, ((FullHttpRequest) o));
        } else if (o instanceof WebSocketFrame) {
            handlerWebSocketFrame(channelHandlerContext, (WebSocketFrame) o);
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }


    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // TextWebSocketFrame 只支持文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }


        String msg = ((TextWebSocketFrame) frame).text();
        System.out.println("["+ctx.channel().remoteAddress()+"]说: " + msg);

        //文本是用TextWebSocketFrame的形式发送
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString()+ " ["
                + ctx.channel().remoteAddress()+ "]说: " + msg);

        // broadcast
        channels.writeAndFlush(tws);
    }
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.getDecoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:7397/websocket", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory
                    .sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }
    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
                    CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    private static boolean isKeepAlive(FullHttpRequest req) {
        return false;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }


}
