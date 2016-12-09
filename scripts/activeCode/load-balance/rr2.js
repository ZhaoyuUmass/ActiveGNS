/**
 * value is a Javascript collection as follows:
 * {"A":{ "record":[ip1, ip2, ...], "ttl": ttl } }
 */

function subnet(ip){
	var pos = ip.lastIndexOf(".");
	var net = ip.substring(0,pos)+".0";
	return net;
}

function run(value, field, querier) {
	var thres = 0.1;
	// set to ttl
	var interval = value["A"]["ttl"];
	var ip = subnet(value["ip"]);
	var records = value["A"]["record"];
	var d = new Date();
	var rate = null;
	var pointer = null;
	
	try{
		pointer = querier.readGuid("", "pointer");
	} catch(err){
		// field "pointer" doesn't exist, create one
		pointer = {"hot":0,"normal":0};
	}

	try{		
		rate = querier.readGuid("", ip);
		if( d.getTime()/1000-rate[ip]["time"] <= interval ){
			rate[ip]["rate"] = rate[ip]["rate"] + 1/interval;
		}else{
			rate[ip]["rate"] = 1/interval;
			rate[ip]["time"] = d.getTime()/1000;
		}
	}catch(err){
		// there is no record for this subnet		
		rate = {ip:{"rate":1/interval, "time":d.getTime()/1000}}
	}
	if(rate[ip]["rate"] > thres){
		var pos = pointer["pointer"]["hot"];
		pointer["pointer"]["hot"] = (pos + 1)%records.length;
		value["A"]["record"] = [records[pos]];
	}else{
		var pos = pointer["pointer"]["normal"];
		pointer["pointer"]["hot"] = (pos + 1)%records.length;
		value["A"]["record"] = [records[pos]];
	}
	
	// update all values
	var update = {"pointer":pointer["pointer"], ip:rate};
	querier.writeGuid("", update);
	return value;
}