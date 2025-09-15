--[[
    用户从市场上下架某个武器。

    KEYS:
        weaponPriceZsetKey      挂在市场上的武器价格键（market:weapon-market:weapon-price）
        sellerInventoryListKey  卖家包裹键（如：inventories:114935169325609268）
        userKey                 用户键（如：users:114934523722107784）

    ARGV:
        sellerUUID 卖家 UUID
        weaponName 武器名
]]
local userKey                = KEYS[1]
local sellerInventoryListKey = KEYS[3]

local sellerUUID  = ARGV[1]
local weaponName  = ARGV[2]

local timestamp = redis.call('TIME')[1]

local foundWeapon = false
local weaponId
local userName    = redis.call('HGET', userKey, "\"name\"")

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

    cursor = result[1]                  -- 获取当前游标值
    local weaponHashKeys = result[2]    -- 获取本页所有匹配的武器哈希键

    for i, weaponKey in ipairs(weaponHashKeys) do
        local fields 
            = redis.call(
                'HMGET', weaponKey, 
                "\"weapon-name\"", "\"seller\""
            )

        local scanWeaponName, scanSellerUUID
            = unpack(fields)

        if
            scanWeaponName
            and scanSellerUUID
            and scanSellerUUID == sellerUUID
            and scanWeaponName == weaponName
        then
            weaponId
                = string.match(weaponKey, ".*:(.*)")

            redis.call('DEL', weaponKey)
            redis.call(
                'ZREM',
                "market:weapon-market:weapon-price",
                "\"" ..weaponId.. "\""
            )
            redis.call(
                'XADD',
                'market:log', '*',
                'event', 'WEAPON_OUTBOUND',
                'weaponId', "\"" ..weaponId.. "\"",
                'weaponName', weaponName,
                'seller', sellerUUID,
                'timestamp', timestamp
            )

            foundWeapon = true  -- 只需要下架一件武器
            break
        end
    end

    -- 如果已经找到武器，提前结束SCAN
    if foundWeapon then
        break
    end
until cursor == "0"

-- 只有在找到并下架武器后才将其放回用户库存
if foundWeapon then

    -- 重新将武器放回对应用户的包裹中
    redis.call('RPUSH', sellerInventoryListKey, weaponName)
    redis.call(
            'XADD',
            'inventories:log', '*',
            'event', 'WEAPON_INBOUND',
            'uuid', sellerUUID,
            'user-name', userName,
            'weapon-name', weaponName,
            'amount', '1',
            'timestamp', timestamp
    )

    return '{"result": "SUCCESS"}'
else
    -- 如果用户在市场上没有上架指定武器，则返回错误消息
    return '{"result": "WEAPON_NOT_FOUND"}'
end