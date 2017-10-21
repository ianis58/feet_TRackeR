package ca.uqac.mobile.feet_tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceStarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent("PersistingSamplingService");
        //Toast.makeText(context, "Starting sampling service...1", Toast.LENGTH_LONG).show();
        i.setClass(context, PersistingSamplingService.class);
        context.startService(i);
    }
}
