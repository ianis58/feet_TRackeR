package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;

public class TrainerActivity extends AppCompatActivity {
    private static final String TAG = TrainerActivity.class.getSimpleName();

    private RecyclerView recyclerViewTracksHistory;

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

        //recyclerViewTracksHistory = (RecyclerView) findViewById(R.id.recyclerViewTracksHistory);

        Query query = myRef.child(firebaseUser.getUid());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

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
