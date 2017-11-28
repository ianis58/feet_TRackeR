package ca.uqac.mobile.feet_tracker.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ca.uqac.mobile.feet_tracker.android.services.segmentlogger.SegmentLoggerService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    public static void ensureSegmentLoggerServiceIsAlive(Context context) {
        //Start service (we don't care if it was already started)
        Intent segmentLoggerIntent = new Intent(context, SegmentLoggerService.class);
        context.startService(segmentLoggerIntent);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        //ACTION_BOOT_COMPLETED is received right after boot is completed, even if app is not opened
        //ACTION_TIME_TICK is received every minute when time change
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {
            ensureSegmentLoggerServiceIsAlive(context);
        }
    }
}
