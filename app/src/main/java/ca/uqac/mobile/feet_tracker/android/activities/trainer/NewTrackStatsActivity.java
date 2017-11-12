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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;

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
    private DatabaseReference myRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_track_stats);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("tracks");


        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    firebaseUser = user;
                }
                else {
                    Intent intent = new Intent(NewTrackStatsActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener);

        etNewTrackTitle = (EditText) findViewById(R.id.etNewTrackTitle);
        tvTrackDuration = (TextView) findViewById(R.id.tvTrackDuration);
        tvTrackUid = (TextView) findViewById(R.id.tvTrackUid);
        btnTerminer = (Button) findViewById(R.id.btnTerminer);

        Intent i = getIntent();

        if(i != null){
            newTrackUid = i.getStringExtra("newTrackUid");
            tvTrackUid.setText(newTrackUid);

            newTrackTimeMillis = i.getLongExtra("newTrackTime", 0);

            newTrackTimeString = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(newTrackTimeMillis),
                    TimeUnit.MILLISECONDS.toMinutes(newTrackTimeMillis) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(newTrackTimeMillis) % TimeUnit.MINUTES.toSeconds(1));

            tvTrackDuration.setText(newTrackTimeString);
        }
        else{
            Toast.makeText(getBaseContext(), "Pas d'uid pour le parcours...", Toast.LENGTH_LONG).show();
            finish();
        }


        btnTerminer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myRef.child(firebaseUser.getUid()).child(newTrackUid).child("title").setValue(etNewTrackTitle.getText().toString());
                myRef.child(firebaseUser.getUid()).child(newTrackUid).child("duration").setValue(newTrackTimeMillis/1000);

                startActivity(new Intent(getBaseContext(), TrainerActivity.class));

                finish();
            }
        });

    }


}
