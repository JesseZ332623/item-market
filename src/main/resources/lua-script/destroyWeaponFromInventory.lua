--[[
    用户销毁包裹中的某个武器。

    KEYS:
        inventoryKey 用户包裹键（如：inventories:114935169325609268）

    ARGV:
        uuid        用户 UUID
        weaponName  武器名
]]
local inventoryKey = KEYS[1]

local uuid       = ARGV[1]
local weaponName = ARGV[2]

local timestamp = redis.call('TIME')[1]

redis.call('LREM', inventoryKey, 1, weaponName)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_DESTORY',
    'uuid', uuid,
    'user-name', '---',
    'amount', '1',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'