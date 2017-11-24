package ca.uqac.mobile.feet_tracker.android.services.trackrecorder;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.android.services.location.LocationBinder;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationListener;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationService;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;

/**
 * Created by MeTaL125 on 2017-11-21.
 *
 */

public class TrackRecorderService extends Service {
    private static final float DEFAULT_INTERVAL_SECS = 10.0f;
    private final TrackRecorderBinder mBinder = new TrackRecorderBinder(this);

    FirebaseDatabase database;
    DatabaseReference tracksRef;
    DatabaseReference curTrackRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    private String trackUID;
    private float trackingIntervalSecs = DEFAULT_INTERVAL_SECS;

    private LocationListener locationListener;
    private LocationService locationService;
    private ServiceConnection locationServiceConnection;

    private void recordTrackLocation(GeodesicLocation geodesicLocation) {
        if (curTrackRef != null) {
            curTrackRef.child("locations").push().setValue(geodesicLocation);
        }
    }

    private void bindToLocationService() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(GeodesicLocation geodesicLocation, MetricLocation metricLocation, long millisElapsed) {
                recordTrackLocation(geodesicLocation);
            }
        };

        Intent locationServiceIntent = new Intent(this, LocationService.class);

        locationServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                locationService = null;
                if (service instanceof LocationBinder) {
                    final LocationBinder locationBinder = (LocationBinder) service;
                    locationService = locationBinder.getService();

                    locationService.registerListener(locationListener, trackingIntervalSecs);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                //Reconnect to service
                bindToLocationService();
            }
        };

        bindService(locationServiceIntent, locationServiceConnection, BIND_AUTO_CREATE);
    }

    private void unbindFromLocationService() {
        if (locationService != null) {
            locationService.unregisterListener(locationListener);
            locationService = null;
        }
        if (locationServiceConnection != null) {
            unbindService(locationServiceConnection);
            locationServiceConnection = null;
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
            authStateListener = new FirebaseAuth.AuthStateListener() {
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

        bindToLocationService();
    }

    public void stopTracking() {
        unbindFromLocationService();

        //Make sure we're not gonna track anything new
        curTrackRef = null;
    }


}
