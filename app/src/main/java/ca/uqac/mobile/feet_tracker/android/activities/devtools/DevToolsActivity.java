package ca.uqac.mobile.feet_tracker.android.activities.devtools;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.devtools.graphviewer.GraphNode;
import ca.uqac.mobile.feet_tracker.android.activities.devtools.graphviewer.GraphViewerActivity;
import ca.uqac.mobile.feet_tracker.android.activities.devtools.segmentviewer.SegmentViewerActivity;

public class DevToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev_tools);

        final Button btnSegmentView = (Button) findViewById(R.id.devtool_segmentviewer);
        if (btnSegmentView != null) {
            btnSegmentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(DevToolsActivity.this, SegmentViewerActivity.class);
                    startActivity(intent);
                }
            });
        }

        final Button btnGraphView = (Button) findViewById(R.id.devtool_graphviewer);
        if (btnGraphView != null) {
            btnGraphView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent(DevToolsActivity.this, GraphViewerActivity.class);
                    startActivity(intent);
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
