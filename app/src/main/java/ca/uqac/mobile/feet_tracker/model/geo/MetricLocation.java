package ca.uqac.mobile.feet_tracker.model.geo;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class MetricLocation {

    private String uid;
    private double north;
    private double east;
    private double altitude;
    private Long date;

    public MetricLocation(double north, double east, double alt) {
        this.north = north;
        this.east = east;
        this.altitude = alt;
    }
    public MetricLocation(double north, double east) {
        this(north, east, 0.0);
    }
    public MetricLocation() {
        this(0.0, 0.0, 0.0);
    }

    public double getNorth() {
        return north;
    }

    public void setNorth(double north) {
        this.north = north;
    }

    public double getEast() {
        return east;
    }

    public void setEast(double east) {
        this.east = east;
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

        if(!(obj instanceof MetricLocation)){
            return false;
        }

        MetricLocation location = (MetricLocation) obj;

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

}
