/**
 * Created by gaozy on 12/27/16.
 * This piece of code is used when a DNS requests coming in.
 * We devide all clients into 7 groups based on the continent.
 */

function isLargerThan(value) {
    return function(element, index, array) {
        return (element > value);
    }
}

function run(value, accessor, querier) {
    if(!value.hasOwnProperty("A")){
        // if this is not a DNS request, return the original value
        return value;
    }

    var records = value["A"]["record"],
        client = value["client_ip"],
        weight = querier.readGuid(null, "weight")["weight"],
        load = querier.readGuid(null, "load")["weight"],
        decision = querier.readGuid(null, "decision")["decision"],
        locs = [],
        i = -1;

    if (client == undefined) {
        // if client ip does not exist, fetch a ip from the test field
        client = querier.readGuid(null, "testIp")["testIp"];
    }

    // query the location info through geoip for client ip address
    locs.push(client);
    var coords = querier.getLocations(locs); // the returned value is formatted as {ip1: {"latitude":lat1, "longitude":lng1},...}

    // retrieve the continent info and get the corresponding weight
    var continent = coords[client]["continent"];
    var w = weight[continent];
    var indexes = w.filter(isLargerThan(0));

    // figure out the index of the chosen replica
    var r = Math.random(),
        i = -1;
    while(r>=0){
        i++;
        r = r - w[indexes[i]];
    }

    // strip off the replicas not being chosen
    records.splice(indexes[i]+1, records.length);
    records.splice(0, indexes[i]);

    // update for this request
    var json = {};
    load[continent] = load[continent] + 1;
    json["load"] = load;
    decision[indexes[i]] = decision[indexes[i]] + 1;
    json["decision"] = decision; // collected by the self-running process to figure out the proportion of requests directed to replica i
    querier.writeGuid(null, "", json);

    return value;
}