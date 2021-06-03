package com.lbw.rpc.server;

import com.lbw.rpc.common.codec.RpcDecoder;
import com.lbw.rpc.common.codec.RpcEncoder;
import com.lbw.rpc.common.model.RpcRequest;
import com.lbw.rpc.common.model.RpcResponse;
import com.lbw.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC 服务器（用于发布 RPC 服务）
 * 实现了 ApplicationContextAware 接口和 InitializingBean 接口
 */
public class RpcServer implements ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);

    private String serviceAddress;

    private ServiceRegistry serviceRegistry;

    // 存放服务名与服务对象之间的映射关系
    private Map<String, Object> handlerMap = new HashMap<>();

    public RpcServer(String serviceAddress, ServiceRegistry serviceRegistry) {
        this.serviceAddress = serviceAddress;
        this.serviceRegistry = serviceRegistry;
    }

    // 实现 ApplicationContextAware 接口的方法
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // 扫描带有 RpcService 注解的类，并初始化 handlerMap 对象
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
        if (serviceBeanMap.size() != 0) {
            for (Object serviceBean : serviceBeanMap.values()) {
                // 获取 bean 的 @RpcService 注解中的内容
                RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
                String serviceName = rpcService.value().getName();
                String serviceVersion = rpcService.version();
                // 如果在注解中指定了服务的版本号，则在原服务名末尾加 -serviceVersion
                if (!serviceVersion.isEmpty()) {
                    serviceName += "-" + serviceVersion;
                }
                handlerMap.put(serviceName, serviceBean);
            }
        }
    }

    // 实现 InitializingBean 接口的方法，使用 Netty 来服务
    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Server starting...");
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 创建并初始化 Netty 服务端 Bootstrap 对象
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    ChannelPipeline pipeline = channel.pipeline();
                    // 解码 RPC 请求
                    pipeline.addLast(new RpcDecoder(RpcRequest.class));
                    // 编码 RPC 响应
                    pipeline.addLast(new RpcEncoder(RpcResponse.class));
                    // 处理 RPC 请求
                    pipeline.addLast(new RpcServerHandler(handlerMap));
                }
            });
            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            // 获取 RPC 服务器的 IP 地址与端口号
            String[] hostPort = serviceAddress.split(":");
            String ip = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);
            // 启动 RPC 服务器
            ChannelFuture future = bootstrap.bind(ip, port).sync();
            // 注册 RPC 服务地址
            if (serviceRegistry != null) {
                for (String interfaceName : handlerMap.keySet()) {
                    serviceRegistry.register(interfaceName, serviceAddress);
                    LOGGER.info("Register service: {} => {}", interfaceName, serviceAddress);
                }
            }
            LOGGER.info("Server started on port {}", port);
            // 关闭 RPC 服务器
            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
