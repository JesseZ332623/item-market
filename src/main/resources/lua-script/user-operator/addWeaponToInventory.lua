--[[
    为用户的包裹添加一件武器。

    KEYS:
        userKey      用户键（如：users:114934523722107784）
        inventoryKey 用户包裹键（如：inventories:114934523722107784）

    ARGV:
        weapon 武器类型（Broadsword、Mace、Halberd 等）
]]
local userKey      = KEYS[1]
local inventoryKey = KEYS[2]

local weapon       = ARGV[1]

local uuid     = string.match(inventoryKey, ":([^:]+)")
local userName = redis.call('HGET', userKey, "\"name\"")

local timestamp = redis.call('TIME')[1]

redis.call('RPUSH', inventoryKey, weapon)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_INBOUND',
    'uuid', uuid,
    'user-name', userName,
    'weapon-name', weapon,
    'amount', '1',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'