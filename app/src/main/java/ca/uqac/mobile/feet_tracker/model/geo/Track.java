package ca.uqac.mobile.feet_tracker.model.geo;

import com.google.firebase.database.Exclude;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Track {
    private String uid;
    private String title;
    private Long duration;
    private Long date;
    private HashMap<String,Segment> segments;

    public Track() {
    }

    public Track(String uid, String title, Long duration, Long date){
        this.uid = uid;
        this.title = title;
        this.duration = duration;
        this.date = date;
    }

    public int getSegmentsCount(){
        return segments.size();
    }

    public HashMap<String,Segment> getSegments(){
        return segments;
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

    @Exclude
    public Long getLongDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getStringDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm");
        return "le " + sdf.format(getDate()) + " à " + sdf2.format(getDate());
    }
}
