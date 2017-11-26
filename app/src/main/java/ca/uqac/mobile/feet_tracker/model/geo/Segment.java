package ca.uqac.mobile.feet_tracker.model.geo;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class Segment {
    private String uid;
    private MetricLocation origin = new MetricLocation();
    private MetricLocation destination = new MetricLocation();
    private String userID;
    private double speed;
    private long date;

    public Segment(){
        //empty constructor required by firebase
    }

    public Segment(MetricLocation origin, MetricLocation destination, String userID){
        setOrigin(origin);
        setDestination(destination);
        this.userID = userID;
    }

    public String getUid() {
        return uid;
    }

    public MetricLocation getOrigin() {
        return origin;
    }

    public MetricLocation getDestination() {
        return destination;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setOrigin(MetricLocation origin) {
        this.origin.copyFrom(origin);
    }

    public void setDestination(MetricLocation destination) {
        this.destination.copyFrom(destination);
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public Map<String, String> getDate() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public long getLongDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

}
