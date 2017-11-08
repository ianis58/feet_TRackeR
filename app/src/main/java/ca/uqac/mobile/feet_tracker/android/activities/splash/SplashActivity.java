package ca.uqac.mobile.feet_tracker.android.activities.splash;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.tracker.TrackerActivity;
import ca.uqac.mobile.feet_tracker.android.services.LocationTrackerService;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = SplashActivity.class.getSimpleName();

    private static final int FINE_LOCATION_PERMISSION_REQUEST_ID = 1;

    FirebaseDatabase database;
    DatabaseReference myRef;

    private void startLocationTracker() {
        Intent serviceIntent = new Intent(this, LocationTrackerService.class);
        startService(serviceIntent);

    }

    private void launchTracker() {
        //Wait 2 seconds before starting TrackerActivity (just to show the splash screen a bit)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, TrackerActivity.class);
                startActivity(intent);
            }
        }, 2000);
    }

    private boolean hasGPSPermission() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void checkForPermissions() {
        if (hasGPSPermission()) {
            launchTracker();
        }
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //Afficher un message informant que l'accès au GPS est essentiel au fonctionnement de l'appli
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle("Permission nécessaire");
                alertBuilder.setMessage("L'accès à la position GPS précise est nécessaire pour le bon fonctionnement de l'application.");

                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    FINE_LOCATION_PERMISSION_REQUEST_ID);
                        }
                    }
                });

                AlertDialog alert = alertBuilder.create();
                alert.show();

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST_ID);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("locations");

        checkForPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int len = permissions.length;

        for (int i = 0; i < len; ++i) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    launchTracker();
                }
                else {
                    new AlertDialog.Builder(this)
                            .setCancelable(true)
                            .setTitle("Accès au GPS requis")
                            .setMessage("Le droit d'accès à la géolocalisation est nécessaire au fonctionnement de l'application.")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .create()
                            .show();

                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
