// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });

// The Firebase Admin SDK to access the Firebase Realtime Database. 
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

const Promise = require('promise');

const MIN_WALK_SPEED = 0;//2.5;
const MIN_RUN_SPEED = 8;
const MIN_VEHICULE_SPEED = 20;
const MAX_VEHICULE_SPEED = 80;

/*Database structures:

/segments/{segmentUID}:
{
  "date" : 1511661260282,
  "destination" : {
	"altitude" : double,
	"east" : double,
	"north" : double
  },
  "origin" : {
	"altitude" : double,
	"east" : double,
	"north" : double
  },
  "speed" : double,
  "userid" : "{userID}"
}

/roadGraph/{intersectionUID}:
{
	"east" : double
	"north" : double
  "roads" : {
    "roadUID" : {
      "connectedUID" : intersectionUID,
      "distance" : double,
      "dirX" : double,
      "dirY" : double
    }, ...
  }
}
*/

const myTools = {
  vector: function(x, y) {
    this.init(x, y);
  },
  getParametrizedSegmentFromVectors(origin, destination) {
    const delta = destination.sub(origin);
    const distance = delta.length();
    const normale = delta.normalize();

    return {
      origin: origin,
      destination: destination,
      normale: normale,
      distance: distance
    };    
  },
  getParametrizedSegmentFromSegment: function(segment) {
    return this.getParametrizedSegmentFromVectors(
      new myTools.vector(segment.origin.east, segment.origin.north),
      new myTools.vector(segment.destination.east, segment.destination.north)
    );
  },
  distanceTolerance: 2.0, //About 2 meters
  findIntersection: function(seg1, seg2) {
    /*Segment has the following structure:
    {
      origin: vector,
      destination: vector,
      normale: vector (direction from 'origin' to 'destination',
      distance: double (distance along normale direction to go from 'origin' to 'destination')
    }
    */

    //Pre compute a few useful datas
    const UxV = seg1.normale.crossProduct(seg2.normale);
    const QmP = seg2.origin.sub(seg1.origin);
    const distance = QmP.crossProduct(seg1.normale);
    
    //Are segments parallels?
    if (UxV == 0) {
      //I don't want to mess with 100% colinear segment, overlapping portion, etc
      //so if parrallel, let's assume they do not intersect
      return null;
    }
    else {
      //Not parallel
      
      //Compute intersecting s and t
      const dividedQmP = QmP.divide(UxV);
      const s = dividedQmP.crossProduct(seg2.normale);
      const t = dividedQmP.crossProduct(seg1.normale);
      
      if (s >= 0 && s <= seg1.distance && t >= 0 && t <= seg2.distance) {
        //There's an intersection
        return seg1.origin.add(seg1.normale.mult(s));
      }
    }
    
    return null;
  },
  createNode: function(roadGraphSnapShot, roadGraphData, coordinate) {
    //Create node data
    const node = {
      east: coordinate.x,
      north: coordinate.y
    };
    
    //Insert new node and fetch's it's key
    const key = roadGraphSnapShot.ref.push(node).key;

    //Update local model
    roadGraphData[key] = node;
    
    return key;
  },
  createRoad: function(roadGraphSnapShot, roadGraphData, fromKey, toKey) {
    const fromNode = roadGraphData[fromKey];
    const toNode = roadGraphData[toKey];
    const fromPos = new myTools.vector(fromNode.east, fromNode.north);
    const toPos = new myTools.vector(toNode.east, toNode.north);
    const delta = toPos.sub(fromPos);
    const distance = delta.length();
    
    const dir = delta.normalize();
    const road1 = {
      distance: distance,
      dirX: dir.x,
      dirY: dir.y
    };
    const road2 = {
      distance: distance,
      dirX: -dir.x,
      dirY: -dir.y
    };
    
    //Update local model first
    if (!fromNode.roads) {
      fromNode.roads = {};
    }
    fromNode.roads[toKey] = road1;
    if (!toNode.roads) {
      toNode.roads = {};
    }
    toNode.roads[fromKey] = road2;
    
    //Then update database model
    roadGraphSnapShot.ref.child(fromKey + '/roads/' + toKey).set(road1);
    roadGraphSnapShot.ref.child(toKey + '/roads/' + fromKey).set(road2);
  },
	processSegment: function(roadGraphSnapShot, roadGraphData, newParamSegment, knownEndPoints) {
    //First pass, find close enough endpoints nodes, if not already know
    const knownOrig = !!knownEndPoints.origNodeKey;
    const knownDest = !!knownEndPoints.destNodeKey;
    
    if (!knownOrig || !knownDest) {
      var origNodeKey = knownEndPoints.origNodeKey;
      var origClosestDistance = knownOrig ? -1 : myTools.distanceTolerance * 2;
      var destNodeKey = knownEndPoints.destNodeKey;
      var destClosestDistance = knownDest ? -1 : myTools.distanceTolerance * 2;
      for (var nodeKey in roadGraphData) {
        const testNode = roadGraphData[nodeKey];
        
        //Make a vector out of node's coordinates
        const nodeCoord = new myTools.vector(testNode.east, testNode.north);
        
        //Compute distance to origin and destination
        const distToOrig = nodeCoord.sub(newParamSegment.origin).length();
        const distToDest = nodeCoord.sub(newParamSegment.destination).length();
        
        if (distToOrig < origClosestDistance && distToOrig < myTools.distanceTolerance) {
          //Origin is closer to that node
          origNodeKey = nodeKey;
          origClosestDistance = distToOrig;
        }
        if (distToDest < destClosestDistance && distToDest < myTools.distanceTolerance) {
          //Origin is closer to that node
          destNodeKey = nodeKey;
          destClosestDistance = distToDest;
        }
        
        //That's it, try another node
      }
      //All nodes are tested
      knownEndPoints.origNodeKey = origNodeKey;
      knownEndPoints.destNodeKey = destNodeKey;
    }

    //Second pass, find intersection
    const nodePairVisited = {}; //At the beggining, no pair is visited
    
    //Iterate each known node of the graph
    for (var origKey in roadGraphData) {
      //Avoid intersection test if origKey match a knownEndPoint
      if (origKey != knownEndPoints.origNodeKey && origKey != knownEndPoints.destNodeKey) {
        const origNode = roadGraphData[origKey];
        
        //Iterate each connected road for that node
        for (var destKey in origNode.roads) {
          //Avoid intersection test if destKey match a knownEndPoint
          if (destKey != knownEndPoints.origNodeKey && destKey != knownEndPoints.destNodeKey) {
            
            //Make sure that key as not yet been visited (avoid doing the job twice)
            var visitedKey = destKey + "." + origKey;
            if (!nodePairVisited[visitedKey]) {
              const destNode = roadGraphData[destKey];
              
              //Mark pair as visited
              nodePairVisited[origKey + "." + destKey] = true;
              
              
              //Compute a segment for the tested road
              const testedParamSegment = myTools.getParametrizedSegmentFromVectors(
                new myTools.vector(origNode.east, origNode.north),
                new myTools.vector(destNode.east, destNode.north)
              );
              
              //Compute an intersection
              var intersection = myTools.findIntersection(newParamSegment, testedParamSegment);
              
              if (intersection) {
                //We found an intersection
                
                //Is there already an existing node near that intersection?
                var interNodeKey = "";
                var interClosestDistance = myTools.distanceTolerance * 2;
                for (var interKey in roadGraphData) {
                  const interNode = roadGraphData[interKey];
                  
                  //Make a vector out of node's coordinates
                  const nodeCoord = new myTools.vector(interNode.east, interNode.north);
                  
                  //Compute distance to intersection
                  const distToInter = nodeCoord.sub(intersection).length();
                  
                  if (distToInter < interClosestDistance && distToInter < myTools.distanceTolerance) {
                    //Intersection is closer to that node
                    interNodeKey = interKey;
                    interClosestDistance = distToInter;
                  }
                  
                  //That's it, try another node
                }
                //All nodes are tested
                
                //We found a node to use for intersection
                if (interNodeKey) {
                  //Update intersection coordinate from node
                  intersection = new myTools.vector(
                    roadGraphData[interNodeKey].east,
                    roadGraphData[interNodeKey].north
                  );
                }
                else {
                  interNodeKey = myTools.createNode(roadGraphSnapShot, roadGraphData, intersection);
                }
                
                //If the intersection is not one of the tested road's end point, drop that road
                if (interNodeKey != origKey && interNodeKey != destKey) {
                  roadGraphSnapShot.ref.child(origKey + "/roads/" + destKey).remove();
                  roadGraphSnapShot.ref.child(destKey + "/roads/" + origKey).remove();
                  
                  //NOTE: Using delete is not a clean Javascript practice, but use it anyway
                  if (origNode.roads && origNode.roads[destKey]) {
                    delete origNode.roads[destKey];
                  }
                  if (destNode.roads && destNode.roads[origKey]) {
                    delete destNode.roads[origKey];
                  }
                }
                
                //Insert a new road for the origKey -> interNodeKey segment if they are not the same
                if (origKey != interNodeKey) {
                  myTools.createRoad(roadGraphSnapShot, roadGraphData, origKey, interNodeKey);
                }
                //Insert a new road for the interNodeKey -> destKey segment if they are not the same
                if (interNodeKey != destKey) {
                  myTools.createRoad(roadGraphSnapShot, roadGraphData, interNodeKey, destKey);
                }
                
                //Process new segment origin->intersection if known origin not the intersection
                if (knownEndPoints.origNodeKey != interNodeKey) {
                  const paramSegment = myTools.getParametrizedSegmentFromVectors(
                    (knownEndPoints.origNodeKey) ? 
                      new myTools.vector(
                        roadGraphData[knownEndPoints.origNodeKey].east, 
                        roadGraphData[knownEndPoints.origNodeKey].north
                      )
                      : newParamSegment.origin,
                    intersection
                  );

                  const newKnownEndPoints = {origNodeKey: knownEndPoints.origNodeKey, destNodeKey: interNodeKey};
                  myTools.processSegment(roadGraphSnapShot, roadGraphData, paramSegment, newKnownEndPoints);
                }

                //Process new segment intersection->destination if known destination not the intersection
                if (knownEndPoints.destNodeKey != interNodeKey) {
                  const paramSegment = myTools.getParametrizedSegmentFromVectors(
                    intersection,
                    (knownEndPoints.destNodeKey) ? 
                      new myTools.vector(
                        roadGraphData[knownEndPoints.destNodeKey].east, 
                        roadGraphData[knownEndPoints.destNodeKey].north
                      )
                      : newParamSegment.destination
                  );

                  const newKnownEndPoints = {origNodeKey: interNodeKey, destNodeKey: knownEndPoints.destNodeKey};
                  myTools.processSegment(roadGraphSnapShot, roadGraphData, paramSegment, newKnownEndPoints);
                }
                
                //We no longer want to process that intersected segment
                return;
              }
            }
          }
        }
      }
    }
    
    //If we reached that point, no intersection was found, we need to insert nodes and road
    if (!knownEndPoints.origNodeKey) {
      knownEndPoints.origNodeKey = myTools.createNode(roadGraphSnapShot, roadGraphData, newParamSegment.origin);
    }
    if (!knownEndPoints.destNodeKey) {
      knownEndPoints.destNodeKey = myTools.createNode(roadGraphSnapShot, roadGraphData, newParamSegment.destination);
    }
    //Doesn't really matter if it already exist, it will be replaced with same values
    myTools.createRoad(roadGraphSnapShot, roadGraphData, knownEndPoints.origNodeKey, knownEndPoints.destNodeKey);
	}
};

myTools.vector.prototype = {
  x: 0,
  y: 0,
  init: function(x, y) {
    this.x = x;
    this.y = y;
  },
  add: function(other) {
    return new myTools.vector(this.x + other.x, this.y + other.y);
  },
  sub: function(other) {
    return new myTools.vector(this.x - other.x, this.y - other.y);
  },
  crossProduct: function(other) {
    return this.x * other.y - this.y * other.x;
  },
  dotProduct: function(other) {
    return this.x * other.x + this.y * other.y;
  },
  mult: function(scalar) {
    return new myTools.vector(this.x * scalar, this.y * scalar);
  },
  divide: function(scalar) {
    return this.mult(1/scalar);
  },
  length: function() {
    return Math.sqrt(this.x * this.x + this.y * this.y);
  },
  normalize: function() {
    const len = this.length();
    if (len > 0) {
      return this.divide(len);
    }
    else {
      return new myTools.vector(0, 0);
    }
  }
};


// Listens for new segments added to /segments/{segmentUID} and /paths/intersections and /path/roads accordingly
exports.newSegment = functions.database.ref('/segments/{segmentUID}')
    .onCreate(event => {
		// Grab the current value of what was written to the Realtime Database.
		const segment = event.data.val();
    
		//Make sure speed makes sens
		if (segment.speed >= MIN_WALK_SPEED && segment.speed < MAX_VEHICULE_SPEED) {
      const newParamSegment = myTools.getParametrizedSegmentFromSegment(segment);
		
			// You must return a Promise when performing asynchronous tasks inside a Functions such as
			// writing to the Firebase Realtime Database.
			// Setting an "uppercase" sibling in the Realtime Database returns a Promise.
      const roadGraphRef = admin.database().ref('/roadgraph');
      
			return roadGraphRef.once('value', function (roadGraphSnapShot) {
        var roadGraphData = roadGraphSnapShot.val();
        if (!roadGraphData) {
          roadGraphData = {};
        }
				myTools.processSegment(roadGraphSnapShot, roadGraphData, newParamSegment, {origNodeKey: "", destNodeKey: ""});
			});
		}
		else {
			return null;
		}
});

exports.batchProcessAllSegments = functions.https.onRequest((req, res) => {
  const startWith = req.query.startwith;
  
  console.log('startWith: ' + startWith);
  
  const startTime = (new Date()).getTime();

  //Fetch all segments
	const promSeg = admin.database().ref('/segments').once('value', function(segmentSnapshot) {});
  
  const promGraphData = admin.database().ref('/roadgraph').once('value', function (roadGraphSnapShot) {});

  const promises = [promSeg, promGraphData];
  return Promise.all(promises).then(function (ret) {
    console.log("ret.length: " + ret.length);

    const segmentSnapshot = ret[0];
    const roadGraphSnapShot = ret[1];

		var segments = segmentSnapshot.val();
    
    if (!segments) {
      segments = {};
    }
    console.log('segments.length: ' + segments.length);

    var roadGraphData = roadGraphSnapShot.val();
    if (!roadGraphData) {
      roadGraphData = {};
    }
    
    const result = {
      processed: 0,
      invalidSpeed: 0,
      skipped: 0,
      nextStartWith: "none"
    };
    
    if (segments && roadGraphData) {
      var started = !startWith;
      for (var segmentKey in segments) {
        //Check elapsed time:
        const curTime = (new Date()).getTime();
        const ellapsed = curTime - startTime;
        
        //Stop after 55 seconds to avoid timeout
        if (ellapsed >= 55000) {
          console.log('About to timeout, batch halted at key: ' + segmentKey);
          result.nextStartWith = segmentKey;
          console.log("Processed: " + result.processed + ", Invalid speed: " + result.invalidSpeed + ", Skipped: " + result.skipped);
          res.send(result);
          return;
        }
        
        if (started || segmentKey == startWith) {
          started = true;
          
          console.log('batch processing segment: ' + segmentKey);
  
          const segment = segments[segmentKey];
      		//Make sure speed makes sens
      		if (segment.speed >= MIN_WALK_SPEED && segment.speed < MAX_VEHICULE_SPEED) {
            const newParamSegment = myTools.getParametrizedSegmentFromSegment(segment);
        
      		  myTools.processSegment(roadGraphSnapShot, roadGraphData, newParamSegment, {origNodeKey: "", destNodeKey: ""});
            result.processed++;
          }
          else {
            result.invalidSpeed++;
          }
        }
        else {
            result.skipped++;
        }
      }
    }
    else {
      if (!segments) console.log("segments is undefined: (" + (typeof segments) + ") " + segments);
      if (!roadGraphData) console.log("roadGraphData is undefined: (" + (typeof roadGraphData) + ") " + roadGraphData);
    }
    
    console.log("Processed: " + result.processed + ", Invalid speed: " + result.invalidSpeed + ", Skipped: " + result.skipped);
    res.send(result);
  });
});