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

const MIN_WALK_SPEED = 2.5;
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
    "{connectedIntersectionUID}" : {
      "dirX" : double,
      "dirY" : double
      "distance": double
    }, ...
  }
}
*/

const myTools = {
  TYPE_MISSING: 0,
  TYPE_APPROX: 1,
  TYPE_REAL: 2,
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
  distanceTolerance: 5.0, //meters
  missingTolerance: 50.0, //meters
  findIntersection: function(seg1, seg2) {
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
      north: coordinate.y,
      coord: coordinate
    };
    
    //Insert new node (insert only east and north) and fetch's it's key
    const key = roadGraphSnapShot.ref.push({
      east: node.east, 
      north: node.north
    }).key;

    console.log("Created node: " + key);

    //Update local model
    roadGraphData[key] = node;
    
    return key;
  },
  createRoad: function(roadGraphSnapShot, roadGraphData, fromKey, toKey) {
    const fromNode = roadGraphData[fromKey];
    const toNode = roadGraphData[toKey];
    const fromPos = fromNode.coord;
    const toPos = toNode.coord;
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

    console.log("Created road between " + fromKey + " and " + toKey);
  },
  
	processSegment: function(roadGraphSnapShot, roadGraphData, newParamSegment, knownEndPoints) {
    //First pass, find close enough endpoints nodes, if not already know
    const knownOrig = !!knownEndPoints.origNodeKey;
    const knownDest = !!knownEndPoints.destNodeKey;
    
    if (knownOrig && knownDest && knownEndPoints.origNodeKey == knownEndPoints.destNodeKey) {
      //Same origin and destination, we don't want to do anything
      return;
    }
    
    if (!knownOrig || !knownDest) {
      var origClosestDistance = knownOrig ? -1 : myTools.distanceTolerance * 2;
      var destClosestDistance = knownDest ? -1 : myTools.distanceTolerance * 2;
      for (var nodeKey in roadGraphData) {
        const testNode = roadGraphData[nodeKey];
        
        //Make a vector out of node's coordinates
        const nodeCoord = testNode.coord;
        
        //Compute distance to origin and destination
        const distToOrig = nodeCoord.distance(newParamSegment.origin);
        const distToDest = nodeCoord.distance(newParamSegment.destination);
        
        if (distToOrig < origClosestDistance && distToOrig < myTools.distanceTolerance) {
          //Origin is closer to that node
          knownEndPoints.origNodeKey = nodeKey;
          origClosestDistance = distToOrig;
        }
        if (distToDest < destClosestDistance && distToDest < myTools.distanceTolerance) {
          //Origin is closer to that node
          knownEndPoints.destNodeKey = nodeKey;
          destClosestDistance = distToDest;
        }
        
        //That's it, try another node
      }
      //All nodes are tested
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
                origNode.coord,
                destNode.coord
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
                  
                  const nodeCoord = interNode.coord;
                  
                  //Compute distance to intersection
                  const distToInter = nodeCoord.distance(intersection);
                  
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
                  intersection = roadGraphData[interNodeKey].coord;
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
                      roadGraphData[knownEndPoints.origNodeKey].coord
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
                      roadGraphData[knownEndPoints.destNodeKey].coord
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
    if (knownEndPoints.origNodeKey != knownEndPoints.destNodeKey) {
      //Doesn't really matter if it already exist, it will be replaced with same values
      myTools.createRoad(roadGraphSnapShot, roadGraphData, knownEndPoints.origNodeKey, knownEndPoints.destNodeKey);
    }
	},
  

  computeGraphCoords: function(graph){
    for (var nodeKey in graph) {
      const node = graph[nodeKey];
      
      //Make a vector out of node's coordinates
      node.coord = new myTools.vector(node.east, node.north);
    }
  },
  
  aStar: function(graph, fromNode, toNode) {
    //Returns an array of the path (might not lead to "toNode")
    //NOTE: graph[key].coord (vector) must be computed before calling this function
    
    const endNode = graph[toNode];
    
    //Initialize A*

    //Nodes are not accepted yet, and distance to start is unknown (infinity) and not connected to anything
    for (var nodeKey in graph) {
      const node = graph[nodeKey];
      node.accepted = false;
      node.connectedTo = "";
      node.distToStart = Infinity;
      node.distToEnd = node.coord.distance(endNode.coord);
    }
    
    //Except for start node
    const startNode = graph[fromNode];
    startNode.accepted = true; //Start node is in accepted set
    startNode.distToStart = 0; //Obviously, distance from start to start is 0
    if (startNode.roads) {
      //For all start's connected nodes, we know their initial best distance to start
      for (var destKey in startNode.roads) {
        if (destKey != fromNode) {
          const destNode = graph[destKey];
          destNode.distToStart = startNode.roads[destKey].distance;
          destNode.connectedTo = fromNode;
        }
      }
    }
    
    //Iterate until we select "toNode", or we can't select a non Infinity node
    var lastSelectedNodeKey = fromNode;
    while (true) {
      //Refresh distanceFromStartToEnd for each node
      for (var nodeKey in graph) {
        const node = graph[nodeKey];
        node.distFromStartToEnd = node.distToStart + node.distToEnd;
      }

      //Select unaccepted node with smallest distFromStartToEnd
      var selectedNodeKey = "";
      var selectedNode = null;
      for (var nodeKey in graph) {
        var node = graph[nodeKey];
        if (!node.accepted) {
          if (!selectedNode || node.distFromStartToEnd < selectedNode.distFromStartToEnd) {
            selectedNodeKey = nodeKey;
            selectedNode = node;
          }
        }
      }
      
      //No node selectd?
      if (!selectedNode) {
        break;
      }
      else if (selectedNode.distFromStartToEnd == Infinity) {
        //Closest node is not connected to anything else?
        break;
      }
      lastSelectedNodeKey = selectedNodeKey;

      if (selectedNodeKey == toNode) {
        //We reached toNode
        break;
      }
      else {
        //We accept selectedNodeKey
        selectedNode.accepted = true;

        //Iterate all nodes connected to selectedNode
        if (selectedNode.roads) {
          for (var connectedKey in selectedNode.roads) {
            if (connectedKey != selectedNodeKey) {
              const connectedNode = graph[connectedKey];
              const road = selectedNode.roads[connectedKey];
  
              const newDistToStart = selectedNode.distToStart + road.distance;
  
              if (newDistToStart >= connectedNode.distToStart) {
                //We do nothing
              }
              else {
                //Replace connected distToStart and reject connected node
                connectedNode.connectedTo = selectedNodeKey;
                connectedNode.distToStart = newDistToStart;
                connectedNode.accepted = false;
              }
            }
          }
        }
      }

    }//End of iteration
    
    var returnNodes = [];
    
    var pathNode = lastSelectedNodeKey;
    var i = 0;
    while (pathNode) {
      ++i;
      //Since we start by the end, we must insert at the begining (unshift and not push)
      returnNodes.unshift(pathNode);
      pathNode = graph[pathNode].connectedTo;
    }
    
    return returnNodes;
  },
  
  typeFromDistance: function(distance) {
    if (distance > myTools.missingTolerance) {
      return myTools.TYPE_MISSING;
    }
    else if (distance > myTools.distanceTolerance) {
      return myTools.TYPE_APPROX;
    }
    else {
      return myTools.TYPE_REAL;
    }
  },
  
  enableNodesConnectedToKey: function(graph, key) {
      //Untouch every node
      for (var nodeKey in graph) {
        const node = graph[nodeKey];
        node.touched = 0;
      }
      //But the starting key
      graph[key].touched = 1;
      
      //Touch every connected node until we are unable to touch another one
      var touched = true;
      while (touched) {
        touched = false;
        for (var nodeKey in graph) {
          const node = graph[nodeKey];
          
          if (node.touched == 1) {
            node.touched = 2;
            if (node.roads) {
              for (var connectedNodeKey in node.roads) {
                const connectedNode = graph[connectedNodeKey];
                
                if (connectedNode.touched == 0) {
                  connectedNode.touched = 1;
                  touched = true;
                }
              }
            }
          }
        }
      }

      //Disable untouched nodes
      for (var nodeKey in graph) {
        const node = graph[nodeKey];
        node.enabled = (node.touched != 0);
      }
  },
  
  findOptimalPath: function(graph, fromCoord, toCoord) {

    var path = {
      start: {
        east: fromCoord.x,
        north: fromCoord.y
      },
      paths: [
        /*
        {
          east: double,
          north: double,
          distance: double,
          type: (0=TYPE_MISSING, 1=TYPE_APPROX, 2=TYPE_REAL)
        }, ...
        */
      ],
      distance: 0
    };

    //Local function to add coordinate to returned path    
    var lastCoord = fromCoord;
    const addCoord = function(coord, type) {
      //New segment's distance
      const dist = coord.distance(lastCoord);
      lastCoord = coord;
      
      //Add new coordinate
      path.paths.push({
        east: coord.x,
        north: coord.y,
        distance: dist,
        type: type
      });
      
      //Inc total distance
      path.distance += dist;
    };


    //Precompute all graph's coordinates
    myTools.computeGraphCoords(graph);

    //Find closest fromNodeKey and toNode
    var closestFromDistance = 0;
    var fromNodeKey = "";
    var closestToDistance = 0;
    var toNodeKey = "";
    
    for (var nodeKey in graph) {
      const node = graph[nodeKey];
    
      //Compute distance to fromCoord and toCoord
      const fromDistance = node.coord.distance(fromCoord);
      const toDistance = node.coord.distance(toCoord);

      if (!fromNodeKey || fromDistance < closestFromDistance) {
        closestFromDistance = fromDistance;
        fromNodeKey = nodeKey;
      }
      if (!toNodeKey || toDistance < closestToDistance) {
        closestToDistance = toDistance;
        toNodeKey = nodeKey;
      }
    }
    
    if (fromNodeKey && toNodeKey) {
    
      //Execute A* algorithm recursivly until we find a path
      var currentNodeKey = fromNodeKey;
      while (currentNodeKey != toNodeKey) {

        //Enable only nodes connected to currentNodeKey
        myTools.enableNodesConnectedToKey(graph, currentNodeKey);
        
        //Reduce graph for AStar (include only enabled nodes)
        const aStarGraph = {};
        for (var nodeKey in graph) {
          if (graph[nodeKey].enabled) {
            aStarGraph[nodeKey] = graph[nodeKey];
          }
        }     
      
        const currentNode = graph[currentNodeKey];
        //Add currentNodeKey to path, TYPE based on distance from lastCoord
        const distance = currentNode.coord.distance(lastCoord);
        addCoord(currentNode.coord, myTools.typeFromDistance(distance));

        //Is it possible to reach 'toNodeKey' directly?'
        var aStarEndKey = toNodeKey;
        if (!aStarGraph[toNodeKey]) {
          //No, toNodeKey is not connected to currentNodeKey
        
          //Find the enabled node that is closest to our target key
          const endCoord = graph[toNodeKey].coord;
          var closest = Infinity;
          for (var nodeKey in aStarGraph) {
            var node = aStarGraph[nodeKey];
            
            //Compute distance to endCoord
            const distance = node.coord.distance(endCoord);
            if (distance < closest) {
              closest = distance;
              aStarEndKey = nodeKey;
            }
          }
          
        }
        //else we'll be able to reach toNodeKey in this pass
        
        //Execute A* to find a path (hopefuly) up to 'toNodeKey'
        var returnedNodes = myTools.aStar(aStarGraph, currentNodeKey, aStarEndKey);

        currentNodeKey = returnedNodes.length > 0 ? returnedNodes[returnedNodes.length-1] : "";
  
        //Add found (REAL) coordinates (except the first one, already added)
        for (var index in returnedNodes) {
          if (index > 0) {
            pathNode = returnedNodes[index];
            const node = graph[pathNode];
            
            addCoord(node.coord, myTools.TYPE_REAL);
          }
        }
        
        if (currentNodeKey != toNodeKey) {
          //We're still not at destination
        
          //Considering only disabled nodes closer to end then "currentNode",
          //find the one closer to "currentNodeKey"
          
          const toNodeCoord = graph[toNodeKey].coord;
          
          const currentNode = graph[currentNodeKey];
          currentNode.distToEnd = currentNode.coord.distance(toNodeCoord);

          var closestDistToCurrent = Infinity;
          for(var nodeKey in graph) {
            const node = graph[nodeKey];
            node.distToEnd = node.coord.distance(toNodeCoord);
            
            //Closer to "toNodeKey" then "currentNode" (considere toNodeKey too)
            if (nodeKey == toNodeKey || (!node.enable && node.distToEnd < currentNode.distToEnd)) {
              //Compute distance to currentNode
              const distance = node.coord.distance(currentNode.coord);
              
              if (distance < closestDistToCurrent) {
                closestDistToCurrent = distance;
                currentNodeKey = nodeKey; //NOTE: instead of using a "closestKey", we replace currentNodeKey directly
              }
            }
          }
        }
        
      } //End of loop


      //Add final toCoord (closestToDistance computed at the beginning)
      addCoord(toCoord, myTools.typeFromDistance(closestToDistance));
    }
    else {
      //Should only happen if graph is empty
      addCoord(toCoord, myTools.TYPE_MISSING);
    }
    
    return path;
  }
};

//Vector class
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
  },
  distance: function(other) {
    const deltaX = other.x - this.x;
    const deltaY = other.y - this.y;
    return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
  },
  toStr: function() {
    return " ( " + this.x + " , " + this.y + " ) ";
  }
};


// Listens for new segments added to /segments/{segmentUID} and /paths/intersections and /path/roads accordingly
/*exports.newSegment = functions.database.ref('/segments/{segmentUID}')
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
        var graph = roadGraphSnapShot.val();
        if (!graph) {
          graph = {};
        }
        myTools.computeGraphCoords(graph);
				myTools.processSegment(roadGraphSnapShot, graph, newParamSegment, {origNodeKey: "", destNodeKey: ""});
			});
		}
		else {
			return null;
		}
});*/

exports.batchProcessAllSegments = functions.https.onRequest((req, res) => {
  const startWith = req.query.startwith;
  
  const startTime = (new Date()).getTime();

  //Fetch all segments
	const promSeg = admin.database().ref('/segments').once('value', function(segmentSnapshot) {});
  
  const promGraphData = admin.database().ref('/roadgraph').once('value', function (roadGraphSnapShot) {});

  const promises = [promSeg, promGraphData];
  return Promise.all(promises).then(function (ret) {

    const segmentSnapshot = ret[0];
    const roadGraphSnapShot = ret[1];

		var segments = segmentSnapshot.val();
    
    if (!segments) {
      segments = {};
    }

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
      if (roadGraphData._____abc) {
        delete roadGraphData._____abc;
      }
    
      //Precompute all graph's coordinates
      myTools.computeGraphCoords(roadGraphData);

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

exports.findOptimalPath = functions.https.onRequest((req, res) => {
  const params = (req.method == 'GET') ? req.query : req.body;

  const fromEast = params.fromEast;
  const fromNorth = params.fromNorth;
  const toEast = params.toEast;
  const toNorth = params.toNorth;
  
  if (fromEast && fromNorth && toEast && toNorth) {
    return admin.database().ref('/roadgraph').once('value', function (roadGraphSnapShot) {
      var roadGraphData = roadGraphSnapShot.val();
      
      if (!roadGraphData) {
        roadGraphData = {};
      }
      const fromCoord = new myTools.vector(fromEast, fromNorth);
      const toCoord = new myTools.vector(toEast, toNorth);
      
      const path = myTools.findOptimalPath(roadGraphData, fromCoord, toCoord);
      
      res.send(path);
    });
  }
  else {
    //Bad request
    res.status(400).end();
  }
});

