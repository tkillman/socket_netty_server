package com.example.socket_netty_server.myNetty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

    private static final AttributeKey<Claims> AUTH_CLAIMS = AttributeKey.valueOf("AUTH_CLAIMS");
    private final JwtVerifier jwtVerifier;

    public NettyServerHandler(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    private void authenticate(ChannelHandlerContext ctx, String msg) {
        if (!msg.startsWith("AUTH|")) {
            ctx.writeAndFlush("AUTH_FAIL|먼저 인증해야 합니다.\n");
            ctx.close();
            return;
        }

        try {
            String jwt = msg.substring("AUTH|".length());

            Claims claims = jwtVerifier.verify(jwt);

            String cccode = claims.get("cccode", String.class);
            String shopCd = claims.get("shop_cd", String.class);

            if (cccode == null || shopCd == null) {
                throw new IllegalArgumentException(
                        "cccode 또는 shop_cd가 없습니다.");
            }

            // 인증 성공 정보를 이 TCP 연결에 저장
            ctx.channel().attr(AUTH_CLAIMS).set(claims);

            ctx.writeAndFlush(
                    "AUTH_OK|" + cccode + "|" + shopCd + "\n");

        } catch (Exception e) {
            logger.warn("JWT 인증 실패: {}", e.getMessage());
            ctx.writeAndFlush("AUTH_FAIL|유효하지 않은 토큰입니다.\n");
            ctx.close();
        }
    }

    private void handleAuthorizedRequest(
            ChannelHandlerContext ctx,
            Claims claims,
            String msg) {
        String cccode = claims.get("cccode", String.class);
        String shopCd = claims.get("shop_cd", String.class);

        if (msg.equals("PING")) {
            ctx.writeAndFlush("PONG\n");
            return;
        }

        if (msg.equals("SHOP_INFO")) {
            // 클라이언트 메시지가 아닌 JWT의 shop_cd 기준으로 처리
            ctx.writeAndFlush(
                    "SHOP_INFO|" + cccode + "|" + shopCd + "\n");
            return;
        }

        ctx.writeAndFlush("ERROR|지원하지 않는 요청입니다.\n");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Client connected ::: " + ctx.channel().remoteAddress());
        ctx.writeAndFlush("INFO|AUTH|JWT를 먼저 전송하세요.\n");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        logger.info("Received message: {}", msg);
        Claims claims = ctx.channel().attr(AUTH_CLAIMS).get();

        // 인증 전: AUTH|JWT 메시지만 허용
        if (claims == null) {
            authenticate(ctx, msg);
            return;
        }

        // 인증 후: 권한에 따라 요청 처리
        handleAuthorizedRequest(ctx, claims, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
