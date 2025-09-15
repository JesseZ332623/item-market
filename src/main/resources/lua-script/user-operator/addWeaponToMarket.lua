--[[
    用户将武器上架至市场。

    KEYS:
        weaponHashKey           挂在市场上的武器键（如：market:weapon-market:weapons:1985f067af74d6d）
        weaponPriceZsetKey      挂在市场上的武器价格键（market:weapon-market:weapon-price）
        sellerInventoryListKey  卖家包裹键（如：inventories:114935169325609268）
        userKey                 用户键（如：users:114934523722107784）

    ARGV:
        weaponUUID 武器 ID
        sellerUUID 卖家 UUID
        weaponName 武器名
        weaponPrice 武器价格
]]

local weaponHashKey          = KEYS[1]
local weaponPriceZsetKey     = KEYS[2]
local sellerInventoryListKey = KEYS[3]
local userKey                = KEYS[4]

local weaponUUID  = ARGV[1]
local sellerUUID  = ARGV[2]
local weaponName  = ARGV[3]
local weaponPrice = ARGV[4]

local timestamp = redis.call('TIME')[1]

local userName = redis.call('HGET', userKey, "\"name\"")

-- 从用户包裹中移除指定武器
if redis.call('LREM', sellerInventoryListKey, 1, weaponName) == 0
then
    return '{"result": "INVENTORY_REM_FAILED"}'
end

-- 用户包裹数据的审计信息
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_OUTBOUND',
    'uuid', sellerUUID,
    'user-name', userName,
    'weapon-name', weaponName,
    'amount', '1',
    'timestamp', timestamp
)

redis.call(
    'HSET', weaponHashKey,
    '"weapon-name"', weaponName, '"seller"', sellerUUID
)
redis.call(
    'ZADD', weaponPriceZsetKey,
    'NX',
    string.format("%.2f", weaponPrice),
    weaponUUID
)

redis.call(
    'XADD',
    'market:log', '*',
    'event', 'WEAPON_LISTING',
    'weaponId', weaponUUID,
    'weaponName', weaponName,
    'seller', sellerUUID,
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'