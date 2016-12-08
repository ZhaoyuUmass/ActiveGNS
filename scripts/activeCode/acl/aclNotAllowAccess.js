function run(value, field, querier) {
	var accessor = value.get("accessorGuid");
	var unallowed = //replace with guid;
	if (unallowed.localeCompare(accessor) == 0){
		value.put("publicKey", null);
	}
	return value;
}