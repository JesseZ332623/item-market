--[[
    为用户的包裹添加一件武器。

    KEYS:
        userKey 用户键（如：users:114934523722107784）

    ARGV:
        weapon 武器类型（Broadsword、Mace、Halberd 等）
]]
local userKey = KEYS[1]
local weapon  = ARGV[1]

local uuid    = string.match(userKey, ":([^:]+)")

local timestamp = redis.call('TIME')[1]

redis.call('RPUSH', userKey, weapon)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_INBOUND',
    'uuid', uuid,
    'user-name', '---',
    'amount', '1',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'