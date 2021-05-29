package com.lbw.rpc.registry;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 ZooKeeper 的服务注册，实现了 ServiceRegistry 接口
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    private String zkAddress;


    public ZooKeeperServiceRegistry(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    @Override
    public void register(String serviceName, String serviceAddress) {
        // 创建 ZooKeeper 客户端
        ZkClient zkClient = new ZkClient(zkAddress, ZKConstant.ZK_SESSION_TIMEOUT, ZKConstant.ZK_CONNECTION_TIMEOUT);
        LOGGER.info("Connect to zookeeper");
        // 如果 registry 节点不存在，则创建一个 registry 节点（持久）
        String registryPath = ZKConstant.ZK_REGISTRY_PATH;
        if (!zkClient.exists(registryPath)) {
            zkClient.createPersistent(registryPath);
            LOGGER.info("Create registry node: {}", registryPath);
        }
        // 如果 service 节点不存在则创建（持久）
        String servicePath = registryPath + "/" + serviceName;
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
            LOGGER.info("Create service node: {}", servicePath);
        }
        // 创建 address 节点（临时）
        String addressPath = servicePath + "/address-";
        // 向节点写入服务地址，ip:port，创建临时节点时会自动给路径名后面加上序号
        String addressNode = zkClient.createEphemeralSequential(addressPath, serviceAddress);
        LOGGER.info("Create address node: {}", addressNode);
    }
}