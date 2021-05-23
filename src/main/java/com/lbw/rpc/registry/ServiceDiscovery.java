package com.lbw.rpc.registry;

/**
 * 服务发现接口
 */
public interface ServiceDiscovery {

    /**
     * 根据服务名称查找服务地址
     */
    String discover(String serviceName);
}