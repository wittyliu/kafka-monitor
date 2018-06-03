# kafka-monitor
kafka 消息积压告警监控

* 原本只是一个很简陋的需求：监控lag参数，超阈值发送告警短信
* 时间比较宽裕就加了一些保证健壮性和可用性的功能
* 由于发送短信的jar包不是开源的 就把对应jar包的配置文件ignore掉了， 可自行匹配相关jar包，修改相关代码和pom.xml
* 关于maven项目依赖本地jar包编译成可执行jar包可参考 [https://blog.csdn.net/hd_xb/article/details/80534684](https://blog.csdn.net/hd_xb/article/details/80534684)


* 后续将提供邮件提醒功能
