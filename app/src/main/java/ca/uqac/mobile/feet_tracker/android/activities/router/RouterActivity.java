package ca.uqac.mobile.feet_tracker.android.activities.router;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Random;

import ca.uqac.mobile.feet_tracker.R;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationService;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationBinder;
import ca.uqac.mobile.feet_tracker.android.services.location.LocationListener;
import ca.uqac.mobile.feet_tracker.model.geo.GeodesicLocation;
import ca.uqac.mobile.feet_tracker.model.geo.MetricLocation;
import ca.uqac.mobile.feet_tracker.tools.MTM7Converter;
import ca.uqac.mobile.feet_tracker.tools.maps.DisplayPath;
import ca.uqac.mobile.feet_tracker.tools.maps.EnhancedSupportMapFragment;
import ca.uqac.mobile.feet_tracker.tools.maps.EnhancedSupportPlaceAutocompleteFragment;

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

    //UI components
    LinearLayout placesContainer;
    EnhancedSupportPlaceAutocompleteFragment placesFrom;
    EnhancedSupportPlaceAutocompleteFragment placesTo;
    EnhancedSupportMapFragment mapFragment;
    ProgressBar pbWorking;


    //Map display attributes
    MarkerOptions markerFrom = new MarkerOptions();
    MarkerOptions markerTo = new MarkerOptions();
    PolylineOptions polyLine = new PolylineOptions();
    CameraUpdate cameraUpdate;
    MarkerOptions currentPosMarkerOptions = new MarkerOptions();
    Marker currentPosMarker;
    DisplayPath displayPath;

    //Selected coordinates
    LatLng fromPos;
    LatLng toPos;

    final GeodesicLocation pathGeodesic = new GeodesicLocation();
    final MetricLocation pathMetricFrom = new MetricLocation();
    final MetricLocation pathMetricTo = new MetricLocation();

    RequestQueue pathRequestQueue;

    private void initFirstMapLocation(LatLng initalLatLng) {
        if (mLastKnownPos == null && initalLatLng != null) {
            mLastKnownPos = initalLatLng;

            fromPos = mLastKnownPos;
            placesFrom.setText(getString(R.string.router_actuel_location));

            refreshMap();
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

        //Update places search bias
        LatLngBounds bounds = new LatLngBounds.Builder().include(mLastKnownPos).build();
        if (placesFrom != null) {
            placesFrom.setBoundsBias(bounds);
        }
        if (placesTo != null) {
            placesTo.setBoundsBias(bounds);
        }


        //Update current postion marker position
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
                                pointsChanged();
                                break;
                            case R.id.router_to:
                                toPos = latLng;
                                placesTo.setLatLng(latLng);
                                pointsChanged();
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

    private void setWorkingState(boolean working) {
        if (placesFrom != null) {
            placesFrom.setEnabled(!working);
        }
        if (placesTo != null) {
            placesTo.setEnabled(!working);
        }
        if (mMap != null) {
            mMap.getUiSettings().setAllGesturesEnabled(!working);
        }
        if (pbWorking != null) {
            pbWorking.setVisibility(working ? View.VISIBLE : View.GONE);
        }
    }

    private void pointsChanged() {
        displayPath = null;
        if (fromPos != null && toPos != null) {
            fetchPath(fromPos, toPos);
        }
        refreshMap();
    }

    private void fetchPath(final LatLng from, final LatLng to) {
        //Convert LatLng coordinate to metric coordinate, since that's what findOptimalPath function use
        pathGeodesic.setLatitude(from.latitude);
        pathGeodesic.setLongitude(from.longitude);
        MTM7Converter.geodesicToMetric(pathGeodesic, pathMetricFrom);
        pathGeodesic.setLatitude(to.latitude);
        pathGeodesic.setLongitude(to.longitude);
        MTM7Converter.geodesicToMetric(pathGeodesic, pathMetricTo);

        //Construct request URL
        String baseUrl = "https://us-central1-feet-tracker.cloudfunctions.net/findOptimalPath";
        //String baseUrl = "http://192.168.1.215/feet-tracker/us-central1/findOptimalPath";
        String url = String.format(
                Locale.US,
                "%s?fromEast=%.2f&fromNorth=%.2f&toEast=%.2f&toNorth=%.2f",
                baseUrl,
                pathMetricFrom.getEast(), pathMetricFrom.getNorth(),
                pathMetricTo.getEast(), pathMetricTo.getNorth()
        );

        final JSONObject requestJSON = new JSONObject();
        try {
            requestJSON.accumulate("fromEast", pathMetricFrom.getEast());
            requestJSON.accumulate("fromNorth", pathMetricFrom.getNorth());
            requestJSON.accumulate("toEast", pathMetricTo.getEast());
            requestJSON.accumulate("toNorth", pathMetricTo.getNorth());

            /*Test data
            requestJSON.accumulate("fromEast", 252641.4829831833);
            requestJSON.accumulate("fromNorth", 5363832.887026333);
            requestJSON.accumulate("toEast", 250243.40521142152);
            requestJSON.accumulate("toNorth", 5363948.791111185);
            */
        }
        catch (JSONException e) {
            Log.e(TAG, "Error constructing requestJSON in fetchPath", e);
            Toast.makeText(this, R.string.router_error_unexpected, Toast.LENGTH_LONG).show();
            return;
        }

        //Enter "Working" state
        setWorkingState(true);

        //Do the request
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.POST,
                baseUrl,
                requestJSON,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //Construct DisplayPath
                        displayPath = new DisplayPath(response);

                        //And refresh map
                        refreshMap();

                        //Not working anymore
                        setWorkingState(false);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching path", error);

                        //Not working anymore
                        setWorkingState(false);

                        //Tell the user an error occured and ask if he wants to retry
                        new AlertDialog.Builder(RouterActivity.this)
                                .setMessage(R.string.router_error_fetch_path_title)
                                .setMessage(R.string.router_error_fetch_path_message)
                                .setCancelable(true)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        fetchPath(from, to);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Doing nothing more
                                    }
                                })
                                .create()
                                .show();
                    }
                }
        );
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                60000, //Allow up to a 3  secs before timing out
                0,  //Do not retry by itself, let the user decide
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Add download to request queue
        pathRequestQueue.add(jsObjRequest);
    }

    private void refreshMap() {
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
            if (displayPath != null) {
                LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
                displayPath.includeInBounds(boundsBuilder);
                bounds = boundsBuilder.build();

                for (DisplayPath.Segment segment : displayPath.getSegments()) {
                    mMap.addPolyline(segment.getPolyline());
                }
            }
            else if (fromPos != null && toPos != null) {
                LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
                bounds = boundsBuilder
                        .include(fromPos)
                        .include(toPos)
                        .build()
                ;
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

    private void findUIComponents() {
        mapFragment = (EnhancedSupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        placesContainer = (LinearLayout) findViewById(R.id.router_places);
        placesFrom = (EnhancedSupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.places_fragment_from);
        placesTo = (EnhancedSupportPlaceAutocompleteFragment) getSupportFragmentManager().findFragmentById(R.id.places_fragment_to);

        pbWorking = (ProgressBar) findViewById(R.id.working);
    }

    private void initUIComponents() {
        mapFragment.getMapAsync(this);
        placesFrom.setHint(getResources().getString(R.string.router_from_hint));
        placesFrom.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                fromPos = place.getLatLng();
                pointsChanged();
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
                pointsChanged();
            }
        });


        placesTo.setHint(getResources().getString(R.string.router_to_hint));
        placesTo.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                toPos = place.getLatLng();
                pointsChanged();
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
                pointsChanged();
            }
        });
    }

    private void updateOrientation(int orientation) {
        if (placesContainer != null) {
            /*getResources().getConfiguration().orientation*/
            placesContainer.setOrientation(
                    orientation == Configuration.ORIENTATION_PORTRAIT
                            ? LinearLayout.VERTICAL
                            : LinearLayout.HORIZONTAL
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        findUIComponents();
        initUIComponents();

        pathRequestQueue = Volley.newRequestQueue(this);

        mMap = null;
        fromPos = null;
        toPos = null;

        initializeTracker();

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


        updateOrientation(getResources().getConfiguration().orientation);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updateOrientation(newConfig.orientation);
        super.onConfigurationChanged(newConfig);
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
