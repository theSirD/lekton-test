-- KEYS[1] = stock key, KEYS[2] = reserve key
-- ARGV[1] = quantity, ARGV[2] = ttl seconds
local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
local quantity = tonumber(ARGV[1])
if stock < quantity then
  return 0
end
redis.call('DECRBY', KEYS[1], quantity)
redis.call('SET', KEYS[2], quantity, 'EX', tonumber(ARGV[2]))
return 1
