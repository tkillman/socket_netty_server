package com.example.socket_netty_server.myNetty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;

import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import jakarta.annotation.PreDestroy;

@Component
public class NettyServer implements ApplicationRunner {

    @Value("${netty.port:9000}")
    private int port;

    /*
     * 서버 포트로 들어오는 새 연결 요청을 받음
     * 연결이 성립되면 해당 소켓 채널을 workerGruop에 넘기
     * 연결 수락 작업만 하므로 보통 스레드 1개면 충분
     * 손님을 입장시키는 접수대
     */
    private EventLoopGroup bossGroup;

    /**
     * 연결된 각 클라이언트 실제 통신을 담당
     * 데이터 읽기.쓰기, 디코딩.인코딩, NettyServerHandler의 channelRead() 같은 메소드 실행이 여기서 일어남
     * 여러 클라이언트를 동시에 처리해야 하므로 기본 스레드 수를 사용하거나 상황에 맞게 지정
     * 입장한 손님과 실제로 대화하는 직원들
     * 
     */
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(new DelimiterBasedFrameDecoder(
                                    8192, Delimiters.lineDelimiter()));
                            pipeline.addLast(new StringDecoder(
                                    CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(
                                    CharsetUtil.UTF_8));
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });

            serverChannel = bootstrap.bind(port).sync().channel();
            logger.info("TCP Server started: {}", port);

        } catch (Exception e) {
            e.printStackTrace();
            stop();
            throw e;
        }

    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
    }
}
