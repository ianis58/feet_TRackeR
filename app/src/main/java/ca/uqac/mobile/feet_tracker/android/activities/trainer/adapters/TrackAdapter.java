package ca.uqac.mobile.feet_tracker.android.activities.trainer.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.model.geo.Track;

public class TrackAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<Track> tracks;

    public TrackAdapter() {
        this.tracks = new ArrayList<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_history_row, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Track track = tracks.get(position);

        ((TrackViewHolder) holder).bind(track);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public void addTrack(Track track){
        tracks.add(track);
        notifyDataSetChanged();
    }

    public void deleteTrack(Track track){
        int index = tracks.indexOf(track);
        tracks.remove(index);
        notifyItemRemoved(index);
    }

    public void clearTrack(){
        tracks.clear();
        notifyDataSetChanged();
    }

    private class TrackViewHolder extends RecyclerView.ViewHolder{

        private TextView title;
        private TextView duration;
        private TextView locationsCount;
        private TextView date;

        public TrackViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.trackTitle);
            duration = (TextView) itemView.findViewById(R.id.trackDuration);
            locationsCount = (TextView) itemView.findViewById(R.id.trackLocationsCount);
            date = (TextView) itemView.findViewById(R.id.trackDate);
        }

        void bind(Track track){
            title.setText(track.getTitle());
            duration.setText(track.getDuration().toString());
        }
    }
}
