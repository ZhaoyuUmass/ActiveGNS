/**
 * value is a Javascript collection as follows:
 * {"A":{ "record":[ip1, ip2, ...], "ttl": ttl } }
 */

function run(value, field, querier) {
	var thres = 0.1;
	// set to ttl
	var interval = value["A"]["ttl"];
	var ip = value["ip"];
	var records = value["A"]["record"];
	var pointer = querier.readGuid("", "pointer");
	var rate = querier.readGuid("", ip);
	if(rate[ip] > thres){
		var pos = pointer["pointer"]["hot"];
		pointer["pointer"]["hot"] = (pos + 1)%records.length;
		value["A"]["record"] = [records[pos]];
	}else{
		var pos = pointer["pointer"]["normal"];
		pointer["pointer"]["hot"] = (pos + 1)%records.length;
		value["A"]["record"] = [records[pos]];
	}
	
	// update all values
	var update = {"pointer":pointer["pointer"], "ip":rate[ip]+1/interval};
	querier.writeGuid("", update);
	return value;
}