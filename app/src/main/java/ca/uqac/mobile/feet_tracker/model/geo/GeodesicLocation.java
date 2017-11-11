package ca.uqac.mobile.feet_tracker.model.geo;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class GeodesicLocation {

    private String uid;
    private double latitude;
    private double longitude;
    private double altitude;
    private double locationId;
    private Long date;

    public GeodesicLocation(int locationsCount, double lat, double lon, double alt) {
        latitude = lat;
        longitude = lon;
        altitude = alt;
        locationId = locationsCount;
    }
    public GeodesicLocation(int locationsCount, double lat, double lon) {
        this(locationsCount, lat, lon, 0.0);
    }
    public GeodesicLocation() {
        this(0, 0.0, 0.0, 0.0);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }

        if(!(obj instanceof GeodesicLocation)){
            return false;
        }

        GeodesicLocation location = (GeodesicLocation) obj;

        return this.getUid().equals(location.getUid());
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }


    public Map<String, String> getDate(){
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public Long getLongDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public double getLocationId() {
        return locationId;
    }

    public void setLocationId(double locationId) {
        this.locationId = locationId;
    }
}
