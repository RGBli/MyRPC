package com.lbw.rpc.client;

import com.lbw.rpc.common.codec.RpcDecoder;
import com.lbw.rpc.common.codec.RpcEncoder;
import com.lbw.rpc.common.model.RpcRequest;
import com.lbw.rpc.common.model.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 客户端
 * 用于发送 RPC 请求，并接收来自 Server 端的响应
 * 继承了 SimpleChannelInboundHandler 类，该类又继承了 ChannelHandlerAdapter 类
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClient.class);

    private final String host;

    private final int port;

    private RpcResponse response;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // 读取传来的 RpcResponse
    @Override
    public void channelRead0(ChannelHandlerContext ctx, RpcResponse response) {
        this.response = response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Api caught exception", cause);
        ctx.close();
    }

    // 使用 Netty 来发送 RpcRequest 并获取服务器返回的 RpcResponse
    public RpcResponse send(RpcRequest request) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建并初始化 Netty 客户端 Bootstrap 对象
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) {
                            ChannelPipeline pipeline = channel.pipeline();
                            // 编码 RPC 请求
                            pipeline.addLast(new RpcEncoder(RpcRequest.class));
                            // 解码 RPC 响应
                            pipeline.addLast(new RpcDecoder(RpcResponse.class));
                            // 处理 RPC 响应，该类继承了 ChannelHandlerAdapter 类，故可以直接作为 Handler
                            pipeline.addLast(RpcClient.this);
                        }
                    });
            // 连接 RPC 服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            // 写入 RPC 请求数据并关闭连接
            Channel channel = future.channel();
            channel.writeAndFlush(request).sync();
            channel.closeFuture().sync();
            // 返回 RPC 响应对象
            return response;
        } finally {
            group.shutdownGracefully();
        }
    }
}
