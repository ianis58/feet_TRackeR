package ca.uqac.mobile.feet_tracker.android.services.locationtracker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.LinkedList;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.trainer.RecordActivity;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;
import ca.uqac.mobile.feet_tracker.tools.MTM7Converter;

public class LocationTrackerService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;

    private GoogleApiClient mGoogleApiClient;
    private android.location.Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private String newTrackUid;
    private int locationsCount;

    private final LinkedList<LocationTrackerListener> mListeners = new LinkedList<>();

    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;


    private boolean isGeodesicLocationValid = false;
    private final GeodesicLocation lastKnownGeodesicLocation = new GeodesicLocation();
    private boolean isMetricLocationValid = false;
    private final MetricLocation lastKnownMetricLocation = new MetricLocation();

    // This is the object that receives interactions from clients.
    private final IBinder mBinder = new LocationTrackerBinder(this);

    private int UPDATE_INTERVAL = 1000 * 5; // /< location update interval
    private int FASTEST_INTERVAL = 1000 * 5; // /< fastest location update interval

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
        onLocationChanged(mLocation);
    }

    private void updateGeodesicLocation(int locationsCount, double latitude, double longitude, double altitude) {
        //Update geodesic location
        //TODO: Remove
        lastKnownGeodesicLocation.setLocationId(locationsCount);
        lastKnownGeodesicLocation.setLatitude(latitude);
        lastKnownGeodesicLocation.setLongitude(longitude);
        lastKnownGeodesicLocation.setAltitude(altitude);
        //It's now valid
        isGeodesicLocationValid = true;

        //Metric location is no longer valid
        isMetricLocationValid = false;
    }

    private void updateMetricLocation() {
        //Only update if geodesic is valid since we'll compute metric from geodesic
        if (isGeodesicLocationValid) {
            MTM7Converter.geodesicToMetric(lastKnownGeodesicLocation, lastKnownMetricLocation);
            //Metric location is now valid
            isMetricLocationValid = true;
        }
    }

    public GeodesicLocation getGeodesicLocation() {
        return isGeodesicLocationValid ? lastKnownGeodesicLocation : null;
    }

    public MetricLocation getMetricLocation() {
        updateMetricLocation();
        return isMetricLocationValid ? lastKnownMetricLocation : null;
    }

    public void registerListener(LocationTrackerListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void unregisterListener(LocationTrackerListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLocation == null){
            startLocationUpdates();
        }

        if (mLocation != null) {
            double latitude = mLocation.getLatitude();
            double longitude = mLocation.getLongitude();
            double altitude = mLocation.getAltitude();

            updateGeodesicLocation(locationsCount, latitude, longitude, altitude);
            GeodesicLocation loc = lastKnownGeodesicLocation;

            //TODO: Déplacer tout ça dans TrainerActivity
            if (newTrackUid != null && !"".equals(newTrackUid)) {
                myRef.child(firebaseUser.getUid()).child(newTrackUid).child("locations").push().setValue(loc);
            }

            //Broadcast location changed event
            for (LocationTrackerListener listener : mListeners) {
                listener.onLocationChanged(loc);
            }

            locationsCount++;

        } else {
            Toast.makeText(this, R.string.error_accessing_location, Toast.LENGTH_SHORT).show();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        //Log.d("reque", "--->>>>");
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
        return mBinder;
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("tracks");

        locationsCount = 0;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
        //Toast.makeText(getApplicationContext(), "Starting sampling service...3", Toast.LENGTH_LONG).show();
        showNotification();

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    firebaseUser = user;
                }
                else {
                    stopSelf();
                }
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_LONG).show();
        mGoogleApiClient.connect();

        if(intent != null){
            newTrackUid = intent.getStringExtra("newTrackUid");
            UPDATE_INTERVAL = intent.getIntExtra("samplingInterval", 5) * 1000;
            FASTEST_INTERVAL = UPDATE_INTERVAL;
        }
        else {
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        Intent notifyIntent = new Intent(this, RecordActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_sample)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getString(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

}
