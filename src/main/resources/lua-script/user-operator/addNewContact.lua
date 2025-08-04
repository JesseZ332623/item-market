--[[
    用户记录另一个用户为最近联系人。

    KEYS:
        newUserKey       用户键（如：users:114934523722107784）
        contactKey    用户联系人列表键（如：contact:114968372130032550）
        contactLogKey 用户最近联系人日志键（contact:log）

    ARGV:
        uuid        哪个用户要添加一条最近联系人？
        contactName 哪个用户成为了它的最近联系人？
        maxContact  联系人列表的最大数量（大小有所限制，防止内存滥用）
]]
local userKey       = KEYS[1]
local contactKey    = KEYS[2]
local contactLogKey = KEYS[3]

local uuid        = ARGV[1]
local contactName = ARGV[2]
local maxContact  = math.min(tonumber(ARGV[3]), 175)

local timestamp = redis.call('TIME')[1]

-- 无论如何，不能添加自己为最近联系人
if
    contactName == redis.call('HGET', userKey, "name")
then
    return '{"result": "SELF_ADDED"}'
end

-- 直接尝试删除指定联系人，
-- 如果他存在与列表中的话，会返回 1
if
    redis.call('LREM', contactKey, 1, contactName) == 1
then
    redis.call(
        'XADD',
        contactLogKey, '*',
        'event', 'REMOVE_CONTACT',
        'uuid', uuid,
        'contactName', contactName,
        'timestamp', timestamp
    )
end

-- 将联系人插入列表，使之成为最新的一个联系人
redis.call('LPUSH', contactKey, contactName)
redis.call(
    'XADD',
    contactLogKey, '*',
    'event', 'ADD_NEW_CONTACT',
    'uuid', uuid,
    'contactName', contactName,
    'timestamp', timestamp
)

local currentListLen = redis.call('LLEN', contactKey);
-- 插入完成后，需要检查列表长度有没有超过 maxContact
-- 若有，则修剪列表，丢弃索引超过 maxContact 的元素
if
    currentListLen > maxContact
then
    -- 在正式修剪前，
    -- 先弹出被修剪的联系人名字，用于日志记录
    local trimContactName = redis.call('RPOP', contactKey)

    redis.call('LTRIM', contactKey, 0, maxContact - 1)
    redis.call(
        'XADD',
        contactLogKey, '*',
        'event', 'TRIM_CONTACT_LIST',
        'uuid', uuid,
        'contactName', trimContactName,
        'timestamp', timestamp
    )
end

return '{"result": "SUCCESS"}'

