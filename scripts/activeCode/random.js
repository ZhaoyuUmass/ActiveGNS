function run(value, field, querier) {
	var a = value.get("A");
	var records = a.get("record");
	var rand = records.get(Math.ceil(Math.random()*records.length())-1);
	var length = records.length();
	for (var i = length-1; i>=0; i--) {
		records.remove(i);
    }
    records.put(rand);
	return value.put("A", a.put("record", records).put("ttl",0));
}