package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.R;

public class RecordActivity extends AppCompatActivity {
    private static final String TAG = RecordActivity.class.getSimpleName();

    private Chronometer chronometerNewTrack;
    private Button endRecordNewTrack;

    FirebaseDatabase database;
    DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //database = FirebaseDatabase.getInstance();
        //myRef = database.getReference("locations");

        setContentView(R.layout.activity_record);

        chronometerNewTrack = (Chronometer) findViewById(R.id.chronometerNewTrack);
        endRecordNewTrack = (Button) findViewById(R.id.btnEndRecordNewTrack);

        endRecordNewTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chronometerNewTrack.stop();
                if(saveTrack()){
                    showStats();
                }
            }
        });

        chronometerNewTrack.start();
    }

    private void showStats() {

    }

    private boolean saveTrack() {
        //TODO: save "positions" or "MetricLocations" (should be done in

        return true;
    }
}
