local zsetKeys = KEYS
local totalDeleted = 0

for _, zsetKey in ipairs(zsetKeys) do
    local cursor = 0
    repeat
        local result = redis.call('ZSCAN', zsetKey, cursor, 'COUNT', 100)
        cursor = tonumber(result[1])
        local members = result[2]

        for i = 1, #members, 2 do
            local memberKey = members[i]

            if redis.call('EXISTS', memberKey) == 0 then
                redis.call('ZREM', zsetKey, memberKey)
                totalDeleted = totalDeleted + 1
            end
        end
    until cursor == 0
end

return totalDeleted
