package ca.uqac.mobile.feet_tracker.android.services.locationtracker;

import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;

/**
 * Created by MeTaL125 on 2017-11-13.
 */

public interface LocationTrackerListener {
    void onLocationChanged(GeodesicLocation geodesicLocation);
}
