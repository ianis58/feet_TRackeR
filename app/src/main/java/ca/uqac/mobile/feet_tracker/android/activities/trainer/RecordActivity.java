package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.android.activities.splash.SplashActivity;
import ca.uqac.mobile.feet_tracker.android.services.LocationTrackerService;

public class RecordActivity extends AppCompatActivity {
    private static final String TAG = RecordActivity.class.getSimpleName();

    private Chronometer chronometerNewTrack;
    private ImageButton endRecordNewTrack;
    private ImageButton startRecordNewTrack;
    private SeekBar seekBarSamplingFrequency;
    private TextView tvSamplingFrequencyLabel;
    private int SAMPLING_INTERVAL;

    private String newTrackUid;

    FirebaseDatabase database;
    DatabaseReference myRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        //outState.put
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("tracks");

        newTrackUid = myRef.push().getKey();

        setContentView(R.layout.activity_record);

        chronometerNewTrack = (Chronometer) findViewById(R.id.chronometerNewTrack);
        startRecordNewTrack = (ImageButton) findViewById(R.id.btnStartRecordNewTrack);
        endRecordNewTrack = (ImageButton) findViewById(R.id.btnEndRecordNewTrack);
        seekBarSamplingFrequency = (SeekBar) findViewById(R.id.seekBarSamplingFrequency);
        tvSamplingFrequencyLabel = (TextView) findViewById(R.id.tvSamplingFrequencyLabel);

        SAMPLING_INTERVAL = seekBarSamplingFrequency.getProgress();

        startRecordNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBarSamplingFrequency.setEnabled(false);
                startRecordNewTrack.setEnabled(false);

                chronometerNewTrack.setBase(SystemClock.elapsedRealtime());
                chronometerNewTrack.start();

                Intent intent = new Intent(getBaseContext(), LocationTrackerService.class);
                intent.putExtra("newTrackUid", newTrackUid);
                intent.putExtra("samplingInterval", SAMPLING_INTERVAL);

                startService(intent);
            }
        });

        endRecordNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationTrackerService();
                showStats();
                finish();
            }
        });

        seekBarSamplingFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                SAMPLING_INTERVAL = seekBarSamplingFrequency.getProgress();
                if(SAMPLING_INTERVAL < 2){
                    SAMPLING_INTERVAL = 2;
                    seekBarSamplingFrequency.setProgress(SAMPLING_INTERVAL);
                }
                tvSamplingFrequencyLabel.setText("Fréquence d'échantillonnage de la position : " + SAMPLING_INTERVAL + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
        Intent intent = new Intent(RecordActivity.this, NewTrackStatsActivity.class);
        intent.putExtra("newTrackUid", newTrackUid);
        intent.putExtra("newTrackTime", SystemClock.elapsedRealtime() - chronometerNewTrack.getBase());
        startActivity(intent);
    }
}
