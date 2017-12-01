package ca.uqac.mobile.feet_tracker.android.activities.trainer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.activities.login.LoginActivity;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.Segment;
import ca.uqac.mobile.feet_tracker.model.geo.Track;
import ca.uqac.mobile.feet_tracker.tools.MTM7Converter;

import static ca.uqac.mobile.feet_tracker.android.activities.devtools.DevToolsActivity.DEV_TOOLS_PREFS;

public class TrackStatsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etNewTrackTitle;
    private TextView tvTrackDuration;
    private TextView tvLocationsCount;
    private TextView tvTrackUid;
    private TextView tvTrackUidLabel;
    private TextView tvTrackDate;
    private TextView tvTrackSpeed;

    private String newTrackUid;
    private String userUid;
    private Long newTrackTimeMillis;
    private String newTrackTimeString;
    private ArrayList<Double> speeds;

    private FirebaseDatabase database;
    private DatabaseReference tracksRef;
    private DatabaseReference currentTrackRef;
    private DatabaseReference segmentsRef;
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser firebaseUser;

    private GoogleMap mMap;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trainer_track_stats_actionbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_save_track:
                saveTrackAndClose();
                break;
        }

        return true;
    }

    private void saveTrackAndClose() {
        //Update title
        currentTrackRef.child("title").setValue(etNewTrackTitle.getText().toString());

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Init UI components
        setContentView(R.layout.activity_track_stats);
        etNewTrackTitle = (EditText) findViewById(R.id.etNewTrackTitle);
        tvTrackDuration = (TextView) findViewById(R.id.tvTrackDuration);
        tvTrackUid = (TextView) findViewById(R.id.tvTrackUid);
        tvTrackUidLabel = (TextView) findViewById(R.id.tvTrackUidLabel);
        tvTrackDate = (TextView) findViewById(R.id.tvTrackDate);
        tvTrackSpeed = (TextView) findViewById(R.id.tvTrackSpeed);

        speeds = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapTrack);
        mapFragment.getMapAsync(this);

        SharedPreferences devToolsPrefs = getSharedPreferences(DEV_TOOLS_PREFS, MODE_PRIVATE);
        boolean showTracksIds = devToolsPrefs.getBoolean("showTracksIds", false);
        if(!showTracksIds){
            tvTrackUid.setVisibility(View.GONE);
            tvTrackUidLabel.setVisibility(View.GONE);
        }

        //Init temporary data
        etNewTrackTitle.setText("");
        tvTrackDuration.setText("");
        etNewTrackTitle.setEnabled(false);

        Intent intent = getIntent();

        if(intent != null){
            newTrackUid = intent.getStringExtra("newTrackUid");
            tvTrackUid.setText(newTrackUid);

        }
        else{
            newTrackUid = "";
            Toast.makeText(getBaseContext(), "Pas d'uid pour le parcours...", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //Init database
        database = FirebaseDatabase.getInstance();

        //Try to fetch user
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            //Couldn't get the user, try to get it otherwise
            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseAuth.getInstance().removeAuthStateListener(this);
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        //OK we found the connected user
                        firebaseUser = user;
                        //We're now ready to fetch track
                        //fetchTrackData();
                    } else {
                        Intent intent = new Intent(TrackStatsActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            };
            FirebaseAuth.getInstance().addAuthStateListener(authStateListener);
        }
        else {
            //OK we already have user, fetch Track right away
        }

        userUid = firebaseUser.getUid();

        tracksRef = database.getReference("tracks");
        segmentsRef = database.getReference("tracks").child(userUid).child(newTrackUid).child("segments");

        fetchTrackData();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fetchTrackData() {
        //Ensure firebaseUser is valid
        if (!userUid.isEmpty() && !"".equals(newTrackUid)) {
            //Find current track ref
            currentTrackRef = tracksRef.child(userUid).child(newTrackUid);

            //Register a callback to fetch data
            currentTrackRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currentTrackRef.removeEventListener(this);

                    Track track = dataSnapshot.getValue(Track.class);

                    newTrackTimeMillis = track.getDuration() * 1000;

                    newTrackTimeString = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(newTrackTimeMillis),
                            TimeUnit.MILLISECONDS.toMinutes(newTrackTimeMillis) % TimeUnit.HOURS.toMinutes(1),
                            TimeUnit.MILLISECONDS.toSeconds(newTrackTimeMillis) % TimeUnit.MINUTES.toSeconds(1));

                    etNewTrackTitle.setText(track.getTitle());
                    tvTrackDuration.setText(newTrackTimeString);
                    tvTrackDate.setText(track.getStringDate());

                    //OK we can enable title edit and update button
                    etNewTrackTitle.setEnabled(true);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    currentTrackRef.removeEventListener(this);
                    Toast.makeText(getBaseContext(), "Impossible de charger les informations de la scéance.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        refreshMap();
    }

    private final GeodesicLocation geodesicLocation = new GeodesicLocation();

    private void refreshMap() {
        segmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Bounds to ensure the map will show everything
                final LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();



                for (DataSnapshot segmentSnapshot : dataSnapshot.getChildren()) {
                    final Segment segment = segmentSnapshot.getValue(Segment.class);

                    final double speed = segment.getSpeed();

                    speeds.add(speed);

                    final int color;
                    if (speed < Segment.MIN_WALK_SPEED) {
                        color = Color.BLACK;
                    }
                    else if (speed < Segment.MIN_RUN_SPEED) {
                        color = Color.GREEN;
                    }
                    else if (speed < Segment.MIN_VEHICULE_SPEED) {
                        color = Color.YELLOW;
                    }
                    else if (speed < Segment.MAX_VEHICULE_SPEED) {
                        color = 0xffffa500; //Orange
                    }
                    else {
                        color = Color.RED;
                    }

                    PolylineOptions polylineOptions = new PolylineOptions()
                            .clickable(true)
                            .geodesic(false)
                            .color(color);

                    LatLng latLng;

                    //Convert metric coordinates to geodesic coordinate
                    MTM7Converter.metricToGeodesic(segment.getOrigin(), geodesicLocation);
                    latLng = new LatLng(geodesicLocation.getLatitude(), geodesicLocation.getLongitude());
                    polylineOptions.add(latLng);
                    boundsBuilder.include(latLng);

                    MTM7Converter.metricToGeodesic(segment.getDestination(), geodesicLocation);
                    latLng = new LatLng(geodesicLocation.getLatitude(), geodesicLocation.getLongitude());
                    polylineOptions.add(latLng);
                    boundsBuilder.include(latLng);

                    mMap.addPolyline(polylineOptions);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50));

                //computing harmonic mean of all segments speeds in order to show track mean speed
                int numerateur = speeds.size();//n
                double denominateur = 0.0;
                for(Double speed : speeds){
                    if(speed != 0) {
                        denominateur += 1 / speed;
                    }
                }
                double harmonicMean = (double)numerateur / denominateur;

                double scale = Math.pow(10, 1);//1 decimal only
                harmonicMean = Math.round(harmonicMean * scale) / scale;

                tvTrackSpeed.setText(harmonicMean + "km/h");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
