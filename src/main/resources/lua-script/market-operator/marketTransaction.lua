--[[
    市场武器交易脚本。

    KEYS:
        sellerUserKey       卖家用户键（如：users:114940680399943670）
        buyerUserKey        买家用户键（如：users:114950910119824488）
        buyerInventoryKey   买家包裹键（如：inventories:114950910119824488）
        weaponHashKey       市场武器信息哈希键（如：market:weapon-market:weapons:1985f067af74d6d）
        weaponZsetKey       市场武器价格有序集合键（market:weapon-market:weapon-price）

    ARGV:
        buyerUUID  买家 UUID
        sellerUUID 卖家 UUID
        weaponId   武器 UID
]]

local sellerUserKey      = KEYS[1]
local buyerUserKey       = KEYS[2]
local buyerInventoryKey  = KEYS[3]
local weaponHashKey      = KEYS[4]
local weaponZsetKey      = KEYS[5]

local buyerUUID  = ARGV[1]
local sellerUUID = ARGV[2]
local weaponId   = ARGV[3]

local timestamp = redis.call('TIME')[1]

-- redis.log(
--     redis.LOG_NOTICE,
--     "sellerUserKey = " ..sellerUserKey..
--     " buyerUserKey = " ..buyerUserKey..
--     " buyerInventoryKey = " ..buyerInventoryKey..
--     " weaponHashKey = " ..weaponHashKey..
--     " weaponZsetKey = " ..weaponZsetKey..
--     " buyerUUID = " ..buyerUUID..
--     " sellerUUID = " ..sellerUUID..
--     " weaponId = " ..weaponId
-- )

-- 禁止左手倒右手
if
    buyerUUID == sellerUUID
then
    return '{"result": "SELF_TRANSACTIONAL"}'
end

-- 在市场上查询武器价格
local weaponPrice
    = redis.call('ZSCORE', weaponZsetKey, weaponId)

if not weaponPrice
then
    return '{"result": "WEAPON_NOT_FOUND"}'
end

-- 查询买家的资金，并判断其是否存在以及能否购买本武器
local buyerFunds
    = redis.call('HGET', buyerUserKey, "\"funds\"")

if not buyerFunds
then
    return '{"result": "BUYER_FUNDS_NOT_FOUND"}'
end

buyerFunds = string.gsub(buyerFunds, '"', '')

-- 格式化武器价格和买家资金为两位小数

local formatWeaponPrice = tonumber(string.format("%.2f", weaponPrice))
local formatBuyerFunds  = tonumber(buyerFunds)

if formatBuyerFunds < formatWeaponPrice
then
    return '{"result": "BUYER_FUNDS_NOT_ENOUGH"}'
end

local weaponName
    = redis.call('HGET', weaponHashKey, "\"weapon-name\"")

local sellerName
    = redis.call('HGET', sellerUserKey, "\"name\"")

local buyerName
    = redis.call('HGET', buyerUserKey, "\"name\"")

-- 所有校验完毕，正式执行交易操作（别忘记记录审计信息）
-- 删除市场上的武器信息
redis.call('DEL', weaponHashKey)
redis.call('ZREM', weaponZsetKey, weaponId)
redis.call(
    'XADD',
    'market:log', '*',
    'event', 'WEAPON_SOLD',
    'weaponId', weaponId,
    'weaponName', weaponName,
    'seller', sellerUUID,
    'buyer', buyerUUID,
    'timestamp', timestamp
)

-- 卖家资金增加
redis.call('HINCRBYFLOAT', sellerUserKey, "\"funds\"", formatWeaponPrice)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_FUNDS_INCR',
    'uuid', sellerUUID,
    'user-name', sellerName,
    'user-funds', "+" ..formatWeaponPrice,
    'timestamp', timestamp
)

-- 买家资金减少
redis.call('HINCRBYFLOAT', buyerUserKey, "\"funds\"", -formatWeaponPrice)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_FUNDS_INCR',
    'uuid', buyerUUID,
    'user-name', buyerName,
    'user-funds', "-" ..formatWeaponPrice,
    'timestamp', timestamp
)

-- 将武器移库至买家包裹
redis.call('RPUSH', buyerInventoryKey, weaponName)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_INBOUND',
    'uuid', buyerUUID,
    'user-name', buyerName,
    'amount', '1',
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'
