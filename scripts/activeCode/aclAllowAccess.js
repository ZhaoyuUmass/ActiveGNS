function run(value, field, querier) {
	var accessor = value.get("accessorGuid");
	var allowed = //replace with guid;
	var allowed_public_key = //replace with public key;
	if (allowed.localeCompare(accessor)==0){
		value.put("publicKey", allowed_public_key);
	}
	return value;
}