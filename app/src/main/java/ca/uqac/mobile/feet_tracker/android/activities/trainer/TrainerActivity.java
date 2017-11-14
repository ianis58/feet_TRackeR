package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.android.activities.trainer.adapters.TrackAdapter;
import ca.uqac.mobile.feet_tracker.model.geo.Track;

import static android.support.v7.widget.RecyclerView.HORIZONTAL;

public class TrainerActivity extends AppCompatActivity {
    private static final String TAG = TrainerActivity.class.getSimpleName();

    private RecyclerView recyclerViewTracksHistory;

    private TrackAdapter adapter;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

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

        //recyclerViewTracksHistory = (RecyclerView) findViewById(R.id.recyclerViewTracksHistory);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("tracks");


        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //Log.w(TAG, "onAuthStateChanged: user is not null.");
                    firebaseUser = user;
                }
                else {
                    //Log.w(TAG, "onAuthStateChanged: user is null.");
                    Intent intent = new Intent(TrainerActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);



        recyclerViewTracksHistory = (RecyclerView) findViewById(R.id.recyclerViewTracksHistory);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        recyclerViewTracksHistory.setLayoutManager(manager);
        DividerItemDecoration itemDecor = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerViewTracksHistory.addItemDecoration(itemDecor);
        adapter = new TrackAdapter();
        recyclerViewTracksHistory.setAdapter(adapter);

        //Query query = myRef.child("kPllxxwTYsUgrzQTMqoU32ud8Y72");
        FirebaseDatabase.getInstance().getReference().child("tracks").child("kPllxxwTYsUgrzQTMqoU32ud8Y72")
                .addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                /*for (DataSnapshot snapshot: dataSnapshot.getChildren()) {

                    Track track = snapshot.getValue(Track.class);
                    if(track != null){
                        Toast.makeText(getBaseContext(), track.getTitle(), Toast.LENGTH_LONG).show();
                    }
                }*/

/*                String trackUid = dataSnapshot.getKey();
                Toast.makeText(getBaseContext(), trackUid, Toast.LENGTH_LONG).show();*/

                Track track = dataSnapshot.getValue(Track.class);//le track est récupéré...
                adapter.addTrack(track);
                /*if (track != null) {
                    Toast.makeText(getBaseContext(), track.getTitle(), Toast.LENGTH_LONG).show();
                }*/
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

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
