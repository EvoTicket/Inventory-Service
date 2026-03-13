-- KEYS[1] = available key
-- KEYS[2] = reserved key
-- ARGV[1] = quantity

local available = tonumber(redis.call("GET", KEYS[1]) or "0")
local qty = tonumber(ARGV[1])

if available >= qty then
    redis.call("DECRBY", KEYS[1], qty)
    redis.call("INCRBY", KEYS[2], qty)
    return 1
else
    return 0
end
