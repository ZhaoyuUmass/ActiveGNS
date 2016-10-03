function run(value, field, querier) {
	var records = value.get(field);
	var index = Math.ceil(Math.random()*2)-1;
	records.remove(index);
	return value.put(field, records);
}