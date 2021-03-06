package com.lbw.rpc.registry;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于 ZooKeeper 的服务发现，实现了 ServiceDiscovery 接口
 */
public class ZooKeeperServiceDiscovery implements ServiceDiscovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);

    private String zkAddress;

    public ZooKeeperServiceDiscovery(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    @Override
    public String discover(String serviceName) {
        // 创建 ZooKeeper 客户端
        ZkClient zkClient = new ZkClient(zkAddress, ZKConstant.ZK_SESSION_TIMEOUT, ZKConstant.ZK_CONNECTION_TIMEOUT);
        LOGGER.info("Connect to zookeeper");
        try {
            // 获取指定服务注册的节点
            String servicePath = ZKConstant.ZK_REGISTRY_PATH + "/" + serviceName;
            // 节点不存在就说明该服务没有被注册
            if (!zkClient.exists(servicePath)) {
                throw new RuntimeException(String.format("Cannot find any service node on path: %s", servicePath));
            }
            // 获取该服务的所有可用地址
            List<String> addressList = zkClient.getChildren(servicePath);
            if (addressList == null || addressList.size() == 0) {
                throw new RuntimeException(String.format("Cannot find any address node on path: %s", servicePath));
            }
            // 获取 address 节点
            String address;
            int size = addressList.size();
            if (size == 1) {
                // 若只有一个地址，则获取该地址
                address = addressList.get(0);
                LOGGER.info("Get the only address node: {}", address);
            } else {
                // 若存在多个地址，则随机获取一个地址
                // 使用了随机的负载均衡策略
                address = addressList.get(ThreadLocalRandom.current().nextInt(size));
                LOGGER.info("Get random address node: {}", address);
            }
            // 获取 address 节点的值
            String addressPath = servicePath + "/" + address;
            return zkClient.readData(addressPath);
        } finally {
            zkClient.close();
        }
    }
}