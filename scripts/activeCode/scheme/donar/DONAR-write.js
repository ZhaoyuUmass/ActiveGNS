/**
 * Created by gaozy on 12/27/16.
 */

// a distance function
function distance(lat1, lon1, lat2, lon2) {
    if(lat1 == undefined || lat2 == undefined) return Number.MAX_VALUE;
    var R = 6371; // Radius of the earth in km
    var dLat = (lat2 - lat1) * Math.PI / 180;  // deg2rad below
    var dLon = (lon2 - lon1) * Math.PI / 180;
    var a =
        0.5 - Math.cos(dLat)/2 +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        (1 - Math.cos(dLon))/2;

    return R * 2 * Math.asin(Math.sqrt(a));
}


function sum(x, y){
    return x+y;
}


function run(value, accessor, querier) {
    if(!value.hasOwnProperty("lambda") && !value.hasOwnProperty("p")){
        // if this is not a periodical update request for the price and redirection probability field of a DONAR mapping node
        return value;
    }

    // constant
    const theta = 0.01,
        clientRegion = ["EU", "AS", "NA", "SA", "AF", "OC", "AN"];

    // 1. collect the latest P_ni for all other nodes
    // 2. collect the latest lambda_i for every replica i
    var p = value["p"],// the traffic load contributed by all other mapping nodes, independent of this node's decision
        // price : lambda
        lambda = value["lambda"],
        // weight of each replica define by user: w
        w = querier.readGuid(null, "w")["w"],
        // deviation for each replica: epsilon
        epsilon = querier.readGuid(null, "epsilon")["epsilon"],
        // # reqs from client c: can be used to calculate alpha
        clientReqs = querier.readGuid(null, "clientReqs")["clientReqs"],
        // the set of clients that is divided into a few groups
        regionInfo = querier.readGuid(null, "regionInfo")["regionInfo"],
        // the A record to fetch the information of replicas
        replicas = querier.readGuid(null, "A")["A"]["record"],
        dist = [],
        alpha = [],
        Q = [],
        c = [],
        A = [],
        h = [],
        weight = {}, // the real weight for request redirection
        numReplicas = replicas.length,
        numClientRegions = clientRegion.length,
        total = numReplicas*numClientRegions,
        s = querier.readGuid("null", "s")["s"];

    // calculate alpha
    var total = clientReqs.reduce(sum);
    clientReqs.forEach(function(element, index){alpha[index] = element/total});

    // get locations for all replicas through GeoIP
    var coords = querier.getLocations(replicas);

    // calculate the distance between client regions and replicas
    for(i=0; i<replicas.length; i++){
        dist.push(new Array());
        for(var j=0; j<clientRegion.length; j++) {
            dist[i].push(Math.round(distance(coords[replicas[i]]["latitude"], coords[replicas[i]]["longitude"],
                regionInfo[clientRegion[j]]["latitude"], regionInfo[clientRegion[j]]["longitude"])));
        }
    }


    // 3. solve optimization problem
    var sqr = function(x) { return x*x; };
    for(var i=0; i<total; i++){
        Q.push([]);
        A.push([]);
        var xi = Math.floor(i/numReplicas),
            xj = i%numReplicas;
        for(var j=0; j<total; j++){

            var yi = Math.floor(j/numReplicas),
                yj = j%numReplicas;

            if(xi==yi){
                Q[i].push(lambda[xi]*sqr(s)*alpha[xj]*alpha[yj]);

            }else{
                Q[i].push(0);
            }
            A[i].push(0);
        }
        c.push(lambda[xi]*(p[xi]-w[xi])*s*alpha[xj]+s*alpha[xj]*dist[xi][xj]);
        h.push(0);
    }
    // use numeric to solve the quadratic programming problem
    var solution = numeric.solveQP(Q, c, A, h);
    var policy = solution["unconstrained_solution"];
    for(i=0; i<numClientRegions; i++){
        weight[clientRegion[i]] = policy.slice(i*numReplicas, (i+1)*numReplicas);
    }
    value["weight"] = weight;
    print(JSON.stringify(weight));

    // 4. compute p_ni and update for this mapping node
    var P = [];
    for(i=0; i<numReplicas; i++){
        var total = 0;
        for(var j=0; j<numClientRegions; j++){
            total += weight[clientRegion[j]][i]*alpha[j];
        }
        P.push(total*s);
    }
    value["P"] = P;
    print(P);

    // 5. Update lambda
    for(i=0; i<numReplicas; i++){
        var r = Math.random();
        if(r<1/numReplicas){
            // update
            print("Before updating: "+i+" "+lambda);
            lambda[i] = Math.max(0, lambda[i]+theta*(sqr(P[i]+p[i]-w[i]) - sqr(epsilon[i])));
            print("After updating: "+i+" "+lambda);
        }
    }
    // return result
    print(JSON.stringify(value));
    return value;
}