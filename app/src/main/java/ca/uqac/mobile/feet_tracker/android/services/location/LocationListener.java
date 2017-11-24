package ca.uqac.mobile.feet_tracker.android.services.location;

import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;

/**
 * Created by MeTaL125 on 2017-11-13.
 */

public interface LocationListener {
    void onLocationChanged(GeodesicLocation geodesicLocation, MetricLocation metricLocation, long millisElapsed);
}
