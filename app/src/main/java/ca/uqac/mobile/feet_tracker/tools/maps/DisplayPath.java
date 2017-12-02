package ca.uqac.mobile.feet_tracker.tools.maps;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;
import ca.uqac.mobile.feet_tracker.tools.MTM7Converter;

/**
 * Created by MeTaL125 on 2017-12-01.
 */

public class DisplayPath {
    private final static String TAG = DisplayPath.class.getSimpleName();
    private final static GeodesicLocation GEODESIC = new GeodesicLocation();
    private final static MetricLocation METRIC = new MetricLocation();

    public class Segment {

        private final int color;
        private final List<LatLng> points = new ArrayList<>();

        public Segment(int color) {
            this.color = color;
        }

        public void addPoint(double east, double north) {
            synchronized (METRIC) {
                METRIC.setEast(east);
                METRIC.setNorth(north);
                synchronized (GEODESIC) {
                    MTM7Converter.metricToGeodesic(METRIC, GEODESIC);
                    points.add(new LatLng(GEODESIC.getLatitude(), GEODESIC.getLongitude()));
                }
            }
        }

        public PolylineOptions getPolyline() {
            if (points.size() >= 2) {
                PolylineOptions polyline = new PolylineOptions();
                polyline
                        .clickable(false)
                        .geodesic(false)
                        .color(color)
                ;

                for (LatLng point : points) {
                    polyline.add(point);
                }

                return polyline;
            }
            return null;
        }

        public void includeInBounds(LatLngBounds.Builder boundBuilder) {
            for (LatLng point : points) {
                boundBuilder.include(point);
            }
        }

    }

    public static final int TYPE_MISSING = 0;
    public static final int TYPE_APPROX = 1;
    public static final int TYPE_REAL = 2;

    private double distance;
    private final List<Segment> segments = new ArrayList<>();

    public DisplayPath(JSONObject jsonPath) {
        constructFromJSON(jsonPath);
    }

    public void constructFromJSON(JSONObject jsonRoot) {
        segments.clear();

        /*NOTE: jsonRoot's structure:
        {
          start: {
            east: double,
            north: double
          },
          paths: [
            {
              east: double,
              north: double,
              distance: double,
              type: (0=TYPE_MISSING, 1=TYPE_APPROX, 2=TYPE_REAL)
            }, ...
          ],
          distance: double (total distance)
        }
        */

        try {
            distance = jsonRoot.getDouble("distance");

            final JSONObject jsonStart = jsonRoot.getJSONObject("start");

            double lastEast = jsonStart.getDouble("east");
            double lastNorth = jsonStart.getDouble("north");
            int lastType = -1;
            Segment segment = null;

            final JSONArray jsonPaths = jsonRoot.getJSONArray("paths");

            final int pathCount = jsonPaths.length();
            for (int pathIndex = 0; pathIndex < pathCount; ++pathIndex) {
                final JSONObject jsonPath = jsonPaths.getJSONObject(pathIndex);

                final double east = jsonPath.getDouble("east");
                final double north = jsonPath.getDouble("north");
                final int type = jsonPath.getInt("type");

                //Must we change segment?
                if (segment == null || type != lastType) {
                    final int color;
                    if (type == TYPE_REAL) {
                        color = Color.GREEN;
                    }
                    else if (type == TYPE_APPROX) {
                        color = 0xffffa500; //Orange
                    }
                    else {
                        color = Color.RED; //TYPE_MISSING or invalid type
                    }
                    //Create new segment
                    segment = new Segment(color);
                    segments.add(segment);

                    //Add last point to it
                    segment.addPoint(lastEast, lastNorth);
                }

                segment.addPoint(east, north);

                //Backup east, north and type in case must change type and must change segment
                lastEast = east;
                lastNorth = north;
                lastType = type;
            }
            //Done!
        }
        catch (JSONException e) {
            Log.e(TAG, "constructFromJSON: Invalid JSON", e);
        }
    }

    public double getDistance() {
        return distance;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void includeInBounds(LatLngBounds.Builder boundBuilder) {
        for (Segment segment : segments) {
            segment.includeInBounds(boundBuilder);
        }
    }
}
