--[[
    市场武器交易脚本（Redis 数据结构变更，暂未实现）。
]]
local buyerKey     = KEYS[1]
local sellerKey    = KEYS[2]
local marketKey    = KEYS[3]
local inventoryKey = KEYS[4]
local itemMember   = ARGV[1]
local weaponName   = ARGV[2]

local timestamp = redis.call('TIME')[1]

local buyerUUID  = string.match(buyerKey, ":([^:]+)")
local sellerUUID = string.match(sellerKey, ":([^:]+)")

redis.log(redis.LOG_NOTICE, "Starting transaction between " .. buyerKey .. " and " .. sellerKey)

-- 禁止左手倒右手
if buyerKey == sellerKey
then
    return '{"result": "SELF_TRANSACTION"}'
end

-- 获取武器价格和买家资金
local weaponPrice = redis.call('ZSCORE', marketKey, itemMember)
if not weaponPrice
then
    return '{"result": "ITEM_NOT_FOUND"}'
end

local buyerFunds = redis.call('HGET', buyerKey, 'funds')
if not buyerFunds
then
    return '{"result": "BUYER_FUNDS_NOT_FOUND"}'
end

weaponPrice = tonumber(string.format("%.2f", weaponPrice)
buyerFunds  = tonumber(string.format("%.2f", buyerFunds))

redis.log(redis.LOG_NOTICE, "Weapon price = " .. weaponPrice .. " Buyer funds = " .. buyerFunds)

-- 验证资金和物品存在
if buyerFunds < weaponPrice
then
    return '{"result": "INSUFFICIENT_FUNDS"}'
end

-- 原子化执行所有操作
redis.call('ZREM', marketKey, itemMember)
redis.call(
    'XADD',
    'market:user-operator:log', '*',
    'event', 'WEAPON_OUTBOUND',
    'uuid', sellerUUID,
    'item-name', weaponName,
    'weapon-price', price,
    'timestamp', timestamp
)

redis.call('RPUSH', inventoryKey, weaponName)
redis.call(
    'XADD',
    'inventories:log', '*',
    'event', 'WEAPON_INBOUND',
    'uuid', buyerUUID,
    'weapon', weaponName,
    'amount', '1'
    'timestamp', timestamp
)

redis.call('HINCRBYFLOAT', buyerKey, 'funds', -weaponPrice)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_FUNDS_REDUCE',
    'uuid', buyerUUID,
    'user-name', '---',
    'user-funds', "-" ..weaponPrice,
    'timestamp', timestamp
)

redis.call('HINCRBYFLOAT', sellerKey, 'funds', weaponPrice)
redis.call(
    'XADD',
    'users:log', '*',
    'event', 'USER_FUNDS_INCR',
    'uuid', sellerUUID,
    'user-name', '---',
    'user-funds', "+" ..weaponPrice,
    'timestamp', timestamp
)

-- 最后添加审计日志
redis.call(
    'XADD',
    'market:trade-operator:log', '*',
    'event', 'WEAPON_TRADE',
    'buyer', buyerKey,
    'seller', sellerKey,
    'item', weaponName,
    'weapon-price', weaponPrice,
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'
