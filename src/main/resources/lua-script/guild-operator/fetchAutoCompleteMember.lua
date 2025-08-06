--[[
    在搜索公会成员时，
    对于搜索框输入的 prefix，自动地补全全部匹配的成员名。

    KEYS:
        guildKey 公会键（示例:guild:The-Dark-Brotherhood）

    ARGV:
        predecessor  匹配范围起点（前驱）
        successor    匹配范围终点（后继）
]]
local guildKey = KEYS[1]

local predecessor = ARGV[1]
local successor   = ARGV[2]


local UUID = "{" .. redis.sha1hex(tostringg(math.random())) .. "}"

predecessor = predecessor ..UUID
successor   = successor   ..UUID

redis.log(redis.LOG_NOTICE, predecessor)
redis.log(redis.LOG_NOTICE, successor)

-- 插入临时标记
redis.call(
    'ZADD', guildKey, 'NX',
    0, predecessor,
    0, successor
);

-- 获取前驱在有序列表中的排名
local predecessorRank
    = redis.call('ZRANK', guildKey, predecessor) + 1

-- 获取后继在有序列表中的排名
local successorRank
    = redis.call('ZRANK', guildKey, successor) -  1

redis.log(redis.LOG_NOTICE, predecessorRank)
redis.log(redis.LOG_NOTICE, successorRank)

-- 两者不得为空，否则返回空列表（注意在返回前不要忘记标记）
if
    not predecessorRank or not successorRank
then
    redis.call('ZREM', guildKey, predecessor, successor)
    return { matchedMembers = {} }
end

-- 获取范围内所有匹配的成员（注意 ZRANGE 是闭区间操作）
local members = {}

if
    predecessorRank <= successorRank
then
    redis.call(
        'ZRANGE', guildKey,
        predecessorRank, successorRank
    );
end

-- 最后删除临时标记
redis.call('ZREM', guildKey, predecessor, successor)

redis.log(redis.LOG_NOTICE, members)

-- 返回结果
return { matchedMembers = members }