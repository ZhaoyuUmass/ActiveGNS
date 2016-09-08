/**
 * Created by gaozy on 1/28/16.
 */

function run(value, field, querier) {
	/**
	 * The maximal value is 2^32-1 to create an array
	 * creating an array does not mean the memory of
	 * the array is pre-allocated. 
	 */
	var i=0;
	var arr=new Array();
	while(true){
		arr[i] = i;
		i++;
	}
    return value;
}