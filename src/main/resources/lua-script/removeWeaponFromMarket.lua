--[[
    用户从市场上下架某个武器。

    KEYS:
        weaponPriceZsetKey      挂在市场上的武器价格键（market:weapon-market:weapon-price）
        sellerInventoryListKey  卖家包裹键（如：inventories:114935169325609268）

    ARGV:
        sellerUUID 卖家 UUID
        weaponName 武器名
]]
local weaponPriceZsetKey     = KEYS[1]
local sellerInventoryListKey = KEYS[2]

local sellerUUID  = ARGV[1]
local weaponName  = ARGV[2]

local timestamp = redis.call('TIME')[1]

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

        local scanWeaponName, scanSellerUUID
            = unpack(fields)

        local weaponId
            = string.match(weaponKey, ".*:(.*)")

        if
            scanWeaponName
            and scanSellerUUID
            and scanSellerUUID == sellerUUID
            and scanWeaponName == weaponName
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

-- 重新将武器放回对应用户的包裹中
redis.call('RPUSH', sellerInventoryListKey, weaponName)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_INBOUND',
    'uuid', sellerUUID,
    'user-name', '---',
    'amount', '1',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'