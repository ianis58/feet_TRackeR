package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import ca.uqac.mobile.feet_tracker.R;

public class TrainerActivity extends AppCompatActivity {

    private ListView tracksHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer);

        tracksHistory = (ListView) findViewById(R.id.listViewTracksHistory);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trainer_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_new_track: {
                Intent intent = new Intent(TrainerActivity.this, RecordActivity.class);
                startActivity(intent);

                return true;
            }

        }

        return true;
    }
}
