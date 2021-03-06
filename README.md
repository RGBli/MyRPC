# MyRPC
---
### 1.介绍
MyRPC 是基于 Netty 和 Spring 框架编写的、使用 Zookeeper 来进行服务注册和服务发现的分布式 RPC 框架。
<br/><br/>

### 2.技术点
1）Netty 和 Spring 框架

2）Zookeeper 配置和操作，临时节点和持久节点

3）Protobuf 序列化方法

4）反射和动态代理
<br/><br/>

### 3.未来改进
1）扩充负载均衡策略
目前仅支持随机负载均衡策略，但随机负载均衡策略在服务器数量较少时，可能由于概率问题请求会集中发送到某一服务器上，如果该服务器性能不佳，就会拖慢系统的运行速度。

2）序列化方法
目前仅支持 Protobuf 序列化方式，当前还有一些其他高性能的序列化方法，比如 Avro、Kyro 和 Hessian 等，以后会将这些作为用户的选择项。
<br/><br/>

### 4.参考文章
https://gitee.com/huangyong/rpc
https://my.oschina.net/huangyong/blog/361751