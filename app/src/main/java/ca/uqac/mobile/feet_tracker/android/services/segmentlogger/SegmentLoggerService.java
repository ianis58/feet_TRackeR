package ca.uqac.mobile.feet_tracker.android.services.segmentlogger;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.android.services.LocationBasedService;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationListener;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationService;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;
import ca.uqac.mobile.feet_tracker.model.geo.Segment;

public class SegmentLoggerService extends LocationBasedService {
    private static final String TAG = SegmentLoggerService.class.getSimpleName();
    private static final float DEFAULT_INTERVAL_SECS = 10.0f;

    public static final double MIN_WALK_SPEED = 2.5;
    public static final double MIN_RUN_SPEED = 8;
    public static final double MIN_VEHICULE_SPEED = 20;
    public static final double MAX_VEHICULE_SPEED = 80;

    private final SegmentLoggerBinder mBinder = new SegmentLoggerBinder(this);

    private boolean isValidLastLocation = false;
    private final MetricLocation lastLocation = new MetricLocation();
    private long lastTimestamp;

    private boolean isValidLastSpeed = false;
    private double lastSpeed;

    private FirebaseDatabase database;
    private DatabaseReference segmentsRef;
    private FirebaseUser firebaseUser;

    private Segment segment = new Segment();

    public SegmentLoggerService() {
    }

    private void backupLocation(MetricLocation newLocation, long now) {
        lastLocation.setNorth(newLocation.getNorth());
        lastLocation.setEast(newLocation.getEast());
        lastLocation.setAltitude(newLocation.getAltitude());
        lastTimestamp = now;
        isValidLastLocation = true;
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
                if (speed < MIN_WALK_SPEED) {
                    //Toast.makeText(this,String.format("SegmentLoggerService found a %.2f meters segment at %.2f km/h (too low)", deltaLocation, speed), Toast.LENGTH_LONG).show();
                    Log.d(TAG, String.format("Speed too low: %.2f km/h (minimum: %.2f km/h)", speed, MIN_WALK_SPEED));
                }
                else if (speed > MAX_VEHICULE_SPEED) {
                    //Toast.makeText(this,String.format("SegmentLoggerService found a %.2f meters segment at %.2f km/h (too fast)", deltaLocation, speed), Toast.LENGTH_LONG).show();
                    Log.d(TAG, String.format("Speed too fast: %.2f km/h (maximum: %.2f km/h)", speed, MAX_VEHICULE_SPEED));
                }
                else {
                    //Toast.makeText(this, String.format("SegmentLoggerService found a %.2f meters segment at %.2f km/h (acceptable)", deltaLocation, speed), Toast.LENGTH_LONG).show();
                    //Acceptable speed, update segment
                    segment.setOrigin(lastLocation);
                    segment.setDestination(newLocation);
                    segment.setSpeed(speed);
                    if (firebaseUser != null) {
                        segment.setUid(firebaseUser.getUid());
                    } else {
                        segment.setUid("");
                    }

                    //And add it to database
                    segmentsRef.push().setValue(segment);
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

    @Override
    public void onCreate() {

        //Get database instances and references
        database = FirebaseDatabase.getInstance();
        segmentsRef = database.getReference("segments");

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

        bindToLocationService(DEFAULT_INTERVAL_SECS);

        //Toast.makeText(this,"SegmentLoggerService is Started", Toast.LENGTH_LONG).show();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this,"SegmentLoggerService is Destroyed", Toast.LENGTH_LONG).show();
        unbindFromLocationService();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(GeodesicLocation geodesicLocation, MetricLocation metricLocation, long millisElapsed) {
        handleNewMetricLocation(metricLocation, millisElapsed);
    }
}
