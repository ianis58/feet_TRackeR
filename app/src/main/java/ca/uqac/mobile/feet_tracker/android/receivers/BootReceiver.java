package ca.uqac.mobile.feet_tracker.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ca.uqac.mobile.feet_tracker.android.services.LocationTrackerService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, LocationTrackerService.class);
        //Toast.makeText(context, "Starting sampling service...1", Toast.LENGTH_LONG).show();
        context.startService(i);
    }
}
