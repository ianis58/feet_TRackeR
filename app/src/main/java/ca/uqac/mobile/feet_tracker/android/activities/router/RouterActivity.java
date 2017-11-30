package ca.uqac.mobile.feet_tracker.android.activities.router;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Random;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationService;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationBinder;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationListener;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;

public class RouterActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = RouterActivity.class.getSimpleName();

    private static final float SINGLE_POINT_ZOOM = 13.0f;
    private static final float REFRESH_LOCATION_MARKER_INTERVAL_SECS = 5.0f;

    //LocationTracker service attributes
    LocationService locationService;
    LocationListener locationListener;
    ServiceConnection locationServiceConnection;

    //Google Map API attributes
    private GoogleMap mMap;

    private LatLng mLastKnownPos = null;

    //Google Places API attributes
    private GoogleApiClient mClient;

    //UI attributes
    SupportMapFragment mapFragment;
    EnhancedSupportPlaceAutocompleteFragment placesFrom;
    EnhancedSupportPlaceAutocompleteFragment placesTo;

    //Trajectory attributes
    MarkerOptions markerFrom = new MarkerOptions();
    MarkerOptions markerTo = new MarkerOptions();
    PolylineOptions polyLine = new PolylineOptions();
    CameraUpdate cameraUpdate;

    LatLng fromPos;
    LatLng toPos;

    MarkerOptions currentPosMarkerOptions = new MarkerOptions();
    Marker currentPosMarker;

    private void initFirstMapLocation(LatLng initalLatLng) {
        if (mLastKnownPos == null && initalLatLng != null) {
            mLastKnownPos = initalLatLng;

            fromPos = mLastKnownPos;
            placesFrom.setText(getString(R.string.router_actuel_location));

            refreshMap();

            LatLngBounds bounds = new LatLngBounds.Builder().include(mLastKnownPos).build();

            placesFrom.setBoundsBias(bounds);
            placesTo.setBoundsBias(bounds);
        }
        else {
            mLastKnownPos = initalLatLng;
        }
    }

    private void initializeTracker() {
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(GeodesicLocation geodesicLocation, MetricLocation metricLocation, long millisElapsed) {
                updateTrackerPos(geodesicLocation);
            }
        };

        Intent locationServiceIntent = new Intent(this, LocationService.class);
        locationServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                final LocationBinder binder;
                if (service instanceof LocationBinder) {
                    binder = (LocationBinder) service;
                }
                else {
                    binder = null;
                }

                if (binder != null) {
                    locationService = binder.getService();

                    if (locationService != null) {
                        locationService.registerListener(locationListener, REFRESH_LOCATION_MARKER_INTERVAL_SECS, true);

                        //initFirstMapLocation(locationService.getGeodesicLocation());
                    }
                    else {
                        Log.e(TAG, getString(R.string.router_err_binder_getservice));
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                locationService = null;
                //Reconnect (and recreate service)
                initializeTracker();
            }
        };

        bindService(locationServiceIntent, locationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void updateTrackerPos(GeodesicLocation geodesicLocation) {
        LatLng currentLatLng = new LatLng(geodesicLocation.getLatitude(), geodesicLocation.getLongitude());

        initFirstMapLocation(currentLatLng);

        if (currentPosMarker != null) {
            currentPosMarker.setPosition(currentLatLng);
        }
    }

    private void initializeMap() {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(final LatLng latLng) {
                //Create popup menu to select either From or To
                PopupMenu popupMenu = new PopupMenu(RouterActivity.this, mapFragment.getView());
                MenuInflater menuInflater = popupMenu.getMenuInflater();
                menuInflater.inflate(R.menu.router_from_to, popupMenu.getMenu());

                //Définir son événement de click sur un item
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        switch (id) {
                            case R.id.router_from:
                                fromPos = latLng;
                                placesFrom.setLatLng(latLng);
                                refreshMap();
                                break;
                            case R.id.router_to:
                                toPos = latLng;
                                placesTo.setLatLng(latLng);
                                refreshMap();
                                break;
                        }
                        return false;
                    }
                });

                //Afficher le popup
                popupMenu.show();
            }
        });
        refreshMap();
    }

    private LatLng getPseudoRandomLatLngAlongPath(double pct, double randomPct) {
        final double deltaLon = toPos.longitude - fromPos.longitude;
        final double deltaLat = toPos.latitude - fromPos.latitude;
        final double delta = Math.sqrt(deltaLon*deltaLon+deltaLat*deltaLat);

        final double preciseLon = fromPos.longitude + deltaLon * pct;
        final double preciseLat = fromPos.latitude + deltaLat * pct;

        final Random random = new Random();
        final double randomDeltaLon = delta * randomPct;
        final double randomDeltaLat = delta * randomPct;
        final double randomLon = preciseLon + (random.nextDouble()-0.5) * (2*randomDeltaLon);
        final double randomLat = preciseLat + (random.nextDouble()-0.5) * (2*randomDeltaLat);

        return new LatLng(randomLat, randomLon);
    }

    private void refreshMap() {
        /*
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        */

        if (mMap != null) {
            mMap.clear();

            if (mLastKnownPos != null) {
                currentPosMarkerOptions
                        .alpha(0.8f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                        .title(getString(R.string.router_current_pos))
                        .position(mLastKnownPos)
                ;
                currentPosMarker = mMap.addMarker(currentPosMarkerOptions);
            }

            if (fromPos != null) {
                markerFrom.position(fromPos);
                markerFrom.title(getResources().getString(R.string.router_from_hint));
                markerFrom.alpha(0.8f);
                mMap.addMarker(markerFrom);
            }
            if (toPos != null) {
                markerTo.position(toPos);
                markerTo.title(getResources().getString(R.string.router_to_hint));
                markerTo.alpha(0.8f);
                mMap.addMarker(markerTo);
            }

            LatLngBounds bounds = null;
            if (fromPos != null && toPos != null) {
                polyLine = new PolylineOptions();
                polyLine
                        .add(fromPos)
                        .color(Color.BLUE)
                        .geodesic(false)
                        .clickable(false)
                ;

                LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
                boundsBuilder
                        .include(fromPos)
                        .include(toPos)
                ;

                //Add a few pseudo random points
                final int segmentCount = 4;
                final double segmentWidth = 1.0 / (segmentCount);
                for (int i = 1; i < segmentCount; i++) {
                    LatLng latLng = getPseudoRandomLatLngAlongPath(i * segmentWidth, segmentWidth/2);

                    polyLine.add(latLng);
                    boundsBuilder.include(latLng);
                }

                //Add target path
                polyLine.add(toPos);
                mMap.addPolyline(polyLine);

                bounds = boundsBuilder.build();
            }

            if (fromPos != null && toPos != null) {
                cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50);
            }
            else if (fromPos != null) {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(fromPos, SINGLE_POINT_ZOOM);
            }
            else if (toPos != null) {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(toPos, SINGLE_POINT_ZOOM);
            }
            else if (mLastKnownPos != null) {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(mLastKnownPos, SINGLE_POINT_ZOOM);
            }
            else {
                cameraUpdate = null;
            }

            if (cameraUpdate != null) {
                mMap.moveCamera(cameraUpdate);
            }

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        final LinearLayout placesContainer = (LinearLayout) findViewById(R.id.router_places);
        if (placesContainer != null) {
            placesContainer.setOrientation(
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                        ? LinearLayout.VERTICAL
                        : LinearLayout.HORIZONTAL
            );
        }

        mMap = null;
        fromPos = null;
        toPos = null;

        initializeTracker();

        //Google map initialization
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        //Google Places initialization
        GoogleApiClient.OnConnectionFailedListener placesFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                //Not much for now...
            }
        };
        mClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, placesFailedListener)
                .build();

        placesFrom = (EnhancedSupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.places_fragment_from);
        placesFrom.setHint(getResources().getString(R.string.router_from_hint));
        placesFrom.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                fromPos = place.getLatLng();
                refreshMap();
            }

            @Override
            public void onError(Status status) {
                fromPos = null;
            }
        });
        placesFrom.setOnClearListener(new EnhancedSupportPlaceAutocompleteFragment.OnClearListener() {
            @Override
            public void onClear() {
                fromPos = null;
                refreshMap();
            }
        });


        placesTo = (EnhancedSupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.places_fragment_to);
        placesTo.setHint(getResources().getString(R.string.router_to_hint));
        placesTo.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                toPos = place.getLatLng();
                refreshMap();
            }

            @Override
            public void onError(Status status) {
                toPos = null;
            }
        });
        placesTo.setOnClearListener(new EnhancedSupportPlaceAutocompleteFragment.OnClearListener() {
            @Override
            public void onClear() {
                toPos = null;
                refreshMap();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        if (locationService != null) {
            locationService.registerListener(locationListener, REFRESH_LOCATION_MARKER_INTERVAL_SECS, true);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (locationService != null) {
            locationService.unregisterListener(locationListener);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (locationServiceConnection != null) {
            unbindService(locationServiceConnection);
            locationServiceConnection = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
        return true;
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
        initializeMap();
    }
}
