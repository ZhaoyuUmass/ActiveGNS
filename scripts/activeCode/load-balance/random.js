/**
 * value is a Javascript collection as follows:
 * {"A":{ "record":[ip1, ip2, ...], "ttl": ttl } }
 */
 
function run(value, accessor, querier) {
  var records = value["A"]["record"];
  var rand = records[Math.ceil(Math.random()*records.length)-1];
  var r = {};
  r["A"] = {};
  r["A"]["ttl"] = 30;
  r["A"]["record"] = [];
  r["A"]["record"].push(rand);
  print(JSON.stringify(r));
  return r;
}
