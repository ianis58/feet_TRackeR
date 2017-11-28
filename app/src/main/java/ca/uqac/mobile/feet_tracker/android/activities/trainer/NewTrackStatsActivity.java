package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.model.geo.Track;

public class NewTrackStatsActivity extends AppCompatActivity {

    private EditText etNewTrackTitle;
    private TextView tvTrackDuration;
    private TextView tvLocationsCount;
    private TextView tvTrackUid;
    private Button btnTerminer;

    private String newTrackUid;
    private Long newTrackTimeMillis;
    private String newTrackTimeString;

    private FirebaseDatabase database;
    private DatabaseReference tracksRef;
    private DatabaseReference currentTrackRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Init UI components
        setContentView(R.layout.activity_new_track_stats);
        etNewTrackTitle = (EditText) findViewById(R.id.etNewTrackTitle);
        tvTrackDuration = (TextView) findViewById(R.id.tvTrackDuration);
        tvTrackUid = (TextView) findViewById(R.id.tvTrackUid);
        btnTerminer = (Button) findViewById(R.id.btnTerminer);

        //Init temporary data
        etNewTrackTitle.setText("");
        tvTrackDuration.setText("");
        etNewTrackTitle.setEnabled(false);
        btnTerminer.setEnabled(false);

        Intent intent = getIntent();

        if(intent != null){
            newTrackUid = intent.getStringExtra("newTrackUid");
            tvTrackUid.setText(newTrackUid);

            //newTrackTimeMillis = i.getLongExtra("newTrackTime", 0);
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
                        Intent intent = new Intent(NewTrackStatsActivity.this, LoginActivity.class);
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

        btnTerminer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Update title
                currentTrackRef.child("title").setValue(etNewTrackTitle.getText().toString());

                finish();
            }
        });

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
                    btnTerminer.setEnabled(true);
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
