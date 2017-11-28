package ca.uqac.mobile.feet_tracker.android.activities.devtools.segmentviewer;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.Segment;
import ca.uqac.mobile.feet_tracker.tools.MTM7Converter;

public class SegmentViewerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FirebaseDatabase database;
    private DatabaseReference segmentsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segment_viewer);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        database = FirebaseDatabase.getInstance();
        segmentsRef = database.getReference("segments");

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.segmentviewermenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();

        switch (id) {
            case R.id.devtool_segmentviewer_clean_invalid:
                deleteOffRangeSegments(Segment.MIN_WALK_SPEED, Segment.MAX_VEHICULE_SPEED);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteOffRangeSegments(final double min, final double max) {
        segmentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int removeCount = 0;
                for (DataSnapshot segmentSnapshot : dataSnapshot.getChildren()) {
                    final Segment segment = segmentSnapshot.getValue(Segment.class);

                    final double speed = segment.getSpeed();
                    if (speed < min || speed > max) {
                        segmentSnapshot.getRef().removeValue();
                        --removeCount;
                    }
                }
                Toast.makeText(
                        SegmentViewerActivity.this,
                        String.format("%d segment(s) invalide(s) effacé(s)", removeCount),
                        Toast.LENGTH_LONG
                ).show();

                refreshMap();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        refreshMap();
    }
}
