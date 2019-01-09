package mega.privacy.android.app.lollipop.megachat;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.SphericalUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MapsAdapter;
import mega.privacy.android.app.utils.Util;

@SuppressLint("MissingPermission")
public class MapsActivity extends PinActivityLollipop implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener,
        View.OnClickListener, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, LocationListener {

    private final int DEFAULT_RADIUS = 50000;
    private final float DEFAULT_ZOOM = 18f;

    private DisplayMetrics outMetrics;
    private Toolbar tB;
    private ActionBar aB;

    private ProgressBar progressBar;
    private GoogleMap mMap;
    private RelativeLayout mapLayout;
    private View shadowLayout;
    private RelativeLayout listLayout;
    private FloatingActionButton setFullScreenFab;
    private FloatingActionButton myLocationFab;
    private ImageView fullscreenMarkerIcon;
    private ImageView fullscreenMarkerIconShadow;
    private TextView noPlacesFound;
    private SupportMapFragment mapFragment;
    private RecyclerView listAddresses;
    private LinearLayoutManager layoutManager;
    private MapsAdapter adapter;

    private LocationManager locationManager;
    private GeoDataClient geoDataClient;
    private PlaceDetectionClient placeDetectionClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    Geocoder geocoder;
    List<Address> addresses;
    ArrayList<MapAddress> arrayListAddresses = new ArrayList<>();
    LatLng myLocation = null;

    SearchView searchView;
    MenuItem searchMenuItem;
    MenuItem refreshMenuItem;

    private boolean isFullScreenEnabled = false;
    private Bitmap iconMarker;
    private Bitmap fullscreenIconMarker;
    private Marker fullScreenMarker;
    private ArrayList<Marker> placesMarker;
    private int numMarkers = 0;
    private String search;

    boolean isGPSEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        if (savedInstanceState != null) {
            isFullScreenEnabled = savedInstanceState.getBoolean("isFullScreenEnabled", false);
            search = savedInstanceState.getString("search", "");
        }
        else {
            isFullScreenEnabled = false;
            search = "";
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        tB = (Toolbar) findViewById(R.id.toolbar_maps);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(getString(R.string.title_activity_maps));

        ((ViewGroup) findViewById(R.id.parent_layout_maps)).getLayoutTransition().setDuration(500);
        ((ViewGroup) findViewById(R.id.parent_layout_maps)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        progressBar = (ProgressBar) findViewById(R.id.progressbar_maps);
        progressBar.setVisibility(View.VISIBLE);
        mapLayout = (RelativeLayout) findViewById(R.id.map_layout);
        shadowLayout = (View) findViewById(R.id.shadow_maps_layout);
        listLayout = (RelativeLayout) findViewById(R.id.list_layout);
        fullscreenMarkerIcon = (ImageView) findViewById(R.id.fullscreen_marker_icon);
        fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
        fullscreenMarkerIconShadow = (ImageView) findViewById(R.id.fullscreen_marker_icon_shadow);
        fullscreenMarkerIconShadow.setVisibility(View.GONE);
        setFullScreenFab = (FloatingActionButton) findViewById(R.id.set_fullscreen_fab);
        setFullScreenFab.setOnClickListener(this);
        setFullScreenFab.setVisibility(View.GONE);
        myLocationFab = (FloatingActionButton) findViewById(R.id.my_location_fab);
        Drawable myLocationFabDrawable = (ContextCompat.getDrawable(this, R.drawable.ic_small_location));
        myLocationFabDrawable.setAlpha(143);
        myLocationFab.setImageDrawable(myLocationFabDrawable);
        myLocationFab.setOnClickListener(this);
        myLocationFab.setVisibility(View.GONE);
        noPlacesFound = (TextView) findViewById(R.id.no_places_found_label);
        noPlacesFound.setVisibility(View.GONE);
        listAddresses = (RecyclerView) findViewById(R.id.address_list);
        listAddresses.setClipToPadding(false);
        setLayoutManager(true);
        listAddresses.setHasFixedSize(true);
        listAddresses.setItemAnimator(new DefaultItemAnimator());
        adapter = new MapsAdapter(this, arrayListAddresses);
        listAddresses.setAdapter(adapter);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            setMapParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.px2dp(204, outMetrics));
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.px2dp(4, outMetrics));
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, mapLayout.getId());
            shadowLayout.setLayoutParams(params);
            shadowLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.maps_shadow_portrait));
            setListParams();
        }
        else {
            setMapParams(Util.px2dp(300, outMetrics), ViewGroup.LayoutParams.MATCH_PARENT);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Util.px2dp(4, outMetrics), ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, mapLayout.getId());
            shadowLayout.setLayoutParams(params);
            shadowLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.maps_shadow_landscape));
            setListParams();
        }

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        geoDataClient = Places.getGeoDataClient(this);
        placeDetectionClient = Places.getPlaceDetectionClient(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        iconMarker = drawableBitmap(getDrawable(R.drawable.ic_map_marker));
    }

    void setMapParams (int width, int height) {
        if (mapLayout == null) {
            mapLayout = (RelativeLayout) findViewById(R.id.map_layout);
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.addRule(RelativeLayout.BELOW, tB.getId());
        mapLayout.setLayoutParams(params);
    }

    void setListParams () {
        if (listLayout == null) {
            listLayout = (RelativeLayout) findViewById(R.id.list_layout);
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.addRule(RelativeLayout.BELOW, mapLayout.getId());
        }
        else {
            params.addRule(RelativeLayout.BELOW, tB.getId());
            params.addRule(RelativeLayout.RIGHT_OF, mapLayout.getId());
        }
        listLayout.setLayoutParams(params);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isFullScreenEnabled", isFullScreenEnabled);
        outState.putString("search", search);
    }

    void setLocationFabDrawable () {
        if (setFullScreenFab == null) {
            setFullScreenFab = findViewById(R.id.set_fullscreen_fab);
        }

        Drawable setFullScreenFabDrawable;
        if (isFullScreenEnabled) {
            setFullScreenFabDrawable = (ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_exit_location));
        }
        else {
            setFullScreenFabDrawable = (ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_location));
        }
        setFullScreenFabDrawable.setAlpha(143);
        setFullScreenFab.setImageDrawable(setFullScreenFabDrawable);
        setFullScreenFab.setOnClickListener(this);
    }

    boolean isGPSEnabled () {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        log("onMapReady");
        mMap = googleMap;

        isGPSEnabled = isGPSEnabled();

        if (!isGPSEnabled) {
            showEnableLocationDialog();
        }
        else {
            initMap();
        }
    }

    void initMap() {

        if (mMap == null) {
            return;
        }

        isGPSEnabled = isGPSEnabled();

        if (isGPSEnabled) {
            if (myLocationFab.getVisibility() != View.VISIBLE) {
                myLocationFab.setVisibility(View.VISIBLE);
            }
            if (setFullScreenFab.getVisibility() != View.VISIBLE) {
                setFullScreenFab.setVisibility(View.VISIBLE);
            }
            mMap.setMyLocationEnabled(true);
        }
        else{
            if (myLocationFab.getVisibility() != View.GONE) {
                myLocationFab.setVisibility(View.GONE);
            }
            if (setFullScreenFab.getVisibility() != View.GONE) {
                setFullScreenFab.setVisibility(View.GONE);
            }
            isFullScreenEnabled = true;
            mMap.setMyLocationEnabled(false);
        }

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        if (isGPSEnabled && (search == null || search.isEmpty())) {
            setMyLocation(true, false);
        }
        setFullScreen();
    }

    private void showEnableLocationDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gps_disabled)
                .setMessage(R.string.open_location_settings)
                .setCancelable(false)
                .setPositiveButton(R.string.cam_sync_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        isFullScreenEnabled = true;
                        initMap();
                        dialog.cancel();
                        if (progressBar.getVisibility() != View.GONE) {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void findNearbyPlaces () {
        if (mMap == null) {
            return;
        }
        clearPlaces();
        placeDetectionClient.getCurrentPlace(null).addOnSuccessListener(this, new OnSuccessListener<PlaceLikelihoodBufferResponse>() {
                @Override
                public void onSuccess(PlaceLikelihoodBufferResponse placeLikelihoods) {
                int i = 0;
                if (iconMarker == null) {
                    iconMarker = drawableBitmap(getDrawable(R.drawable.ic_map_marker));
                }
                Marker marker;
                for (PlaceLikelihood placeLikelihood : placeLikelihoods) {
                    i++;
                    marker = mMap.addMarker(new MarkerOptions().position(placeLikelihood.getPlace().getLatLng()).icon(BitmapDescriptorFactory.fromBitmap(iconMarker)));
                    placesMarker.add(marker);
                    arrayListAddresses.add(i, new MapAddress(placeLikelihood.getPlace()));
                }
                setAdapterAddresses();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setMyLocation(final boolean animateCamera, final boolean searchMode) {
        log("setMyLocation");
        if (mMap == null) {
            return;
        }

        if (arrayListAddresses == null) {
            arrayListAddresses = new ArrayList<>();
        }

        fusedLocationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                log("getLastLocation() onFailure: "+e.getMessage());
            }
        }).addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    log("getLastLocation() onSuccess");
                    if (!isFullScreenEnabled) {
                        addresses = getAddresses(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null) {
                            if (!arrayListAddresses.isEmpty()) {
                                arrayListAddresses.set(0, new MapAddress(new LatLng(location.getLatitude(), location.getLongitude()), getString(R.string.current_location_label), addresses.get(0).getAddressLine(0)));
                            }
                            else {
                                arrayListAddresses.add(0, new MapAddress(new LatLng(location.getLatitude(), location.getLongitude()), getString(R.string.current_location_label), addresses.get(0).getAddressLine(0)));
                            }
                            findNearbyPlaces();
                        }
                    }
                    myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (animateCamera) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
                    }
                    else if (searchMode) {
                        findSearchPlaces();
                    }
                }
                setAdapterAddresses();
            }
        });
    }

    Bitmap drawableBitmap (Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public void itemClick (int position) {
        if (arrayListAddresses != null && !arrayListAddresses.isEmpty() && position < arrayListAddresses.size()) {
            setActivityResult(position, arrayListAddresses.get(position));
        }
    }

    private void clearPlaces () {
        if (placesMarker == null)  {
            placesMarker = new ArrayList<>();
        }
        else if (!placesMarker.isEmpty()) {
            for (Marker marker : placesMarker) {
                marker.remove();
            }
            placesMarker.clear();
        }
    }

    void setActivityResult (final int position, MapAddress location) {
        if (mMap == null) {
            return;
        }
        if (fullScreenMarker != null) {
            fullScreenMarker.remove();
        }
        clearPlaces();

        final String name = location.getName();
        final String address = location.getAddress();
        final Double latitude = location.getLatLng().latitude;
        final Double longitude = location.getLatLng().longitude;

        mMap.moveCamera(CameraUpdateFactory.newLatLng(location.getLatLng()));
        mMap.addMarker(new MarkerOptions().position(location.getLatLng()));
        mMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                Intent intent = new Intent();
//                (Like Whatsapp, on desgins only latitude and longitude)
//                Fullscreen mode --> send only direction
//                Click on place --> send name and direction
//                Click on send current location --> send nothing
//                Click on search location --> send name and direction if available
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                intent.putExtra("snapshot", byteArray);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                if (position == 0) {
                    if (isFullScreenEnabled) {
                        intent.putExtra("name", address);
                        intent.putExtra("address", address);
                    }
                    else {
                        intent.putExtra("name", "");
                        intent.putExtra("address", "");
                    }
                }
                else {
                    intent.putExtra("name", name);
                    intent.putExtra("address", address);
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    void setAdapterAddresses () {
        if (adapter == null){
            adapter = new MapsAdapter(this, arrayListAddresses);
        }
        else{
            adapter.setAddresses(arrayListAddresses);
        }
        layoutManager.scrollToPosition(0);
    }

    List<Address> getAddresses (double latitude, double longitude, int maxResults) {
        if (geocoder == null) {
            geocoder = new Geocoder(this, Locale.getDefault());
        }
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, maxResults);
            if (addresses != null) {
                return addresses;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home :{
              onBackPressed();
              break;
            }
            case R.id.action_refresh: {
                if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
                    searchMenuItem.collapseActionView();
                }
                search = "";
                if (isFullScreenEnabled) {
                    isFullScreenEnabled = false;
                    setFullScreen();
                }
                setMyLocation(true, false);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_maps, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = (MenuItem) menu.findItem(R.id.action_search);
        searchMenuItem.setIcon(Util.mutateIconSecondary(this, R.drawable.ic_menu_search, R.color.white));
        searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setIconifiedByDefault(true);

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                log("onMenuItemActionExpand");
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                log("onMenuItemActionCollapse");
                return true;
            }
        });
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                onMapSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                log("onQueryTextChange searchView");
                return true;
            }

        });

        if (search != null && !search.isEmpty()) {
            searchView.setQuery(search, true);
        }

        refreshMenuItem = (MenuItem) menu.findItem(R.id.action_refresh);

        if (isGPSEnabled()) {
            refreshMenuItem.setVisible(true);
            searchMenuItem.setVisible(true);
        }
        else {
            refreshMenuItem.setVisible(false);
            searchMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    void onMapSearch(String search) {
        log("onMapSearch");
        if (search != null && !search.equals("")) {
            this.search = search;
            noPlacesFound.setVisibility(View.GONE);
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            View view = getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            if (iconMarker == null) {
                iconMarker = drawableBitmap(getDrawable(R.drawable.ic_map_marker));
            }
            if (arrayListAddresses != null) {
                arrayListAddresses.clear();
            }
            clearPlaces();
            setMyLocation(false, true);
        }
    }

    void findSearchPlaces () {
        double distanceFromCenterToCorner = DEFAULT_RADIUS * Math.sqrt(2);
        LatLng southwestCorner = SphericalUtil.computeOffset(myLocation, distanceFromCenterToCorner, 225.0);
        LatLng northeastCorner = SphericalUtil.computeOffset(myLocation, distanceFromCenterToCorner, 45.0);
        final LatLngBounds latLngBounds = new LatLngBounds(southwestCorner, northeastCorner);
        final LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(myLocation);
        clearPlaces();
        geoDataClient.getAutocompletePredictions(search, latLngBounds, GeoDataClient.BoundsMode.STRICT, null).addOnSuccessListener(new OnSuccessListener<AutocompletePredictionBufferResponse>() {
            @Override
            public void onSuccess(AutocompletePredictionBufferResponse autocompletePredictions) {
                final int size = autocompletePredictions.getCount();
                if (size == 0) {
                    noPlacesFound.setVisibility(View.VISIBLE);
                }
                else {
                    for (AutocompletePrediction autocompletePrediction : autocompletePredictions) {
                        geoDataClient.getPlaceById(autocompletePrediction.getPlaceId()).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                            @Override
                            public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                                numMarkers++;
                                Place place = task.getResult().get(0);
                                if (place != null) {
                                    arrayListAddresses.add(new MapAddress(place));
                                    Marker marker = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).icon(BitmapDescriptorFactory.fromBitmap(iconMarker)));
                                    if (marker != null) {
                                        placesMarker.add(marker);
                                        builder.include(place.getLatLng());
                                    }
                                }
                                if (numMarkers == size) {
                                    numMarkers = 0;
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 110));
                                    setAdapterAddresses();
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        log("onMyLocationButtonClick");
        return false;
    }

    @Override
    public void onMyLocationClick(Location location) {
        log("onMyLocationClick");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    void setLayoutManager (boolean scrollEnabled) {
        if (scrollEnabled) {
            layoutManager = new LinearLayoutManager(this);
        }
        else {
            listAddresses.scrollToPosition(0);
            layoutManager = new LinearLayoutManager(this) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            };
        }
        listAddresses.setLayoutManager(layoutManager);
    }

    void setFullScreenLayouts () {
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (isFullScreenEnabled) {
                setLayoutManager(false);
                Rect rect = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);

                int heigth;
                if (isGPSEnabled) {
                    heigth = outMetrics.heightPixels - rect.top - tB.getHeight() - Util.px2dp(72, outMetrics);
                }
                else {
                    heigth = outMetrics.heightPixels - rect.top - tB.getHeight();
                }
                setMapParams(ViewGroup.LayoutParams.MATCH_PARENT, heigth);
            }
            else {
                setLayoutManager(true);
                setMapParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.px2dp(204, outMetrics));
            }
        }
        else {
            if (isFullScreenEnabled) {
                setLayoutManager(false);
                setMapParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            else {
                setLayoutManager(true);
                setMapParams(Util.px2dp(300, outMetrics), ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
    }

    void getMarkerInfo () {
        if (mMap == null) {
            return;
        }

        LatLng latLng = mMap.getCameraPosition().target;
        if (latLng != null) {
            addresses = getAddresses(latLng.latitude, latLng.longitude, 1);
            String title = getString(R.string.title_marker_maps);
            if (fullscreenIconMarker == null) {
                fullscreenIconMarker = drawableBitmap(Util.mutateIconSecondary(this, R.drawable.ic_send_location, R.color.dark_primary_color_secondary));
            }
            if (addresses != null && addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0);
                if (fullScreenMarker == null) {
                    fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(address).icon(BitmapDescriptorFactory.fromBitmap(fullscreenIconMarker)));
                    fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
                }
                else {
                    fullScreenMarker.setPosition(latLng);
                    fullScreenMarker.setSnippet(address);
                    fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
                }
                if (!fullScreenMarker.isVisible()) {
                    if (fullscreenMarkerIcon.getVisibility() == View.VISIBLE) {
                        fullscreenMarkerIcon.animate().translationY(0).setDuration(100L).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
                                fullScreenMarker.setVisible(true);
                                fullScreenMarker.showInfoWindow();
                            }
                        }).start();
                    }
                    else {
                        fullScreenMarker.setVisible(true);
                    }
                }
                fullScreenMarker.showInfoWindow();
                if (adapter != null) {
                    MapAddress marker = (MapAddress) adapter.getItem(0);
                    if (marker != null) {
                        marker.setName(title);
                        marker.setAddress(address);
                        adapter.notifyItemChanged(0);
                    }
                }
                else {
                    arrayListAddresses.add(0, new MapAddress(latLng, title, addresses.get(0).getAddressLine(0)));
                    adapter = new MapsAdapter(this, arrayListAddresses);
                }
            }
            else {
                if (fullScreenMarker == null) {
                    fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet("").icon(BitmapDescriptorFactory.fromBitmap(fullscreenIconMarker)));
                    fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
                }
                else {
                    fullScreenMarker.setPosition(latLng);
                    fullScreenMarker.setSnippet("");
                    fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
                }
                setAnimatingMarker(0);

                if (adapter != null) {
                    MapAddress marker = (MapAddress) adapter.getItem(0);
                    if (marker != null) {
                        marker.setName(title);
                        marker.setAddress("");
                        adapter.notifyItemChanged(0);
                    }
                }
                else {
                    arrayListAddresses.add(0, new MapAddress(latLng, title, addresses.get(0).getAddressLine(0)));
                    adapter = new MapsAdapter(this, arrayListAddresses);
                }
            }
        }
    }

    void setFullScreen() {
        setLocationFabDrawable();
        setFullScreenLayouts();

        if (mMap == null) {
            return;
        }

        if (isFullScreenEnabled) {
            getMarkerInfo();
        }
        else {
            fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
            fullscreenMarkerIconShadow.setVisibility(View.GONE);
            setMyLocation(false, false);

            try {
                fullScreenMarker.remove();
            } catch (Exception e) {}

            fullScreenMarker = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.my_location_fab: {
                setMyLocation(true, false);
                break;
            }
            case R.id.set_fullscreen_fab: {
                isFullScreenEnabled = !isFullScreenEnabled;
                setFullScreen();
                break;
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (isFullScreenEnabled && marker.equals(fullScreenMarker)) {
            itemClick(0);
            return true;
        }

        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        log("onInfoWindowClick");
        if (isFullScreenEnabled && marker.equals(fullScreenMarker)) {
            itemClick(0);
        }
    }

    @Override
    public void onCameraIdle() {
        if (isFullScreenEnabled && fullScreenMarker != null) {
            getMarkerInfo();
        }
    }

    void setAnimatingMarker (long duration) {
        if (isFullScreenEnabled && fullScreenMarker != null) {
            fullScreenMarker.setVisible(false);
            fullscreenMarkerIcon.setVisibility(View.VISIBLE);
            fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            fullscreenMarkerIcon.animate().translationY(-Util.px2dp(12 ,outMetrics)).setDuration(duration).start();
        }
    }

    @Override
    public void onCameraMoveStarted(int i) {
        setAnimatingMarker(100L);
    }


    @Override
    public void onLocationChanged(Location location) {
        log("LocationListener onLocationChanged");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        log("LocationListener onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
        log("LocationListener onProviderEnabled");
        if (!isGPSEnabled) {
            supportInvalidateOptionsMenu();
            if (mMap == null) {
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            mMap.clear();
            mMap.setMyLocationEnabled(true);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    isFullScreenEnabled = false;
                    initMap();
                }
            }, 3000);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        log("LocationListener onProviderDisabled");
        if (isGPSEnabled) {
            supportInvalidateOptionsMenu();

            if (mMap == null) {
                return;
            }
            arrayListAddresses.clear();
            mMap.clear();
            initMap();
        }
    }

    public static void log(String message) {
        Util.log("MapsActivity", message);
    }
}
