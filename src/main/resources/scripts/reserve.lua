-- ========================================
-- Reserve tickets (atomic, anti-oversell)
-- ========================================
-- KEYS[1] = available key       (ticket:available:{id})
-- KEYS[2] = reserved key        (ticket:reserved:{id})
-- ARGV[1] = quantity

local qty = tonumber(ARGV[1])

-- validate input
if not qty or qty <= 0 then
    return -2 -- invalid quantity
end

-- check key exists
if redis.call("EXISTS", KEYS[1]) == 0 then
    return -1 -- not initialized
end

-- decrease first (atomic)
local newAvailable = redis.call("DECRBY", KEYS[1], qty)

-- nếu bị âm → rollback
if newAvailable < 0 then
    redis.call("INCRBY", KEYS[1], qty)
    return 0 -- not enough stock
end

-- tăng reserved
redis.call("INCRBY", KEYS[2], qty)

return 1 -- success