--[[
    用户删除自己最近联系人列表的某个用户。

    KEYS:
        contactKey    用户联系人列表键（如：contact:114968372130032550）
        contactLogKey 用户最近联系人日志键（contact:log）

    ARGV:
        uuid        哪个用户要删除一条最近联系人？
        contactName 要删除哪个用户？
]]
local contactKey    = KEYS[1]
local contactLogKey = KEYS[2]

local uuid         = ARGV[1]
local contactName  = ARGV[2]

local timestamp = redis.call('TIME')[1]

if
    redis.call('LREM', contactKey, 1, contactName) == 0
then
    return '{"result": "CONCAT_NAME_NOT_FOUND"}'
end

redis.call(
    'XADD',
    contactLogKey, '*',
    'event', 'REMOVE_CONTACT',
    'uuid', uuid,
    'contactName', contactName,
    'timestamp', timestamp
)

return '{"result": "SUCCESS"}'