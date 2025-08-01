# Redis 练习之用户交易功能实现

## Technology Selection

![SKILLS](https://skillicons.dev/icons?i=redis,spring,lua)

很多情况下，Redis 数据库仅仅是一个项目的中间层（比如存储用户 Session，缓存关系数据库数据等）。
但有些项目（比如网页游戏）是只依靠 Redis 来存储数据的，所以我想使用 Redis 来编写一个用户交易的实现，
顺便熟悉 Redis 的 5 种数据结构（String, List, Set, Zset, Hash）、Redis 命令和 Redis Lua 脚本的编写。

---

### 项目 Redis key 详见
[Redis key](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/utils/KeyConcat.java)

### Lua 脚本读取器详见：
[Lua script reader](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/utils/LuaScriptReader.java)

### 用户、商品 UID 生成器详见：
[ID generator](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/utils/UUIDGenerator.java)

### 用户操作实现详见：
[User redis service](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/user/impl/UserRedisServiceImpl.java)

### 市场交易操作实现详见：
[Market redis service](https://github.com/JesseZ332623/item-market/blob/main/src/main/java/com/example/jesse/item_market/market/impl/MarketServiceImpl.java)

### 项目所有操作测试类详见：
[Project operator test](https://github.com/JesseZ332623/item-market/blob/main/src/test/java/com/example/jesse/item_market/ProjectOperatorTest.java)

### 项目 Lua 脚本详见：
[Lua scripts](https://github.com/JesseZ332623/item-market/tree/main/src/main/resources/lua-script)

下一个星期，前辈其实可以考虑安排点任务给我，
主要的技术栈我基本上都搞明白了，不再是白纸一张了。

### Latest Update date: 2025-08-01