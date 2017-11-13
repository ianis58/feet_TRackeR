package ca.uqac.mobile.feet_tracker.model.geo;

import java.util.List;

public class Track {
    private String uid;
    private String title;
    private Long duration;
    private List<GeodesicLocation> locations;

    public Track() {
        //
    }

    public Track(String uid, String title, Long duration){
        this.uid = uid;
        this.title = title;
        this.duration = duration;
    }

    public void addLocation(GeodesicLocation loc){
        locations.add(loc);
    }

    public int getLocationsCount(){
        return locations.size();
    }

    public List<GeodesicLocation> getLocations(){
        return locations;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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
}
