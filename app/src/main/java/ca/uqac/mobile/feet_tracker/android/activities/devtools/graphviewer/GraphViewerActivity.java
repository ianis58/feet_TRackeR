package ca.uqac.mobile.feet_tracker.android.activities.devtools.graphviewer;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;
import ca.uqac.mobile.feet_tracker.tools.MTM7Converter;

public class GraphViewerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private DatabaseReference roadGraphRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_viewer);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        roadGraphRef = database.getReference("roadgraph");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private final MetricLocation metricLocation = new MetricLocation();
    private final GeodesicLocation geodesicLocation = new GeodesicLocation();

    private void refreshMap() {
        mMap.clear();

        roadGraphRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {

                    //Bounds to ensure the map will show everything
                    final LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();

                    final HashMap<String, Object> visited = new HashMap<>();

                    final HashMap<String, GraphNode> nodes = new HashMap<>();

                    for (DataSnapshot nodeSnapshot : dataSnapshot.getChildren()) {
                        final String nodeKey = nodeSnapshot.getKey();
                        final GraphNode node = nodeSnapshot.getValue(GraphNode.class);
                        nodes.put(nodeKey, node);
                    }

                    for (Map.Entry<String, GraphNode> entry : nodes.entrySet()) {
                        final String origKey = entry.getKey();
                        final GraphNode node = entry.getValue();

                        //Convert metric coordinates to geodesic coordinate
                        metricLocation.setEast(node.getEast());
                        metricLocation.setNorth(node.getNorth());
                        MTM7Converter.metricToGeodesic(metricLocation, geodesicLocation);
                        LatLng originLatLng = new LatLng(geodesicLocation.getLatitude(), geodesicLocation.getLongitude());

                        //Add a 5 meter radius circle on each node
                        mMap.addCircle(new CircleOptions()
                                .center(originLatLng)
                                .radius(5.0)
                                .fillColor(Color.CYAN)
                                .strokeWidth(0)
                        );

                        for (Map.Entry<String, GraphRoad> roadEntry : node.getRoads().entrySet()) {
                            final String destKey = roadEntry.getKey();

                            if (!visited.containsKey(origKey + "." + destKey)) {
                                visited.put(destKey + "." + origKey, null);

                                final GraphRoad graphRoad = roadEntry.getValue();

                                final PolylineOptions polylineOptions = new PolylineOptions()
                                        .clickable(true)
                                        .geodesic(false)
                                        .color(Color.BLUE);

                                polylineOptions.add(originLatLng);
                                boundsBuilder.include(originLatLng);

                                if (nodes.containsKey(destKey)) {
                                    final GraphNode destNode = nodes.get(destKey);
                                    metricLocation.setEast(destNode.getEast());
                                    metricLocation.setNorth(destNode.getNorth());
                                }
                                else {
                                    metricLocation.setEast(metricLocation.getEast() + (graphRoad.getDirX() * graphRoad.getDistance()));
                                    metricLocation.setNorth(metricLocation.getNorth() + (graphRoad.getDirY() * graphRoad.getDistance()));
                                }

                                MTM7Converter.metricToGeodesic(metricLocation, geodesicLocation);
                                final LatLng destLatLng = new LatLng(geodesicLocation.getLatitude(), geodesicLocation.getLongitude());

                                polylineOptions.add(destLatLng);
                                boundsBuilder.include(destLatLng);

                                mMap.addPolyline(polylineOptions);
                            }
                        }
                    }

                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.graphviewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshMap();
                return true;
        }

        return false;
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
