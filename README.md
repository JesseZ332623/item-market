# 《Redis in action》 练习

## Technology Selection

![SKILLS](https://skillicons.dev/icons?i=redis,mysql,spring,lua)

---

很多情况下，Redis 数据库仅仅是一个项目的中间层（比如存储用户 Session，缓存关系数据库数据等）。
但有些项目（比如网页游戏）是只依靠 Redis 来存储数据的，所以以《Redis in action》这本书提供的范例为基础，
顺便熟悉 Redis 的数据结构（String, List, Set, Zset, Hash, Stream, Geo, Bitmap）、
Redis 命令、Redis Lua 脚本的编写以及 Redis 分布式锁和 Redis 公平信号量的实现。

---

### 项目 Redis key 详见

[Redis key concat](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/utils/KeyConcat.java)

### 项目 Redis key 示例图详见

[Redis key example](https://github.com/JesseZ332623/item-market/blob/main/documents/redis-key-describe.png)

### Lua 脚本读取器详见

[Lua script reader](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/utils/LuaScriptReader.java)

### 项目通用 Redis 错误处理详见

[Redis generic error handle](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/errorhandle/RedisErrorHandle.java)

### 用户、商品 UID 生成器详见

[ID generator](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/utils/UUIDGenerator.java)

### 用户操作实现详见

[User redis service](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/user/impl/UserRedisServiceImpl.java)

### 市场交易操作实现详见

[Market redis service](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/market/impl/MarketServiceImpl.java)

### 邮件任务执行器实现详见

[Email Send Task Executor](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/email_send_task/impl/EmailSendTaskImpl.java)

### 项目所有操作测试类详见

[Project operator test](https://github.com/JesseZ332623/item-market/blob/main/src/test/java/com/example/jesse/item_market)

### 响应式 Redis 分布式锁实现详见

[Reactive Distributed Lock](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/lock/impl/RedisLockImpl.java)

### 响应式 Redis 公平信号量实现详见

[Reactive Fair Semaphore](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/semaphore/impl/FairSemaphoreImpl.java)

### Redis Hash 分片（Sharing）操作实操详见

[Hash Sharing](https://github.com/JesseZ332623/item-market/tree/main/src/main/java/com/example/jesse/item_market/location_search)

### 项目 Lua 脚本详见

[User Script](https://github.com/JesseZ332623/item-market/tree/main/src/main/resources/lua-script/user-operator)

[Market Script](https://github.com/JesseZ332623/item-market/tree/main/src/main/resources/lua-script/market-operator)

[Guild Script](https://github.com/JesseZ332623/item-market/tree/main/src/main/resources/lua-script/guild-operator)

[Distributed Lock Script](https://github.com/JesseZ332623/item-market/tree/main/src/main/resources/lua-script/lock-operator)

[Distributed Fair Semaphore](https://github.com/JesseZ332623/item-market/tree/main/src/main/resources/lua-script/semaphore-operator)

---

### [GNU GENERAL PUBLIC LICENCE](https://github.com/JesseZ332623/item-market/blob/main/LICENSE)

### Latest Update date: 2025-09-15
