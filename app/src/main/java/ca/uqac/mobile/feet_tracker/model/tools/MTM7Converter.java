package ca.uqac.mobile.feet_tracker.model.tools;

import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;

/**
 * Created by MeTaL125 on 2017-11-04.
 *
 * NOTE: This converter is based on Pierre Lewis's work (pierre@leware.net).
 * His online converter can be found at http://leware.net/geo/utmgoogle.htm
 */

public class MTM7Converter {
    private static final double NAD83_AXIS = 6378137.0;
    private static final double NAD83_INVERSED_FLATTENING = 298.257223563;
    private static final double NAD83_FLATTENING = 1.0 / NAD83_INVERSED_FLATTENING;
    private static final double NAD83_SQUARRED_ECCENTRICITY = NAD83_FLATTENING *(2- NAD83_FLATTENING);
    private static final double MTM_SCALE = 0.9999;
    private static final double MTM_FLASE_EASTING = 304800.0;

    //MTM7 bounds (as found at http://spatialreference.org/ref/epsg/nad83-mtm-zone-7/)
    //WGS84 Bounds: -72.0000, 44.9900, -69.0000, 61.9000
    private static final double MTM7_LAT_LOWER_BOUND = 44.99;
    private static final double MTM7_LAT_UPPER_BOUND = 61.9;
    private static final double MTM7_LON_LOWER_BOUND = -72.0;
    private static final double MTM7_LON_UPPER_BOUND = -69.0;
    //Projected Bounds: 186520.9794, 4984429.4038, 423079.0206, 6866010.3299
    private static final double MTM7_EAST_LOWER_BOUND = 186520.9794;
    private static final double MTM7_EAST_UPPER_BOUND = 423079.0206;
    private static final double MTM7_NORTH_LOWER_BOUND = 4984429.4038;
    private static final double MTM7_NORTH_UPPER_BOUND = 6866010.3299;

    private static final double MTM7_ORIGIN_MERIDIAN_DEG = (MTM7_LON_LOWER_BOUND + MTM7_LON_UPPER_BOUND) / 2;
    private static final double MTM7_ORIGIN_MERIDIAN_RAG = Math.toRadians(MTM7_ORIGIN_MERIDIAN_DEG);


    public static boolean geodesicToMetric(GeodesicLocation in, MetricLocation out) {
        final double latDeg = in.getLatitude();
        final double lonDeg = in.getLongitude();

        //Validate MTM7 bounds
        if (latDeg >= MTM7_LAT_LOWER_BOUND && latDeg < MTM7_LAT_UPPER_BOUND && lonDeg >= MTM7_LON_LOWER_BOUND && lonDeg < MTM7_LON_UPPER_BOUND) {
            final double lat = Math.toRadians(latDeg);
            final double lon = Math.toRadians(lonDeg);

            final double axis = NAD83_AXIS;
            final double eccent = NAD83_SQUARRED_ECCENTRICITY;
            final double scale = MTM_SCALE;
            final double falseEasting = MTM_FLASE_EASTING;

            final double lonOriginRad = MTM7_ORIGIN_MERIDIAN_RAG;

            //Iteratively compute variables required to compute East and North
            final double eccPrimeSquared = (eccent) / (1 - eccent);
            final double eccent2 = eccent * eccent;
            final double eccent3 = eccent2 * eccent;

            final double N = axis / Math.sqrt(1 - eccent * Math.sin(lat) * Math.sin(lat));
            final double T = Math.tan(lat) * Math.tan(lat);
            final double C = eccPrimeSquared * Math.cos(lat) * Math.cos(lat);
            final double A = Math.cos(lat) * (lon - lonOriginRad);
            final double M = axis * ((1 - eccent / 4 - 3 * eccent2 / 64 - 5 * eccent3 / 256) * lat - (3 * eccent / 8 + 3 * eccent2 / 32 + 45 * eccent3 / 1024) * Math.sin(2 * lat) + (15 * eccent2 / 256 + 45 * eccent3 / 1024) * Math.sin(4 * lat) - (35 * eccent3 / 3072) * Math.sin(6 * lat));

            final double T2 = T * T;
            final double C2 = C * C;
            final double A2 = A * A;
            final double A3 = A2 * A;

            //Compute East and North
            double east = (scale * N * (A + (1 - T + C) * A3 / 6 + (5 - 18 * T + T2 + 72 * C - 58 * eccPrimeSquared) * A3 * A2 / 120) + falseEasting);
            double north = (scale * (M + N * Math.tan(lat) * (A2 / 2 + (5 - T + 9 * C + 4 * C2) * A2 * A2 / 24 + (61 - 58 * T + T2 + 600 * C - 330 * eccPrimeSquared) * A3 * A3 / 720)));

            //Update MetricLocation
            out.setEast(east);
            out.setNorth(north);
            out.setAltitude(in.getAltitude());

            //Success
            return true;
        }

        return false;
    }

    public static boolean metricToGeodesic(MetricLocation in, GeodesicLocation out) {
        final double east = in.getEast();
        final double north = in.getNorth();

        //Validate MTM7 bounds
        if (east >= MTM7_EAST_LOWER_BOUND && east < MTM7_EAST_UPPER_BOUND && north >= MTM7_NORTH_LOWER_BOUND && north < MTM7_NORTH_UPPER_BOUND) {
            final double axis = NAD83_AXIS;
            final double eccent = NAD83_SQUARRED_ECCENTRICITY;
            final double scale = MTM_SCALE;
            final double eastingOrg = MTM_FLASE_EASTING;
            final double lonOriginRad = MTM7_ORIGIN_MERIDIAN_RAG;

            double easting = east - eastingOrg; //remove false easting offset for longitude
            double northing = north;

            //Iteratively compute variables required to compute Easting and Northing
            final double eccPrimeSquared = (eccent) / (1-eccent);
            final double sqrtInversedEccent = Math.sqrt(1 - eccent);
            final double e1 = (1 - sqrtInversedEccent) / (1 + sqrtInversedEccent);
            final double e2 = e1 * e1;
            final double e3 = e2 * e1;

            final double eccent2 = eccent * eccent;
            final double eccent3 = eccent2 * eccent;

            final double M = northing / scale;
            final double mu = M / (axis * (1 - eccent / 4 - 3 * eccent2 / 64 - 5 * eccent3 / 256));

            final double phi = mu + (3 * e1 / 2 - 27 * e3 / 32) * Math.sin(2 * mu) + (21 * e2 / 16 - 55 * e2 * e2 / 32) * Math.sin(4 * mu) + (151 * e3 / 96) * Math.sin(6 * mu);
            final double sinPhi = Math.sin(phi);
            final double sinPhi2 = sinPhi * sinPhi;
            final double cosPhi = Math.cos(phi);
            final double cosPhi2 = cosPhi * cosPhi;
            final double tanPhi = Math.tan(phi);
            final double tanPhi2 = tanPhi * tanPhi;
            final double tanPhi4 = tanPhi2 * tanPhi2;

            final double N1 = axis / Math.sqrt(1 - eccent * sinPhi2);
            final double C1 = eccPrimeSquared * cosPhi2;
            final double C2 = C1 * C1;
            final double R1 = axis * (1 - eccent) / Math.pow(1 - eccent * sinPhi2, 1.5);

            final double D = easting / (N1 * scale);
            final double D2 = D * D;
            final double D3 = D2 * D;

            //Compute latitude and longitude
            final double latRad = phi - (N1 * tanPhi / R1) * (D2 / 2 - (5 + 3 * tanPhi2 + 10 * C1 - 4 * C2 - 9 * eccPrimeSquared) * D2 * D2 / 24 + (61 + 90 * tanPhi2 + 298 * C1 + 45 * tanPhi4 - 252 * eccPrimeSquared - 3 * C2) * D3 * D3 / 720);
            final double lonRad = (D - (1 + 2 * tanPhi2 + C1) * D3 / 6 + (5 - 2 * C1 + 28 * tanPhi2 - 3 * C2 + 8 * eccPrimeSquared + 24 * tanPhi4) * D3 * D2 / 120) / cosPhi + lonOriginRad;

            //Update GeodesicLocation
            out.setLatitude(Math.toDegrees(latRad));
            out.setLongitude(Math.toDegrees(lonRad));
            out.setAltitude(in.getAltitude());

            //Success
            return true;
        }

        return false;
    }
}
