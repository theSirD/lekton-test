-- KEYS[1] = stock key, KEYS[2] = reserve key
local quantity = redis.call('GET', KEYS[2])
if not quantity then
  return 0
end
redis.call('DEL', KEYS[2])
redis.call('INCRBY', KEYS[1], tonumber(quantity))
return 1
