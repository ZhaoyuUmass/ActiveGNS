/**
 * value is a Javascript collection as follows:
 * {"A":{ "record":[ip1, ip2, ...], "ttl": ttl } }
 */

function subnet(ip){
	var pos = ip.lastIndexOf(".");
	var net = ip.substring(0,pos)+".0";
	return net;
}

// high ttl is set for hot region, low ttl is set for normal region
function run(value, field, querier) {
	var thres = 0.1;
	// set to ttl
	var interval = value["A"]["ttl"];
	var ip = subnet(value["ip"]);
	var records = value["A"]["record"];
	var d = new Date();
	var rate = null;
	
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
		value["A"]["ttl"] = ttl*2;
	}
	
	var rand = records.get(Math.ceil(Math.random()*records.length())-1);
	var value["A"]["record"] = [rand];
	
	// update all values
	var update = {ip:rate};
	querier.writeGuid("", update);
	return value;
}