# MessageX 推送消息模块
-----
### 功能
 1) 可以将MessageX 内嵌系统, 实现推送闭环
 2) 采用MySQL8.0 + Kotlin 1.4 + Jvm8 开发
 3) 本系统有后台管理，可以进行管理渠道


#### 支持的客户端
 - .NET 4.0 SDK
 - 安卓SDK (待开发)


#### 使用接口文档
> 每次进行接口调用必须在请求头部添加 `token` 内容是注册渠道返回的
> 所有请求都是 `POST/JSON` 请求方式
> 所有请求接口始终返回Http状态码200, 判断是否成功通过`success` = `true` 判断接口请求是否成功

##### 1. 注册用户
> 路径: ` /open/user/registered`

> 入参: 
```
{
     "openId":"你的id",  //你系统唯一ID
     "nickName":"网名"
}
``` 

> 返回: 
```
{
    "success": false,
    "code": "2312",
    "message": "账号已存在"
}
``` 

##### 2. 发送消息
> 路径: `/open/message/send`

> 入参: 
```
{
    "sender":"1002",   //发件人
    "title": "title", //标题
    "content": "content", //正文 无限制
    "abstract": "abstract", //摘要
    "recipient": ["1"] //收件人Id组 二则取其一
    "recipientGroup":["1"] //收件人群组 二则取其一
}
``` 

> 返回: 
```
{
    "success": true,
    "data": {
        "id": "159219",
        "sender": "1002",
        "title": "title",
        "abstract": "abstract",
        "content": "content"
    },
    "code": "200"
}
``` 


##### 3. 加入组
> 路径: `/open/userGroup/joinGroup`

> 入参: 
```
{
    "users": ["1"],  //用户组
    "id": "1"  //组ID
}
``` 

> 返回: 
```
{
    "success": true,
    "data": true,
    "code": "200"
}
``` 


##### 4. 获取组列表
> 路径: `/open/userGroup/getGroupList`

> 入参: 
```
{}
``` 

> 返回: 
```
{
    "success": true,
    "data": [
        {
            "id": "4",
            "channelId": "1",
            "name": "测试组"
        }
    ],
    "code": "200"
}
``` 


##### 5. 获得组信息
> 路径: `/open/userGroup/getGroupInfo`

> 入参: 
```
{
    "id":"4"
}
``` 

> 返回: 
```
{
    "success": true,
    "data": {
        "id": "4",
        "userList": [
            {
                "id": "1",
                "openId": "1daa09cf4e5f46bcbd5747dd792b43a8",
                "nickName": "小双丫丫",
                "status": 1
            }
        ]
    },
    "code": "200"
}
``` 

##### 6. 创建组
> 路径: `/open/userGroup/create`

> 入参: 
```
{
    "name":"测试组"
}
``` 

> 返回: 
```
{
    "success": true,
    "data": true,
    "code": "200"
}
``` 

##### 7. 获取用户信息
> 路径: `/open/user/getUserInfo`

> 入参: 
```
{
    "id": "1002" //用户ID
}
``` 

> 返回: 
```
{
    "success": true,
    "data": {
        "id": "1002",
        "channel": "1",
        "openId": "c9c8001579cf3f8f8cebb5b8c6f0916a",
        "nickName": "网名",
        "status": 1,
        "unreadMessageCount": 1
    },
    "code": "200"
}
``` 

##### 8. 生成token
> 路径: `/open/user/generateToken`

> 入参: 
```
{
    "id": "1002" //用户ID
}
``` 

> 返回: 
```
{
    "success": true,
    "data": "API_e25a787177b846fbb55b9161eb53195c",
    "code": "200"
}
``` 