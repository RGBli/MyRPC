package com.lbw.rpc.registry;

/**
 * 服务注册接口
 */
public interface ServiceRegistry {

    /**
     * 注册服务名称与服务地址
     */
    void register(String serviceName, String serviceAddress);
}