package ca.uqac.mobile.feet_tracker.android.activities.devtools;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.devtools.segmentviewer.SegmentViewerActivity;

public class DevToolsActivity extends AppCompatActivity {

    public static final String DEV_TOOLS_PREFS = "DevToolsPreferences";

    private Button btnSegmentView;
    private CheckBox cbShowTracksIds;
    private CheckBox cbShowSamplingFrequency;
    private CheckBox cbSkipLoadingSplash;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_tools);

        findViews();
        setViewsListeners();
        restoreCheckboxesState();
    }

    private void restoreCheckboxesState() {
        SharedPreferences devToolsPrefs = getSharedPreferences(DEV_TOOLS_PREFS, MODE_PRIVATE);
        boolean showTracksIds = devToolsPrefs.getBoolean("showTracksIds", false);
        boolean showSamplingFrequency = devToolsPrefs.getBoolean("showSamplingFrequency", false);
        boolean skipLoadingSplash = devToolsPrefs.getBoolean("skipLoadingSplash", false);
        cbShowTracksIds.setChecked(showTracksIds);
        cbShowSamplingFrequency.setChecked(showSamplingFrequency);
        cbSkipLoadingSplash.setChecked(skipLoadingSplash);
    }

    private void findViews() {
        btnSegmentView = (Button) findViewById(R.id.devtool_segmentviewer);
        cbShowTracksIds = (CheckBox) findViewById(R.id.cbShowTracksIds);
        cbShowSamplingFrequency = (CheckBox) findViewById(R.id.cbShowSamplingFrequency);
        cbSkipLoadingSplash = (CheckBox) findViewById(R.id.cbSkipLoadingSplash);
    }

    private void setViewsListeners(){
        if (btnSegmentView != null) {
            btnSegmentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(DevToolsActivity.this, SegmentViewerActivity.class);
                    startActivity(intent);
                }
            });
        }

        if(cbShowTracksIds != null){
            cbShowTracksIds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences.Editor editor = getSharedPreferences(DEV_TOOLS_PREFS, MODE_PRIVATE).edit();
                    editor.putBoolean("showTracksIds", isChecked);
                    editor.apply();
                }
            });
        }

        if(cbShowSamplingFrequency != null){
            cbShowSamplingFrequency.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences.Editor editor = getSharedPreferences(DEV_TOOLS_PREFS, MODE_PRIVATE).edit();
                    editor.putBoolean("showSamplingFrequency", isChecked);
                    editor.apply();
                }
            });
        }

        if(cbSkipLoadingSplash != null){
            cbSkipLoadingSplash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SharedPreferences.Editor editor = getSharedPreferences(DEV_TOOLS_PREFS, MODE_PRIVATE).edit();
                    editor.putBoolean("skipLoadingSplash", isChecked);
                    editor.apply();
                }
            });
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
