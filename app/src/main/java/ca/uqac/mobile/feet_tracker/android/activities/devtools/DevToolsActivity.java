package ca.uqac.mobile.feet_tracker.android.activities.devtools;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import ca.uqac.mobile.feet_tracker.R;

public class DevToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_tools);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
