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
	"altitude" : double
	"east" : double
	"north" : double
  "roads" : {
    "roadUID" : {
      "connectedUID" : intersectionUID,
      "distance" : double
    }, ...
  }
}
*/

const myTools = {
	processSegment: function(segment, roadGraphRef) {
		//As a testing purpose, simply insert 2 new intersections and a new roads
		const intersection1 = {
			altitude: segment.origin.altitude,
			east: segment.origin.east,
			north: segment.origin.north
		};
		const intersection2 = {
			altitude: segment.destination.altitude,
			east: segment.destination.east,
			north: segment.destination.north
		};

    //Create graph nodes
		const nodeRef1 = roadGraphRef.push(intersection1);
		const nodeRef2 = roadGraphRef.push(intersection2);
    
    //Compute road distance
    const deltaeast = intersection2.east - intersection1.east;
    const deltanorth = intersection2.north - intersection1.north;
    const distance = Math.sqrt(deltaeast*deltaeast + deltanorth+deltanorth);
    
    //Append cross reference roads to each node
    nodeRef1.child('roads').push().set({
      connectedUID: nodeRef2.key,
      distance: distance
    });
    nodeRef2.child('roads').push().set({
      connectedUID: nodeRef1.key,
      distance: distance
    });
	}
};


// Listens for new segments added to /segments/{segmentUID} and /paths/intersections and /path/roads accordingly
exports.newSegment = functions.database.ref('/segments/{segmentUID}')
    .onWrite(event => {
		// Grab the current value of what was written to the Realtime Database.
		const segment = event.data.val();
    
		//Make sure speed makes sens
		if (segment.speed >= MIN_WALK_SPEED && segment.speed < MAX_VEHICULE_SPEED) {
		
			// You must return a Promise when performing asynchronous tasks inside a Functions such as
			// writing to the Firebase Realtime Database.
			// Setting an "uppercase" sibling in the Realtime Database returns a Promise.
      const roadGraphRef = admin.database().ref('/roadgraph');
      
			return roadGraphRef.transaction(temp => {
				myTools.processSegment(segment, roadGraphRef);
			});
		}
		else {
			return null;
		}
    });