package ca.uqac.mobile.feet_tracker.android.activities.router;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Random;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.services.locationtracker.LocationTrackerBinder;
import ca.uqac.mobile.feet_tracker.android.services.locationtracker.LocationTrackerListener;
import ca.uqac.mobile.feet_tracker.android.services.locationtracker.LocationTrackerService;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;

public class RouterActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = RouterActivity.class.getSimpleName();

    private static final float SINGLE_POINT_ZOOM = 13.0f;

    //LocationTracker service attributes
    LocationTrackerService locationTracker;
    LocationTrackerListener locationTrackerListener;

    //Google Map API attributes
    private GoogleMap mMap;

    private LatLng mInitialMapPos = null;

    //Google Places API attributes
    private GoogleApiClient mClient;

    //UI attributes
    SupportMapFragment mapFragment;
    SupportPlaceAutocompleteFragment placesFrom;
    SupportPlaceAutocompleteFragment placesTo;

    //Trajectory attributes
    MarkerOptions markerFrom = new MarkerOptions();
    MarkerOptions markerTo = new MarkerOptions();
    PolylineOptions polyLine = new PolylineOptions();
    CameraUpdate cameraUpdate;

    LatLng fromPos;
    LatLng toPos;

    private void initFirstMapLocation(GeodesicLocation geodesicLocation) {
        if (mInitialMapPos == null && geodesicLocation != null) {
            mInitialMapPos = new LatLng(geodesicLocation.getLatitude(), geodesicLocation.getLongitude());

            fromPos = mInitialMapPos;
            placesFrom.setText(getString(R.string.router_actuel_location));

            refreshMap();

            LatLngBounds bounds = new LatLngBounds.Builder().include(mInitialMapPos).build();

            placesFrom.setBoundsBias(bounds);
            placesTo.setBoundsBias(bounds);
        }
    }

    private void initializeTracker() {
        locationTrackerListener = new LocationTrackerListener() {
            @Override
            public void onLocationChanged(GeodesicLocation geodesicLocation) {
                updateTrackerPos(geodesicLocation);
            }
        };

        Intent locationTrackerIntent = new Intent(this, LocationTrackerService.class);
        bindService(locationTrackerIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationTrackerBinder binder = (LocationTrackerBinder) service;

                if (binder != null) {
                    locationTracker = binder.getService();

                    if (locationTracker != null) {
                        locationTracker.registerListener(locationTrackerListener);

                        initFirstMapLocation(locationTracker.getGeodesicLocation());
                    }
                    else {
                        Log.e(TAG, getString(R.string.router_err_binder_getservice));
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                locationTracker = null;
                //Reconnect
                initializeTracker();
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private void updateTrackerPos(GeodesicLocation geodesicLocation) {
        initFirstMapLocation(geodesicLocation);
        //TODO: déplacer le marqueur de la position actuelle
    }

    private void initializeMap() {
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

            if (fromPos != null) {
                markerFrom.position(fromPos);
                markerFrom.title(getResources().getString(R.string.router_from_hint));
                mMap.addMarker(markerFrom);
            }
            if (toPos != null) {
                markerTo.position(toPos);
                markerTo.title(getResources().getString(R.string.router_to_hint));
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
            else if (mInitialMapPos != null) {
                cameraUpdate = CameraUpdateFactory.newLatLngZoom(mInitialMapPos, SINGLE_POINT_ZOOM);
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

        mMap = null;
        fromPos = null;
        toPos = null;

        initializeTracker();

        //Google map initialization
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
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

        placesFrom = (SupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.places_fragment_from);
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


        placesTo = (SupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.places_fragment_to);
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
    }

    @Override
    protected void onResume() {
        if (locationTracker != null) {
            locationTracker.registerListener(locationTrackerListener);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (locationTracker != null) {
            locationTracker.unregisterListener(locationTrackerListener);
        }
        super.onPause();
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
