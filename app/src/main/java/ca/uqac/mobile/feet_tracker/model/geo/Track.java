package ca.uqac.mobile.feet_tracker.model.geo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class Track {
    private String uid;
    private String title;
    private Long duration;
    private Long date;
    private HashMap<String,GeodesicLocation> locations;

    public Track() {
        //
    }

    public Track(String uid, String title, Long duration, Long date){
        this.uid = uid;
        this.title = title;
        this.duration = duration;
        this.date = date;
    }

    public void addLocation(String k, GeodesicLocation loc){
        locations.put(k, loc);
    }

    public int getLocationsCount(){
        return locations.size();
    }

    public HashMap<String,GeodesicLocation> getLocations(){
        return locations;
    }

    public void setLocations(HashMap<String, GeodesicLocation> locations) {
        this.locations = locations;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }

        if(!(obj instanceof Track)){
            return false;
        }

        Track track = (Track) obj;

        return this.getUid().equals(track.getUid());
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Date getDate(){
        return new Date((this.date == null) ? 0 : this.date);
    }
}
