package ca.uqac.mobile.feet_tracker.android.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import ca.uqac.mobile.feet_tracker.android.services.location.LocationBinder;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationListener;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationService;

/**
 * Created by MeTaL125 on 2017-11-25.
 */

public abstract class LocationBasedService extends Service implements LocationListener {

    private LocationService locationService;
    private ServiceConnection locationServiceConnection;

    protected void bindToLocationService(float intervalSeconds, boolean callbackNow) {
        Intent locationServiceIntent = new Intent(this, LocationService.class);

        final float locationRefreshIntervalSeconds = intervalSeconds;
        final boolean locationCallbackNow = callbackNow;

        if (locationService != null) {
            locationService.registerListener(
                    this,
                    locationRefreshIntervalSeconds,
                    locationCallbackNow
            );
        }
        else {
            locationServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    locationService = null;
                    if (service instanceof LocationBinder) {
                        final LocationBinder locationBinder = (LocationBinder) service;
                        locationService = locationBinder.getService();

                        locationService.registerListener(
                                LocationBasedService.this,
                                locationRefreshIntervalSeconds,
                                locationCallbackNow
                        );
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    //Reconnect to service
                    locationService = null;
                    bindToLocationService(locationRefreshIntervalSeconds, locationCallbackNow);
                }
            };

            bindService(locationServiceIntent, locationServiceConnection, BIND_AUTO_CREATE);
        }
    }
    protected void bindToLocationService(float intervalSeconds) {
        bindToLocationService(intervalSeconds, false);
    }

    protected void unbindFromLocationService() {
        if (locationService != null) {
            locationService.unregisterListener(this);
            locationService = null;
        }
        if (locationServiceConnection != null) {
            unbindService(locationServiceConnection);
            locationServiceConnection = null;
        }
    }

}
