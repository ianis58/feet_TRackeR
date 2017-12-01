package ca.uqac.mobile.feet_tracker.android.services.trackrecorder;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.android.services.LocationBasedService;
import ca.uqac.mobile.feet_tracker.android.services.segmentlogger.SegmentLoggerService;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;
import ca.uqac.mobile.feet_tracker.model.geo.Segment;

/**
 * Created by MeTaL125 on 2017-11-21.
 *
 */

public class TrackRecorderService extends LocationBasedService {
    private static final boolean SHOW_TOASTS = false;
    private static final boolean LOG_TOO_SLOW = false;
    private static final boolean LOG_TOO_FAST = false;

    private static final double MIN_INVALID_SPEED = 500.0;

    private static final String TAG = TrackRecorderService.class.getSimpleName();
    private static final float DEFAULT_INTERVAL_SECS = 10.0f;

    private final TrackRecorderBinder mBinder = new TrackRecorderBinder(this);

    private boolean isValidLastLocation = false;
    private final MetricLocation lastLocation = new MetricLocation();
    private long lastTimestamp;

    private boolean isValidLastSpeed = false;
    private double lastSpeed;

    FirebaseDatabase database;
    DatabaseReference tracksRef;
    DatabaseReference curTrackRef;
    FirebaseUser firebaseUser;

    private Segment segment = new Segment();

    private String trackUID;
    private float trackingIntervalSecs = DEFAULT_INTERVAL_SECS;

    private void recordTrackLocation(GeodesicLocation geodesicLocation) {
        if (curTrackRef != null) {
            curTrackRef.child("locations").push().setValue(geodesicLocation);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //Get database instances and references
        database = FirebaseDatabase.getInstance();
        tracksRef = database.getReference("tracks");

        //Get firebase user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            //User could not be fetched right away, try an asynchronous method
            final FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        firebaseUser = user;
                    }
                }
            };
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
        }
    }


    private void handleNewMetricLocation(MetricLocation newLocation, long millisElapsed) {
        final long now = System.currentTimeMillis();
        if (isValidLastLocation) {
            //Compute location delta from last location
            final double deltaNorth = newLocation.getNorth() - lastLocation.getNorth();
            final double deltaEast = newLocation.getEast() - lastLocation.getEast();
            final double deltaAlt = 0;//newLocation.getAltitude() - lastLocation.getAltitude();
            final double deltaLocation = Math.sqrt(deltaNorth*deltaNorth + deltaEast*deltaEast + deltaAlt*deltaAlt);

            //Compute time delta from last timestamp
            final double deltaTime = (double)(now - lastTimestamp);

            if (deltaTime > 0) {
                //Compute speed in km/h (NOTE: deltaLocation is in meters and deltaTime is in millis)
                final double speed = (deltaLocation / 1000) / (deltaTime / 1000 / 60 / 60);

                //Should we log segment?
                if (speed > MIN_INVALID_SPEED) {
                    Log.d(TAG, String.format("Invalid speed detected: %.2f km/h", speed));
                    return; //We don't want to backup location
                }
                if (speed < Segment.MIN_WALK_SPEED && !LOG_TOO_SLOW) {
                    if (SHOW_TOASTS) {
                        Toast.makeText(this, String.format("TrackRecorderService found a %.2f meters segment at %.2f km/h (too low)", deltaLocation, speed), Toast.LENGTH_LONG).show();
                    }
                    Log.d(TAG, String.format("Speed too low: %.2f km/h (minimum: %.2f km/h)", speed, Segment.MIN_WALK_SPEED));
                }
                else if (speed > Segment.MAX_VEHICULE_SPEED && !LOG_TOO_FAST) {
                    if (SHOW_TOASTS) {
                        Toast.makeText(this, String.format("TrackRecorderService found a %.2f meters segment at %.2f km/h (too fast)", deltaLocation, speed), Toast.LENGTH_LONG).show();
                    }
                    Log.d(TAG, String.format("Speed too fast: %.2f km/h (maximum: %.2f km/h)", speed, Segment.MAX_VEHICULE_SPEED));
                }
                else {
                    if (SHOW_TOASTS) {
                        Toast.makeText(this, String.format("TrackRecorderService found a %.2f meters segment at %.2f km/h (acceptable)", deltaLocation, speed), Toast.LENGTH_LONG).show();
                    }
                    //Acceptable speed, update segment
                    segment.setOrigin(lastLocation);
                    segment.setDestination(newLocation);
                    segment.setSpeed(speed);
                    if (firebaseUser != null) {
                        segment.setUserID(firebaseUser.getUid());
                    } else {
                        segment.setUserID("");
                    }

                    //And add it to database
                    curTrackRef.child("segments").push().setValue(segment);
                }
            }
            else {
                //Abnormal situation, let's log it
                Log.e(TAG, String.format("Abnormal time delta '%d' resulting from 'now=%d - lastTimestampt=%d", deltaTime, now, lastTimestamp));
            }
        }
        else {
            //It's our first location, we can't do much with just a single location
        }

        backupLocation(newLocation, now);
    }

    private void backupLocation(MetricLocation newLocation, long now) {
        lastLocation.setNorth(newLocation.getNorth());
        lastLocation.setEast(newLocation.getEast());
        lastLocation.setAltitude(newLocation.getAltitude());
        lastTimestamp = now;
        isValidLastLocation = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        trackUID = null;
        return mBinder;
    }

    @Override
    public void onDestroy() {
        unbindFromLocationService();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(GeodesicLocation geodesicLocation, MetricLocation metricLocation, long millisElapsed) {
        //recordTrackLocation(geodesicLocation);
        handleNewMetricLocation(metricLocation, millisElapsed);
    }

    //API
    public void startTracking(FirebaseUser user, String trackUID, float trackingIntervalSecs) {
        if (user != null && firebaseUser == null) {
            this.firebaseUser = user;
        }
        this.trackUID = trackUID;
        if (this.firebaseUser != null && this.trackUID != null) {
            this.curTrackRef = this.tracksRef.child(this.firebaseUser.getUid()).child(this.trackUID);
        }
        this.trackingIntervalSecs = trackingIntervalSecs;

        bindToLocationService(trackingIntervalSecs);
    }

    public void stopTracking() {
        unbindFromLocationService();

        //Make sure we're not gonna track anything new
        curTrackRef = null;
    }


}
