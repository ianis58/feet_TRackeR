package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.model.geo.Track;

import static ca.uqac.mobile.feet_tracker.android.activities.devtools.DevToolsActivity.DEV_TOOLS_PREFS;

public class TrackStatsActivity extends AppCompatActivity {

    private EditText etNewTrackTitle;
    private TextView tvTrackDuration;
    private TextView tvLocationsCount;
    private TextView tvTrackUid;
    private TextView tvTrackUidLabel;

    private String newTrackUid;
    private Long newTrackTimeMillis;
    private String newTrackTimeString;

    private FirebaseDatabase database;
    private DatabaseReference tracksRef;
    private DatabaseReference currentTrackRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trainer_track_stats_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_save_track:
                saveTrackAndClose();
                break;
        }

        return true;
    }

    private void saveTrackAndClose() {
        //Update title
        currentTrackRef.child("title").setValue(etNewTrackTitle.getText().toString());
        currentTrackRef.child("date").setValue(System.currentTimeMillis());

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Init UI components
        setContentView(R.layout.activity_track_stats);
        etNewTrackTitle = (EditText) findViewById(R.id.etNewTrackTitle);
        tvTrackDuration = (TextView) findViewById(R.id.tvTrackDuration);
        tvTrackUid = (TextView) findViewById(R.id.tvTrackUid);
        tvTrackUidLabel = (TextView) findViewById(R.id.tvTrackUidLabel);

        SharedPreferences devToolsPrefs = getSharedPreferences(DEV_TOOLS_PREFS, MODE_PRIVATE);
        boolean showTracksIds = devToolsPrefs.getBoolean("showTracksIds", false);
        if(!showTracksIds){
            tvTrackUid.setVisibility(View.GONE);
            tvTrackUidLabel.setVisibility(View.GONE);
        }

        //Init temporary data
        etNewTrackTitle.setText("");
        tvTrackDuration.setText("");
        etNewTrackTitle.setEnabled(false);

        Intent intent = getIntent();

        if(intent != null){
            newTrackUid = intent.getStringExtra("newTrackUid");
            tvTrackUid.setText(newTrackUid);

        }
        else{
            newTrackUid = "";
            Toast.makeText(getBaseContext(), "Pas d'uid pour le parcours...", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //Init database
        database = FirebaseDatabase.getInstance();
        tracksRef = database.getReference("tracks");

        //Try to fetch user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            //Couldn't get the user, try to get it otherwise
            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseAuth.getInstance().removeAuthStateListener(this);
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        //OK we found the connected user
                        firebaseUser = user;
                        //We're now ready to fetch track
                        fetchTrackData();
                    } else {
                        Intent intent = new Intent(TrackStatsActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            };
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
        }
        else {
            //OK we already have user, fetch Track right away
            fetchTrackData();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fetchTrackData() {
        //Ensure firebaseUser is valid
        if (firebaseUser != null && !"".equals(newTrackUid)) {
            //Find current track ref
            currentTrackRef = tracksRef.child(firebaseUser.getUid()).child(newTrackUid);

            //Register a callback to fetch data
            currentTrackRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currentTrackRef.removeEventListener(this);

                    Track track = dataSnapshot.getValue(Track.class);

                    newTrackTimeMillis = track.getDuration() * 1000;

                    newTrackTimeString = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(newTrackTimeMillis),
                            TimeUnit.MILLISECONDS.toMinutes(newTrackTimeMillis) % TimeUnit.HOURS.toMinutes(1),
                            TimeUnit.MILLISECONDS.toSeconds(newTrackTimeMillis) % TimeUnit.MINUTES.toSeconds(1));

                    etNewTrackTitle.setText(track.getTitle());
                    tvTrackDuration.setText(newTrackTimeString);

                    //OK we can enable title edit and update button
                    etNewTrackTitle.setEnabled(true);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    currentTrackRef.removeEventListener(this);
                    Toast.makeText(getBaseContext(), "Impossible de charger les informations de la scéance.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }


}
