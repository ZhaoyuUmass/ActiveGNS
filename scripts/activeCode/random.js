function run(value, field, querier) {
	var records = value.get(field);
	var index = Math.ceil(Math.random()*records.length())-1;
	for(var i=0; i<records.length(); i++){
		if(i!=index)
			records.remove(i);
	}
	return value.put(field, records);
}