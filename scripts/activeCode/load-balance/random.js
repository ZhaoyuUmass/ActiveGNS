/**
 * value is a Javascript collection as follows:
 * {"A":{ "record":[ip1, ip2, ...], "ttl": ttl } }
 *
 * Feb 7th, 2017
 */
 
function run(value, accessor, querier) {
  var records = value["A"]["record"];
  var rand = Math.ceil(Math.random()*records.length)-1;
  value["A"]["record"] = records.slice(rand, rand+1);
  return value;
}