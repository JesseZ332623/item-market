--[[
    用户离开公会。

    KEYS:
        guildKey        公会键（示例:guild:The-Dark-Brotherhood）
        guildLogKey     公会操作日志键（guild:log）
        userKey         用户键（如：users:114934523722107784）

    ARGV:
        uuid                用户 UUID
        formatGuildName     公会名
        userGuildField      用户哈希的用户公会字段名
        userGuildRoleField  用户哈希的用户公会身份字段名
]]

local guildKey      = KEYS[1]
local guildLogKey   = KEYS[2]
local userKey       = KEYS[3]

local uuid               = ARGV[1]
local formatGuildName    = ARGV[2]
local userGuildField     = ARGV[3]
local userGuildRoleField = ARGV[4]

local timestamp = redis.call('TIME')[1]

local userGuildName = redis.call('HGET', userKey, userGuildField)
local userGuildRole = redis.call('HGET', userKey, userGuildRoleField)

-- 检查用户的公会信息，
-- 对于未加入任何公会的用户，禁止后续的操作
if
    userGuildName == '---' or
    userGuildRole == '---'
then
    return '{"result": "NOT_JOIN_ANY_GUILD"}'
end

-- 从用户信息中查询用户隶属于哪个公会，
-- 若检查到用户不隶属于本公会，直接返回错误信息
if
    userGuildName ~= formatGuildName
then
    return '{"result", "NOT_BELONG_TO_GUILD"}'
end

-- 如果是公会的头儿，
-- 禁止离开公会，除非他把位置让给别的成员
if
    userGuildRole == "Leader"
then
    return '{"result": "LEAVE_FORBIDDEN"}'
end

-- 查询用户名
local userName = redis.call('HGET', userKey, "\"name\"")

if
    userName == false
then
    return '{"result": "USER_NAME_NOT_FOUND"}'
end

-- 所有检查通过后，正式开始 leaveGuild 操作

-- 从公会有序列表中删除这个用户
redis.call('ZREM', guildKey, userName)
redis.call(
    'XADD',
    guildLogKey, '*',
    'event', 'LEAVE_GUILD',
    'uuid', uuid,
    'guild-name', formatGuildName,
    'timestamp', timestamp
)

-- 把用户信息中的公会部分抹掉
redis.call(
    'HSET', userKey,
    userGuildField,     '---',
    userGuildRoleField, '---'
)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_LEAVE_GUILD',
    'uuid', uuid,
    'user-name', userName,
    'user-funds', '---',
    'timestamp', timestamp
)

return '{"result", "SUCCESS"}'