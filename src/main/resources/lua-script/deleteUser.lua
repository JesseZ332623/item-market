--[[
    删除用户。

    KEYS:
        userKey          用户键（如：users:114934523722107784）
        userSetKey       用户集合键（用来保证用户名的唯一性）
        inventoryKey     用户包裹键（如：inventories:114935169325609268）
    ARGV:
        userNameField    用户名哈希字段名
        userFundsField   用户资金哈希字段名
]]
local userKey      = KEYS[1]
local userSetKey   = KEYS[2]
local inventoryKey = KEYS[3]

local userNameField  = ARGV[1]
local userFundsField = ARGV[2]

local timestamp = redis.call('TIME')[1]

-- 获取用户名字符串并检查
local userName = redis.call('HGET', userKey, userNameField)

if not userName then
    return '{"result": "USER_NOT_FOUND"}'
end

-- 删除用户集合中的用户名
redis.call('SREM', userSetKey, userName)

-- 添加删除用户集合的审计信息
redis.call(
    'XADD',
    'user-name:log', '*',
    'event', 'USERNAME_REMOVE',
    'user-name', userName,
    'timestamp', timestamp
)

-- 获取用户的 uuid
local targetUUID = string.match(userKey, "%:(.+)$")
targetUUID = "\"" ..targetUUID.. "\"";

-- 对于每一个 hash-key，查询买家 uuid 和 武器名，
-- 若买家 uuid 与传入的参数匹配，删除对应的整个哈希并添加审计信息
local cursor = "0"
repeat
    -- 分批次获取市场中的所有武器键
    local result
        = redis.call(
            'SCAN', cursor,
            'MATCH', 'market:weapon-market:weapons:*',
            'COUNT', 15,
            'TYPE', 'hash'
        )

    cursor = result[1]
    local weaponHashKeys = result[2]

    for i, weaponKey in ipairs(weaponHashKeys) do
        local fields = redis.call('HMGET', weaponKey, 'weapon-name', 'seller')

        local weaponName, sellerUUID = unpack(fields)

        local weaponId = string.match(weaponKey, ".*:(.*)")

        if
            sellerUUID and sellerUUID == targetUUID
        then
            redis.call('DEL', weaponKey)
            redis.call(
                'ZREM',
                "market:weapon-market:weapon-price",
                "\"" ..weaponId.. "\""
            )
            redis.call(
                'XADD',
                'market:log', '*',
                'event', 'WEAPON_DELETE',
                'weaponName', weaponName,
                'seller', sellerUUID,
                'timestamp', timestamp
            )
        end
    end
until cursor == "0"

-- 删除用户的包裹，并添加审计信息
local weaponAmount = redis.call('LLEN', inventoryKey)
redis.call('DEL', inventoryKey)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'INVENTORY_REMOVE',
    'uuid', targetUUID,
    'user-name', userName,
    'amount', weaponAmount,
    'timestamp', timestamp
)

-- 最后删除用户数据，并添加审计信息
redis.call('DEL', userKey)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_REMOVE',
    'uuid', targetUUID,
    'user-name', userName,
    'user-funds', '---',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'