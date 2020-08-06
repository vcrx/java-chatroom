# chatroom

## 如何启动

首先需要导入 `./libs` 下的 `jar` 包。参考[这个链接](https://blog.csdn.net/baidu_37107022/article/details/70876993)

先运行 Server 端
  `src/Server/Main.java`
再运行 Client 端，每一个 Client 端都是一个客户端，可以把 Server 端放在公网，这样即可实现公网多人聊天。
  `src/Client/Main.java`

## 开发

文件编码皆为 `GBK`。

## 一些图片

群聊：
![群聊](https://i.loli.net/2020/08/06/MAVKnULqOcYvbSu.png)
![看图](https://i.loli.net/2020/08/06/FRfsmwM9kBq6QPH.png)

私聊：
![私聊](https://i.loli.net/2020/08/06/Rd7LunPywDNHmOz.png)

## 课程设计报告节选

### 需求分析过程

（1） 首先需要支持多人聊天和用户私聊，这些消息通过服务器转发。
（2） 用户打开软件后需要输入用户名密码登录，然后服务器进行校验，返回成功或密码错误。
（3） 可以展示当前在线用户列表。
（4） 用户可以点击其他用户的用户名来开始私聊。
（5） 用户可以发送接收图片，文件
（6） 用户点击图片可以查看大图。
（7） 用户点击文件可以保存文件。
（8） 用户私聊和群聊功能一致。

### 总体设计方案

服务端：
（1） 校验客户端用户名密码。
（2） 转发消息给所有人。
（3） 私聊转发消息给某人。
（4） 广播聊天室人员变动以及当前在线用户信息。

客户端：
（1） 请用户输入用户名密码，向服务器发送登录信息并接收返回结果。
（2） 在聊天室、私聊发送消息。
（3） 在聊天室、私聊发送文件，图片。
（4） 保存文件，查看大图，保存图片。

### 消息内容格式设计

需要规定消息传送的格式及各种字段。
`Message` 类是基本字段，有 `fromUser`, `toUser`, `msg`, `type`, `timeStamp`, `filename`, `users` 等，这些字段只有 `fromUser`, `msg`, `type`, `timeStamp` 是每次发送消息必选字段，其他都是根据不同 `type` 来发送的。举几个例子：

1. 群聊/私聊发送消息：

    群聊发送消息：

    ```json
    {
        "fromUser": { "userName": "小明" },
        "msg": "你好",
        "type": "text",
        "timeStamp": 1591158456593
    }
    ```

    私聊发送消息，私聊和群聊的区别就是私聊会多一个字段 `toUser`:

    ```json
    {
        "fromUser": { "userName": "小明" },
        "toUser": { "userName": "小红" },
        "msg": "你好",
        "type": "text",
        "timeStamp": 1591158456341
    }
    ```

2. 群聊/私聊发送图片：

    ```json
    {
        "filename": "fstring.png",
        "fromUser": { "userName": "user1" },
        "msg": <压缩后对图片进行Base64编码的结果>,
        "timeStamp": 1591158584251,
        "type": "img"
    }
    ```

3. 群聊/私聊发送文件，和图片的 `type` 不同：

    ```json
    {
        "filename": "cc_20200527_194310.reg",
        "fromUser": { "userName": "user1" },
        "msg": <压缩后对文件进行Base64编码的结果>,
        "timeStamp": 1591163516527,
        "type": "file"
    }
    ```

4. 广播人员变动：

    ```json
    {
        "fromUser": { "userName": "user1" },
        "msg": "join",
        "timeStamp": 1591158456963,
        "type": "event",
        "users": [
            {
                "userName": "user1"
            }
        ]
    }
    ```
