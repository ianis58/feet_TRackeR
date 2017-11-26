package ca.uqac.mobile.feet_tracker.android.services.location;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
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

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private class RegistredListener {
        public long now() {
            return SystemClock.elapsedRealtime();
        }

        public LocationListener listener;
        public long interval;
        public long lastTimestamp;

        public RegistredListener(LocationListener listener, long interval) {
            this.listener = listener;
            this.interval = interval;
            keepTimestamp();
        }

        public void keepTimestamp(long now) {
            this.lastTimestamp = now;
        }
        public void keepTimestamp() {
            keepTimestamp(now());
        }


        public long elapsed(long now) {
            return now - lastTimestamp;
        }
        public long elapsed() {
            return elapsed(now());
        }

        public boolean isReady(long now) {
            return elapsed(now) >= interval;
        }
        public boolean isReady() {
            return isReady(now());
        }

        private void doCallback(long now) {
            keepTimestamp(now);
            if (listener != null) {
                listener.onLocationChanged(getGeodesicLocation(), getMetricLocation(), elapsed(now));
            }
        }
        private void doCallback() {
            doCallback(now());
        }
    }

    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;

    private GoogleApiClient mGoogleApiClient;
    private android.location.Location mLocation;
    //private LocationManager locationManager;
    private LocationRequest mLocationRequest;

    private final LinkedList<RegistredListener> mListeners = new LinkedList<>();

    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;


    private boolean isLocationValid = false;
    private final GeodesicLocation lastKnownGeodesicLocation = new GeodesicLocation();
    private final MetricLocation lastKnownMetricLocation = new MetricLocation();

    // This is the object that receives interactions from clients.
    private final IBinder mBinder = new LocationBinder(this);

    private int UPDATE_INTERVAL = 1000 * 5; // /< location update interval
    private int FASTEST_INTERVAL = 1000 * 5; // /< fastest location update interval

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
        onLocationChanged(mLocation);
    }

    private void updateLocationsFromGeodesic(double latitude, double longitude, double altitude) {
        //Update geodesic location
        lastKnownGeodesicLocation.setLatitude(latitude);
        lastKnownGeodesicLocation.setLongitude(longitude);
        lastKnownGeodesicLocation.setAltitude(altitude);

        updateMetricLocation();

        //It's now valid
        isLocationValid = true;

    }

    private void updateMetricLocation() {
        MTM7Converter.geodesicToMetric(lastKnownGeodesicLocation, lastKnownMetricLocation);
    }

    public GeodesicLocation getGeodesicLocation() {
        return isLocationValid ? lastKnownGeodesicLocation : null;
    }

    public MetricLocation getMetricLocation() {
        updateMetricLocation();
        return isLocationValid ? lastKnownMetricLocation : null;
    }

    /**
     * Register a Location callback, new location will be sent each intervalSeconds (minimum, it could be a little longer)
     * @param listener Callback instance
     * @param intervalSeconds Minimum callback interval
     */
    public void registerListener(LocationListener listener, float intervalSeconds, boolean callbackNow) {
        long longInterval = (long) (intervalSeconds * 1000.0f);
        //Search for that listener
        for (RegistredListener registredListener : mListeners) {
            if (registredListener.listener == listener) {
                //Found, update it
                registredListener.interval = longInterval;

                if (callbackNow) {
                    registredListener.doCallback();
                }
                return;
            }
        }
        //Not found, add it
        RegistredListener registredListener = new RegistredListener(listener, longInterval);
        mListeners.add(registredListener);
        if (callbackNow) {
            if (isLocationValid) {
                //If location is valid, do callback right away
                registredListener.doCallback();
            }
            else {
                //Otherwise, do callback as soon as we get a location
                registredListener.lastTimestamp = registredListener.now() - registredListener.interval;
            }
        }
    }
    public void registerListener(LocationListener listener, float intervalSeconds) {
        registerListener(listener, intervalSeconds, false);
    }


    public void unregisterListener(LocationListener listener) {
        //Search for listener
        for (RegistredListener registredListener : mListeners) {
            if (registredListener.listener == listener) {
                mListeners.remove(registredListener);
                break;
            }
        }
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

            updateLocationsFromGeodesic(latitude, longitude, altitude);

            //Broadcast location changed event
            for (RegistredListener registredListener : mListeners) {
                //Keep a copy of the actuel timestamp so we always use the same value for that registred listener
                long now = registredListener.now();

                if (registredListener.isReady(now)) {
                    registredListener.doCallback(now);
                }
            }

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

        // Check for ACCESS_FINE_LOCATION permission
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

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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

    /*@Override
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
    }*/

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
