package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Chronometer;

import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.R;

public class RecordActivity extends AppCompatActivity {

    private Chronometer chronometerNewTrack;
    private Button endRecordNewTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //database = FirebaseDatabase.getInstance();
        //myRef = database.getReference("locations");

        setContentView(R.layout.activity_record);

        chronometerNewTrack = (Chronometer) findViewById(R.id.chronometerNewTrack);
        endRecordNewTrack = (Button) findViewById(R.id.btnEndRecordNewTrack);
    }
}
