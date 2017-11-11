package ca.uqac.mobile.feet_tracker.model.geo;

import java.util.Map;

public class Segment {
    private String uid;
    private MetricLocation origin;
    private MetricLocation destination;

    public Segment(){
        //empty constructor required by firebase
    }

    public Segment(MetricLocation origin, MetricLocation destination){
        this.origin = origin;
        this.destination = destination;

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

    public Map<String, String> getOriginDate(){
        return origin.getDate();
    }

    public Map<String, String> getDestinationDate(){
        return destination.getDate();
    }

    public boolean addMetricLocation(MetricLocation metricLocation){
        if(!this.isFilled()){
            if(origin == null){
                origin = metricLocation;
            }
            else{
                destination = metricLocation;
            }
        }

        return this.isFilled();
    }

    private boolean isFilled() {
        return (origin != null && destination != null);
    }
}
