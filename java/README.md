# Message

一个消息程序, WebSocket 主动推送消息到客户端, 内含用户 用户组 消息(无大小限制的发送), 同时采用了epoll 提高Linux 系统下连接率  


# 技术
- Kotlin
- Vert.x
- OpenJ9
- Vue 2.0


 

 

# 预览地址

 暂无, 可能会更新
 

# Docker 镜像
`ccr.ccs.tencentyun.com/xxscloud/messagex:v1`

> 环境变量: 

- DATASOURCE_PASSWORD=数据库密码

- DATASOURCE_USER= 数据库用户

- DATASOURCE= 数据库地址

- DATASOURCE_PORT=数据库端口

- DB_NAME= 数据库名称

- REDIS_PASSWORD=Redis 密码 没有空

- REDIS= Redis 地址

- REDIS_PORT= Redis 端口

# 最后说明
> 项目是因为有人找我做, 我给别人做了一个.NET客户端, 他们系统通过http向我推送消息, 由此系统进行推送给N个客户端 同时还维护了离线消息, 等等

# 联系我
Email: `15629116378@163.com`
以上技术，如果使用过程中有任何问题可以联系我 15629116378 (支付宝同，付费问问题不谢)
