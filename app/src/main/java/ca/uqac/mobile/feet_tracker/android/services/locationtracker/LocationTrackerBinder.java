package ca.uqac.mobile.feet_tracker.android.services.locationtracker;

import android.os.Binder;

import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;

/**
 * Created by MeTaL125 on 2017-11-13.
 */

public class LocationTrackerBinder extends Binder {
    private LocationTrackerService mService;

    LocationTrackerBinder(LocationTrackerService service) {
        this.mService = service;
    }

    public LocationTrackerService getService() {
        return mService;
    }

}
