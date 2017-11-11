package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.android.services.LocationTrackerService;

public class RecordActivity extends AppCompatActivity {
    private static final String TAG = RecordActivity.class.getSimpleName();

    private Chronometer chronometerNewTrack;
    private Button endRecordNewTrack;
    private Button startRecordNewTrack;
    private EditText etTrackTitle;

    private String newTrackUid;

    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("tracks");

        newTrackUid = myRef.push().getKey();

        setContentView(R.layout.activity_record);

        chronometerNewTrack = (Chronometer) findViewById(R.id.chronometerNewTrack);
        startRecordNewTrack = (Button) findViewById(R.id.btnStartRecordNewTrack);
        endRecordNewTrack = (Button) findViewById(R.id.btnEndRecordNewTrack);
        etTrackTitle = (EditText) findViewById(R.id.etTrackTitle);

        startRecordNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chronometerNewTrack.start();

                Intent intent = new Intent(getBaseContext(), LocationTrackerService.class);
                intent.putExtra("newTrackUid", newTrackUid);
                startService(intent);
            }
        });

        endRecordNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationTrackerService();
                showStats();
            }
        });

        etTrackTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!etTrackTitle.getText().toString().isEmpty()){
                    myRef.child(firebaseUser.getUid()).child(newTrackUid).child("title").setValue(etTrackTitle.getText().toString());
                }
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    firebaseUser = user;
                }
                else {
                    Intent intent = new Intent(RecordActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
    }

    private void stopLocationTrackerService() {
        stopService(new Intent(this, LocationTrackerService.class));
        chronometerNewTrack.stop();
    }

    private void showStats() {

    }
}
