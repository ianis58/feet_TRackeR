package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.android.activities.trainer.adapters.TrackAdapter;
import ca.uqac.mobile.feet_tracker.model.geo.Track;


public class TrainerActivity extends AppCompatActivity {
    private static final String TAG = TrainerActivity.class.getSimpleName();

    private RecyclerView recyclerViewTracksHistory;

    private TrackAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference tracksRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;
    String userUid;

    private void startRecordActivity() {
        Intent intent = new Intent(TrainerActivity.this, RecordActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecordActivity();
            }
        });

        Intent i = getIntent();

        if(i != null) {
            userUid = i.getStringExtra("userUid");
        }

        database = FirebaseDatabase.getInstance();
        tracksRef = database.getReference("tracks");

        //Get firebase user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String tmpUserUid = firebaseUser.getUid();
        }

        if (firebaseUser == null) {
            //If firebase user unavailable, try another method
            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseAuth.getInstance().removeAuthStateListener(this);

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        firebaseUser = user;
                    } else {
                        Intent intent = new Intent(TrainerActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            };

            FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
        }

        recyclerViewTracksHistory = (RecyclerView) findViewById(R.id.recyclerViewTracksHistory);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerViewTracksHistory.setLayoutManager(manager);//associate a default LayoutManager to our RecyclerView

        DividerItemDecoration itemDecor = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerViewTracksHistory.addItemDecoration(itemDecor);//show a divider between every tracks in our RecyclerView

        adapter = new TrackAdapter();
        recyclerViewTracksHistory.setAdapter(adapter);//define the TrackAdapter as the adapter we want to use for our RecyclerView

        tracksRef.child(userUid)
                .addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Track track = dataSnapshot.getValue(Track.class);
                track.setUid(dataSnapshot.getKey());
                adapter.addTrack(track);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Track track = dataSnapshot.getValue(Track.class);
                track.setUid(dataSnapshot.getKey());
                adapter.updateTrack(track);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Track track = dataSnapshot.getValue(Track.class);
                track.setUid(dataSnapshot.getKey());
                adapter.deleteTrack(track);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /*MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trainer_actionbar, menu);*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_new_track: {
                startRecordActivity();

                return true;
            }

        }

        return true;
    }
}
