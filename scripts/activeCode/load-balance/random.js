/**
 * value is a Javascript collection as follows:
 * {"A":{ "record":[ip1, ip2, ...], "ttl": ttl } }
 */
 
function run(value, accessor, querier) {
  print("!!!!!! value:"+JSON.stringify(value));
  var records = value["A"]["record"];
  print("!!!!!! record:"+records.toString());
  var rand = records[Math.ceil(Math.random()*records.length)-1];
  var r = {};
  r["A"] = {};
  r["A"]["ttl"] = 30;
  r["A"]["record"] = [];
  r["A"]["record"].push(rand);
  print("!!!!!! ret:"+JSON.stringify(r));
  return r;
}