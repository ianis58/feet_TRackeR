package ca.uqac.mobile.feet_tracker.android.activities.trainer.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.trainer.TrackStatsActivity;
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
        final Track track = tracks.get(position);

        ((TrackViewHolder) holder).bind(track);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = v.getContext();

                Intent intent = new Intent(context, TrackStatsActivity.class);
                intent.putExtra("newTrackUid", track.getUid());
                //intent.putExtra("newTrackTime", SystemClock.elapsedRealtime() - chronometerNewTrack.getBase());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public void addTrack(Track track){
        tracks.add(track);
        notifyDataSetChanged();
    }

    public void deleteTrack(Track track) {
        int index = tracks.indexOf(track);
        //tracks.remove(index);
        notifyItemRemoved(index);
    }

    public void clearTrack(){
        tracks.clear();
        notifyDataSetChanged();
    }

    public void updateTrack(Track track) {
        //TODO: find a better solution (i.e. replace track ref in tracks)
        if (track != null) {
            for (Track t : tracks) {
                if (t != null && t.getUid() != null) {
                    if (t.getUid().equals(track.getUid())) {
                        t.setDuration(track.getDuration());
                        t.setTitle(track.getTitle());

                        notifyDataSetChanged();
                        return;
                    }
                }
            }
        }
    }

    private class TrackViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView duration;
        private TextView date;

        public TrackViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.trackTitle);
            duration = (TextView) itemView.findViewById(R.id.trackDuration);
            //locationsCount = (TextView) itemView.findViewById(R.id.trackLocationsCount);
            date = (TextView) itemView.findViewById(R.id.trackDate);
        }



        void bind(Track track){
            title.setText(track.getTitle());
            String trackTimeString = "";

            trackTimeString = String.format("%02d:%02d:%02d", TimeUnit.SECONDS.toHours(track.getDuration()),
                    TimeUnit.SECONDS.toMinutes(track.getDuration()) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.SECONDS.toSeconds(track.getDuration()) % TimeUnit.MINUTES.toSeconds(1));

            duration.setText(trackTimeString);
            Date trackDate = track.getDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm");
            date.setText("le "+sdf.format(trackDate)+" à "+sdf2.format(trackDate));

        }
    }
}
