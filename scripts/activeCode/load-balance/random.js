/**
 * value is a Javascript collection as follows:
 * {"A":{ "record":[ip1, ip2, ...], "ttl": ttl } }
 */

function run(value, field, querier) {
	var records = value["A"]["record"];
	var rand = records.get(Math.ceil(Math.random()*records.length())-1);
	var value["A"]["record"] = [rand];
	return value;
}