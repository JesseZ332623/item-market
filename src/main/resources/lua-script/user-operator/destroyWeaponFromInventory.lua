--[[
    用户销毁包裹中的某个武器。

    KEYS:
        inventoryKey 用户包裹键（如：inventories:114935169325609268）
        userKey      用户键（如：users:114934523722107784）

    ARGV:
        uuid        用户 UUID
        weaponName  武器名
]]
local inventoryKey = KEYS[1]
local userKey      = KEYS[2]

local uuid       = ARGV[1]
local weaponName = ARGV[2]

local timestamp = redis.call('TIME')[1]

local userName = redis.call('HGET', userKey, "\"name\"")

redis.call('LREM', inventoryKey, 1, weaponName)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_DESTROY',
    'uuid', uuid,
    'user-name', userName,
    'weapon-name', weaponName,
    'amount', '1',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'