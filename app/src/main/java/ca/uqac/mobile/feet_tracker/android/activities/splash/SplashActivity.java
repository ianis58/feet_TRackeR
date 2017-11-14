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

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.android.services.locationtracker.LocationTrackerService;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = SplashActivity.class.getSimpleName();

    private static final int FINE_LOCATION_PERMISSION_REQUEST_ID = 1;

    private void startLocationTracker() {
        Intent serviceIntent = new Intent(this, LocationTrackerService.class);
        startService(serviceIntent);

    }

    private void launchLogin() {
        //Wait 2 seconds before starting LoginActivity (just to show the splash screen a bit)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        }, 2000);
    }

    private boolean hasGPSPermission() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void checkForPermissions() {
        if (hasGPSPermission()) {
            launchLogin();
        }
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //Afficher un message informant que l'accès au GPS est essentiel au fonctionnement de l'appli
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle(R.string.splash_request_gps_title);
                alertBuilder.setMessage(R.string.splash_request_gps_message);

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

        checkForPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int len = permissions.length;

        for (int i = 0; i < len; ++i) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    launchLogin();
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
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    finish();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
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
