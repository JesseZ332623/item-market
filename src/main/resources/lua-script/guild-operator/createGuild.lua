--[[
    用户创建公会，并成为这个公会的 Leader。

    KEYS:
        guildKey        公会键（示例:guild:The-Dark-Brotherhood）
        guildNameSetKey 公会名集合键，确保用户名不重复（guild:guild-name-set）
        guildLogKey     公会操作日志键（guild:log）
        userKey         用户键（如：users:114934523722107784）

    ARGV:
        uuid                用户 UUID
        formatGuildName     公会名
        userGuildField      用户哈希的用户公会字段名
        userGuildRoleField  用户哈希的用户公会身份字段名
]]
local guildKey           = KEYS[1]
local guildNameSetKey    = KEYS[2]
local guildLogKey        = KEYS[3]
local guildNameSetLogKey = KEYS[4]
local userKey            = KEYS[5]

local uuid               = ARGV[1]
local formatGuildName    = ARGV[2]
local userGuildField     = ARGV[3]
local userGuildRoleField = ARGV[4]

local timestamp = redis.call('TIME')[1]

local userGuildName = redis.call('HGET', userKey, userGuildField)
local userGuildRole = redis.call('HGET', userKey, userGuildRoleField)

-- 检查用户的公会信息，对于已经加入公会的用户，不允许再新建公会
if
    userGuildName ~= '---' or
    userGuildRole ~= '---'
then
    return '{"result", "ALREADY_JOINED"}'
end

-- 尝试往公会名集合插入一条数据
local isAdded
    = redis.call('SADD', guildNameSetKey, formatGuildName)

-- 如果插入失败，说明公会名重复，直接返回
if
    isAdded == 0
then
    return '{"result": "DUPLICATE_GUILD_NAME"}'
end

redis.call(
    'XADD',
     guildNameSetLogKey, '*',
     'event', 'INSERT_GUILD_NAME',
     'guild-name', formatGuildName,
     'timestamp', timestamp
)

-- 查询用户名，会写入这个公会的有序列表中
local userName = redis.call('HGET', userKey, "\"name\"")

if
    userName == false
then
    return '{"result": "USER_NAME_NOT_FOUND"}'
end

redis.call('ZADD', guildKey, 'NX', 0, userName)
redis.call(
    'XADD',
    guildLogKey, '*',
    'event', 'CREATE_GUILD',
    'uuid', uuid,
    'guild-name', formatGuildName,
    'timestamp', timestamp
)

-- 修改用户数据，令创建者成为公会的 Leader
redis.call(
    'HSET', userKey,
    userGuildField, formatGuildName,
    userGuildRoleField, '"Leader"'
)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_CREATE_GUILD',
    'uuid', uuid,
    'user-name', userName,
    'user-funds', '---',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'