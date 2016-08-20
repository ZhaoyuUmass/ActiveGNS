function run(value, field, querier) {
	//substitute this line with the targetGuid
	value.put("someField", querier.readGuid(targetGuid, "depthField").get("depthField"));
	return value;
}