--[[
    创建一个新用户，并为它随机挑选几件武器放入包裹。

    KEYS:
        newUserKey       用户键（如：users:114934523722107784）
        userSetKey       用户集合键（用来保证用户名的唯一性）
        userInventoryKey 用户包裹键（如：inventories:114935169325609268）
    ARGV:
        userNameField    用户名哈希字段名
        userFundsField   用户资金哈希字段名
        newUserName      用户名（如：Peter，Jesse）
        newUserFunds     新用户初始资金
        initWeaponsStr   新用户的初始武器（空格分割的字符串，如："Sword Shield Axe ..."）
]]
local newUserKey       = KEYS[1]
local userSetKey       = KEYS[2]
local userInventoryKey = KEYS[3]

local userNameField    = ARGV[1]
local userFundsField   = ARGV[2]
local newUserName      = ARGV[3]
local newUserFunds     = ARGV[4]
local initWeaponsStr   = string.match(ARGV[5], '^"(.*)"$')

local timestamp = redis.call('TIME')[1]

-- 解析武器列表成一个 Lua Table
local initWeapons = {}
for weapon in string.gmatch(initWeaponsStr, "%S+") do
    table.insert(initWeapons, "\"" ..weapon.. "\"")
end

-- 尝试往用户集合内插入新用户，若返回 0 则代表用户名重复
if redis.call('SADD', userSetKey, newUserName) == 0
then
    return '{"result": "DUPLICATE_USER"}'
end

-- 用户添加进集合成功，需要加一条审计数据
redis.call(
    'XADD', 'user-name:log', '*',
    'event', 'USERNAME_INSERT',
    'user-name', newUserName,
    'timestamp', timestamp
)

-- 添加新用户
redis.call(
    'HSET', newUserKey,
    userNameField, newUserName, userFundsField, newUserFunds
)

-- 为该用户添加初始武器
if #initWeapons > 0 then
    redis.call('RPUSH', userInventoryKey, unpack(initWeapons))
end

local uuid = string.match(newUserKey, ":([^:]+)")

if not uuid then
    uuid = "unknow"
end

-- 用户数据的审计信息
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'NEW_USER_CREATE',
    'uuid', "\"" ..uuid.. "\"",
    'user-name', newUserName,
    'user-funds', newUserFunds,
    'timestamp', timestamp
)

-- 用户包裹数据的审计信息
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_INBOUND',
    'uuid', uuid,
    'user-name', newUserName,
    'amount', #initWeapons,
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'