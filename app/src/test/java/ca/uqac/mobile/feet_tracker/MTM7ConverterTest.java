package ca.uqac.mobile.feet_tracker;

import org.junit.Assert;
import org.junit.Test;

import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;
import ca.uqac.mobile.feet_tracker.tools.MTM7Converter;

/**
 * Created by MeTaL125 on 2017-11-04.
 */

public class MTM7ConverterTest {
    @Test
    public void testGeodesicToMetric() {
        GeodesicLocation geodesic = new GeodesicLocation(48.5, -71.0, 150);
        MetricLocation metric = new MetricLocation();

        MTM7Converter.geodesicToMetric(geodesic, metric);

        Assert.assertEquals(5373608.526442214, metric.getNorth(), 0.01);
        Assert.assertEquals(267853.0462078159, metric.getEast(), 0.01);
        Assert.assertEquals(150, metric.getAltitude(), 0.0);
    }

    @Test
    public void testMetricToGeodesic() {
        GeodesicLocation geodesic = new GeodesicLocation();
        MetricLocation metric = new MetricLocation(5373608.526442214, 267853.0462078159, 150);

        MTM7Converter.metricToGeodesic(metric, geodesic);

        Assert.assertEquals(48.5, geodesic.getLatitude(), 0.01);
        Assert.assertEquals(-71.0, geodesic.getLongitude(), 0.01);
        Assert.assertEquals(150, geodesic.getAltitude(), 0.0);
    }
}
