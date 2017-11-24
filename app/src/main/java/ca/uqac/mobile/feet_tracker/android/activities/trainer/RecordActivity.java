package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.android.services.trackrecorder.TrackRecorderBinder;
import ca.uqac.mobile.feet_tracker.android.services.trackrecorder.TrackRecorderService;

public class RecordActivity extends AppCompatActivity {
    private static final String TAG = RecordActivity.class.getSimpleName();

    private Chronometer chronometerNewTrack;
    private ImageButton endRecordNewTrack;
    private ImageButton startRecordNewTrack;
    private SeekBar seekBarSamplingFrequency;
    private TextView tvSamplingFrequencyLabel;

    private String newTrackUid;
    private int samplingInterval;

    FirebaseDatabase database;
    DatabaseReference tracksRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    TrackRecorderService trackRecorderService;
    ServiceConnection serviceConnection;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        //outState.put
    }

    private void startTrackRecordingService() {
        Intent intent = new Intent(this, TrackRecorderService.class);

        //Create serviceConnection
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (service instanceof TrackRecorderBinder) {
                    trackRecorderService = ((TrackRecorderBinder) service).getService();

                    //That's where trackRecorderService will start sampling
                    trackRecorderService.startTracking(firebaseUser, newTrackUid, (float) samplingInterval);
                }
                else {
                    Log.e(TAG, String.format("Error, bound service is not a TrackRecorderBinder as expected: %s", service.getClass().getSimpleName()));
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                //Reconnect
                startTrackRecordingService();
            }
        };

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopTrackRecordingService() {
        chronometerNewTrack.stop();
        if (trackRecorderService != null) {
            trackRecorderService.stopTracking();
            trackRecorderService = null;
        }
        if (serviceConnection != null) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = FirebaseDatabase.getInstance();
        tracksRef = database.getReference("tracks");

        setContentView(R.layout.activity_record);

        chronometerNewTrack = (Chronometer) findViewById(R.id.chronometerNewTrack);
        startRecordNewTrack = (ImageButton) findViewById(R.id.btnStartRecordNewTrack);
        endRecordNewTrack = (ImageButton) findViewById(R.id.btnEndRecordNewTrack);
        seekBarSamplingFrequency = (SeekBar) findViewById(R.id.seekBarSamplingFrequency);
        tvSamplingFrequencyLabel = (TextView) findViewById(R.id.tvSamplingFrequencyLabel);

        endRecordNewTrack.setVisibility(View.GONE);

        samplingInterval = seekBarSamplingFrequency.getProgress();

        startRecordNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBarSamplingFrequency.setEnabled(false);
                startRecordNewTrack.setVisibility(View.GONE);
                endRecordNewTrack.setVisibility(View.VISIBLE);

                chronometerNewTrack.setBase(SystemClock.elapsedRealtime());
                chronometerNewTrack.start();

                //Find "now" formatted date
                Date today = new Date();
                SimpleDateFormat formatYMD = new SimpleDateFormat("YYYY-MM-dd HH:mm", Locale.US);
                String formatted = formatYMD.format(today);

                //Create a new track
                DatabaseReference newTrack = tracksRef.child(firebaseUser.getUid()).push();
                newTrackUid = newTrack.getKey();

                //Define some starting / default values
                //Starting duration: 0
                newTrack.child("duration").setValue(0);
                //Location list container
                newTrack.child("locations");
                //Default track title
                newTrack.child("title").setValue(String.format("Parcours du %s", formatted));

                startTrackRecordingService();
            }
        });

        endRecordNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrackRecordingService();

                //Update track's real duration
                long durationMillis = SystemClock.elapsedRealtime() - chronometerNewTrack.getBase();
                tracksRef.child(firebaseUser.getUid()).child(newTrackUid).child("duration").setValue(durationMillis/1000);

                showStats();
                finish();
            }
        });

        seekBarSamplingFrequency.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                samplingInterval = seekBarSamplingFrequency.getProgress();
                if(samplingInterval < 2){
                    samplingInterval = 2;
                    seekBarSamplingFrequency.setProgress(samplingInterval);
                }
                tvSamplingFrequencyLabel.setText("Fréquence d'échantillonnage de la position : " + samplingInterval + "s");
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

    private void showStats() {
        Intent intent = new Intent(RecordActivity.this, NewTrackStatsActivity.class);
        intent.putExtra("newTrackUid", newTrackUid);
        //intent.putExtra("newTrackTime", SystemClock.elapsedRealtime() - chronometerNewTrack.getBase());
        startActivity(intent);
    }
}
