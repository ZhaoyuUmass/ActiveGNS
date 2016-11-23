function run(value, field, querier) {
	var ip = "1.1.1.1";
	var loc = querier.getLocations(ip);
	return value.put(field, loc);
}