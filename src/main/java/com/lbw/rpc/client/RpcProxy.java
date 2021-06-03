package com.lbw.rpc.client;

import com.lbw.rpc.common.model.RpcRequest;
import com.lbw.rpc.common.model.RpcResponse;
import com.lbw.rpc.registry.ServiceDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * RPC 代理
 * 用于创建 RPC 服务代理
 */
public class RpcProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcProxy.class);

    private String serviceAddress;

    private ServiceDiscovery serviceDiscovery;

    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    // 用于创建代理实例
    // 创建代理是为了给 RPC 客户端调用，代理将客户端的调用发送给服务器
    public Object getProxyInstance(final Class<?> interfaceClass, final String serviceVersion) {
        // 创建动态代理对象
        return Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    // 在重写的 invoke() 方法中发送 RPC 请求，并返回 RPC 调用的结果
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // 创建 RPC 请求对象并设置请求属性，通过反射获取某些属性
                        RpcRequest request = new RpcRequest();
                        request.setRequestId(UUID.randomUUID().toString());
                        request.setInterfaceName(method.getDeclaringClass().getName());
                        request.setServiceVersion(serviceVersion);
                        request.setMethodName(method.getName());
                        request.setParameterTypes(method.getParameterTypes());
                        request.setParameters(args);
                        // 从 ZooKeeper 指定的路径获取 RPC 服务地址
                        if (serviceDiscovery != null) {
                            String serviceName = interfaceClass.getName();
                            if (serviceVersion != null && !serviceVersion.isEmpty()) {
                                serviceName += "-" + serviceVersion;
                            }
                            serviceAddress = serviceDiscovery.discover(serviceName);
                            LOGGER.info("Discover service: {} => {}", serviceName, serviceAddress);
                        }
                        if (serviceAddress == null) {
                            throw new RuntimeException("Server address is empty");
                        }
                        // 从 RPC 服务地址中解析主机名与端口号
                        String[] hostPort = serviceAddress.split(":");
                        String host = hostPort[0];
                        int port = Integer.parseInt(hostPort[1]);
                        // 创建 RPC 客户端对象并发送 RPC 请求
                        RpcClient client = new RpcClient(host, port);
                        long time = System.currentTimeMillis();
                        // 准备好 request 了，然后调用 send() 方法来发送请求并返回服务器的 response
                        RpcResponse response = client.send(request);
                        LOGGER.info("Time: {}ms", System.currentTimeMillis() - time);
                        if (response == null) {
                            throw new RuntimeException("Response is null");
                        }
                        // 返回 RPC 响应结果
                        if (response.hasException()) {
                            throw response.getException();  
                        } else {
                            return response.getResult();
                        }
                    }
                }
        );
    }

    public Object getProxyInstance(final Class<?> interfaceClass) {
        return getProxyInstance(interfaceClass, "");
    }
}
