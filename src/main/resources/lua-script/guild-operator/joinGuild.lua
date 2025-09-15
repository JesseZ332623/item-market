--[[
    用户加入公会。

    KEYS:
        guildKey        公会键（示例:guild:The-Dark-Brotherhood）
        guildLogKey     公会操作日志键（guild:log）
        userKey         用户键（如：users:114934523722107784）

    ARGV:
        uuid                用户 UUID
        formatGuildName     公会名
        userGuildField      用户哈希的用户公会字段名
        userGuildRoleField  用户哈希的用户公会身份字段名
        maxMembers          公会的最大成员数
]]

local guildKey        = KEYS[1]
local guildLogKey     = KEYS[2]
local guildNameSetKey = KEYS[3]
local userKey         = KEYS[4]

local uuid               = ARGV[1]
local formatGuildName    = ARGV[2]
local userGuildField     = ARGV[3]
local userGuildRoleField = ARGV[4]
local maxMembers         = ARGV[5]

local timestamp = redis.call('TIME')[1]

local userGuildName = redis.call('HGET', userKey, userGuildField)
local userGuildRole = redis.call('HGET', userKey, userGuildRoleField)

local noGuildInfo = "\"---\""

-- 检查用户的公会信息，
-- 对于已经加入公会的用户，
-- 不允许再加入其他公会或者重复加入公会
if
    userGuildName ~= noGuildInfo or
    userGuildRole ~= noGuildInfo
then
    return '{"result": "ALREADY_JOINED"}'
end

-- 检查要加入的公会是否存在
if
    redis.call('SISMEMBER', guildNameSetKey, formatGuildName) ~= 1
then
    return '{"result": "GUILD_NOT_FOUND"}'
end

-- 查询用户名，后面会写入这个公会的有序列表中
local userName = redis.call('HGET', userKey, "\"name\"")

if
    userName == false
then
    return '{"result": "USER_NAME_NOT_FOUND"}'
end

-- 检查这个公会是否已经满员
if
    redis.call('ZCOUNT', guildKey, '-inf', '+inf') == maxMembers
then
    return '{"result": "GUILD_IS_FULL"}'
end

redis.call('ZADD', guildKey, 'NX', 0, userName)
redis.call(
    'XADD',
    guildLogKey, '*',
    'event', 'JOIN_GUILD',
    'uuid', uuid,
    'guild-name', formatGuildName,
    'timestamp', timestamp
)

-- 修改用户数据，令该用户成为公会的 Member
redis.call(
    'HSET', userKey,
    userGuildField, formatGuildName,
    userGuildRoleField, '"Member"'
)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_JOIN_GUILD',
    'uuid', uuid,
    'user-name', userName,
    'user-funds', '---',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'