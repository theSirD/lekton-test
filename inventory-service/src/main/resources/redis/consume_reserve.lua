-- KEYS[1] = reserve key
local quantity = redis.call('GET', KEYS[1])
if not quantity then
  return 0
end
redis.call('DEL', KEYS[1])
return 1
