package mega.privacy.android.app.lollipop.megachat;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.SphericalUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.utils.Util;

@SuppressLint("MissingPermission")
public class MapsActivity extends PinActivityLollipop implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener, View.OnClickListener, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, LocationListener {

    public static final String SNAPSHOT = "snapshot";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String EDITING_MESSAGE = "editingMessage";
    public static final String MSG_ID = "msg_id";

    private final String IS_FULL_SCREEN_ENABLED = "isFullScreenEnabled";

    public static final float DEFAULT_ZOOM = 18f;
    private static Geocoder geocoder;
    public final int SNAPSHOT_SIZE = 750;
    private final int MAX_SIZE = 45000;
    private DisplayMetrics outMetrics;
    private Toolbar tB;
    private ActionBar aB;
    private ProgressBar progressBar;
    private GoogleMap mMap;
    private MapView mapView;
    private RelativeLayout mapLayout;
    private RelativeLayout sendCurrentLocationLayout;
    private RelativeLayout sendCurrentLocationLandscapeLayout;
    private TextView currentLocationName;
    private TextView currentLocationLandscape;
    private TextView currentLocationAddres;
    private FloatingActionButton setFullScreenFab;
    private FloatingActionButton myLocationFab;
    private ImageView fullscreenMarkerIcon;
    private ImageView fullscreenMarkerIconShadow;
    private SupportMapFragment mapFragment;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private List<Address> addresses;
    private MapAddress currentAddress;
    private LatLng myLocation = null;
    private boolean isFullScreenEnabled = false;
    private Bitmap fullscreenIconMarker;
    private Marker fullScreenMarker;
    private MapAddress fullScreenAddress;

    /**
     * This method gets an address from the coordinates passed like parameters
     *
     * @param context   from which the action is done
     * @param latitude  is the latitude of a coordinate
     * @param longitude is the longitude of a coordinate
     * @return
     */
    public static List<Address> getAddresses(Context context, double latitude, double longitude) {
        if (geocoder == null) {
            geocoder = new Geocoder(context, Locale.getDefault());
        }

        try {
            return geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            log("Exception trying to get an address from a latitude and a longitude");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method obtains an area circle represented by an LatLngBounds object,
     * which center is represented by some coordinates and his distance from the center
     * represented by an integer received as parameters
     *
     * @param radius distance from the center of the circle in metres
     * @param latLng latitude and longitude that will be the center of the circle
     * @return the circle represented by a LatLngBounds object
     */
    private LatLngBounds getLatLngBounds(int radius, LatLng latLng) {
        double distanceFromCenterToCorner = radius * Math.sqrt(2);
        LatLng southwestCorner = SphericalUtil.computeOffset(latLng, distanceFromCenterToCorner, 225.0);
        LatLng northeastCorner = SphericalUtil.computeOffset(latLng, distanceFromCenterToCorner, 45.0);
        return new LatLngBounds(southwestCorner, northeastCorner);
    }

    public static void log(String message) {
        Util.log("MapsActivity", message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

        if (savedInstanceState != null) {
            isFullScreenEnabled = savedInstanceState.getBoolean(IS_FULL_SCREEN_ENABLED, false);
        } else {
            isFullScreenEnabled = false;
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        tB = (Toolbar) findViewById(R.id.toolbar_maps);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(getString(R.string.title_activity_maps).toUpperCase());

        ((ViewGroup) findViewById(R.id.parent_layout_maps)).getLayoutTransition().setDuration(500);
        ((ViewGroup) findViewById(R.id.parent_layout_maps)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        progressBar = findViewById(R.id.progressbar_maps);
        progressBar.setVisibility(View.VISIBLE);
        mapLayout = findViewById(R.id.map_layout);
        fullscreenMarkerIcon = findViewById(R.id.fullscreen_marker_icon);
        fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
        fullscreenMarkerIconShadow = findViewById(R.id.fullscreen_marker_icon_shadow);
        fullscreenMarkerIconShadow.setVisibility(View.GONE);
        setFullScreenFab = findViewById(R.id.set_fullscreen_fab);
        setFullScreenFab.setOnClickListener(this);
        setFullScreenFab.setVisibility(View.GONE);
        myLocationFab = findViewById(R.id.my_location_fab);
        Drawable myLocationFabDrawable = (ContextCompat.getDrawable(this, R.drawable.ic_small_location));
        myLocationFabDrawable.setAlpha(143);
        myLocationFab.setImageDrawable(myLocationFabDrawable);
        myLocationFab.setOnClickListener(this);
        myLocationFab.setVisibility(View.GONE);
        sendCurrentLocationLayout = findViewById(R.id.send_current_location_layout);
        sendCurrentLocationLayout.setOnClickListener(this);
        currentLocationName = findViewById(R.id.address_name_label);
        currentLocationAddres = findViewById(R.id.address_label);
        sendCurrentLocationLandscapeLayout = findViewById(R.id.send_current_location_layout_landscape);
        sendCurrentLocationLandscapeLayout.setOnClickListener(this);
        currentLocationLandscape = findViewById(R.id.address_name_label_landscape);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());

        fullscreenIconMarker = drawableBitmap(Util.mutateIconSecondary(this, R.drawable.ic_send_location, R.color.dark_primary_color_secondary));
    }

    /**
     * This method determines if the view of the send current
     * location option has to be shown or hide
     */
    private void setCurrentLocationVisibility() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mapLayout.getLayoutParams();

        if (isGPSEnabled()) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                sendCurrentLocationLayout.setVisibility(View.GONE);
                sendCurrentLocationLandscapeLayout.setVisibility(View.VISIBLE);
                params.bottomMargin = Util.px2dp(45, outMetrics);
            } else {
                sendCurrentLocationLayout.setVisibility(View.VISIBLE);
                sendCurrentLocationLandscapeLayout.setVisibility(View.GONE);
                params.bottomMargin = Util.px2dp(72, outMetrics);
            }
            mapLayout.setLayoutParams(params);
            return;
        } else {
            sendCurrentLocationLayout.setVisibility(View.GONE);
            sendCurrentLocationLandscapeLayout.setVisibility(View.GONE);
            params.bottomMargin = 0;
        }

        mapLayout.setLayoutParams(params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableLocationUpdates();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        enableLocationUpdates();
    }

    /**
     * This method disable the updates of the Location Manager service
     */
    private void disableLocationUpdates() {
        if (locationManager != null) locationManager.removeUpdates(this);
    }

    /**
     * This method enable the updates of the Location Manager service
     */
    private void enableLocationUpdates() {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_FULL_SCREEN_ENABLED, isFullScreenEnabled);
    }

    /**
     * This methods sets the correspondent icon of the full screen
     * mode button
     */
    private void setLocationFabDrawable() {
        Drawable setFullScreenFabDrawable;
        if (isFullScreenEnabled) {
            setFullScreenFabDrawable = (ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_exit_location));
        } else {
            setFullScreenFabDrawable = (ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_location));
        }
        setFullScreenFabDrawable.setAlpha(143);
        setFullScreenFab.setImageDrawable(setFullScreenFabDrawable);
    }

    /**
     * This method queries if the GPS of the devide is enabled
     *
     * @return if the GPS is enabled or not
     */
    private boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        log("onMapReady");
        mMap = googleMap;

        enableLocationUpdates();

        if (!isGPSEnabled()) {
            showEnableLocationDialog();
        } else {
            initMap();
        }

        setCurrentLocationVisibility();
    }

    /**
     * This method sets all the necessary map views
     */
    private void initMap() {

        if (mMap == null) {
            return;
        }

        if (isFullScreenEnabled) {
            progressBar.setVisibility(View.GONE);
        }

        if (isGPSEnabled()) {
            if (myLocationFab.getVisibility() != View.VISIBLE) {
                myLocationFab.setVisibility(View.VISIBLE);
            }
            if (setFullScreenFab.getVisibility() != View.VISIBLE) {
                setFullScreenFab.setVisibility(View.VISIBLE);
            }
            mMap.setMyLocationEnabled(true);
        } else {
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

        if (isGPSEnabled()) {
            if (isFullScreenEnabled) {
                setMyLocation(false);
            } else {
                setMyLocation(true);
            }
        }
        setFullScreen();
    }

    /**
     * This method shows an alert dialog advertising the device
     * does not have the GPS enabled, and permit he proceed to enable it
     */
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

    /**
     * This method sets the approximate current location
     *
     * @param animateCamera determines if has to have an animation or not while the camera of the map is moving
     */
    private void setMyLocation(final boolean animateCamera) {
        log("setMyLocation");
        if (mMap == null) {
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                log("getLastLocation() onFailure: " + e.getMessage());
                showSnackbar(findViewById(R.id.parent_layout_maps), getString(R.string.general_error));
            }
        }).addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    log("getLastLocation() onSuccess");
                    addresses = getAddresses(getApplicationContext(), location.getLatitude(), location.getLongitude());
                    if (addresses != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        String addressLine = addresses.get(0).getAddressLine(0);
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                            currentAddress = new MapAddress(latLng, getString(R.string.current_location_label), addressLine);
                            currentLocationName.setText(currentAddress.getName());
                            currentLocationAddres.setText(currentAddress.getAddress());
                        } else {
                            currentAddress = new MapAddress(latLng, getString(R.string.current_location_landscape_label, addressLine), addressLine);
                            String textToShow = String.format(currentAddress.getName());
                            try {
                                textToShow = textToShow.replace("[A]", "<font color=\'#8c8c8c\'>");
                                textToShow = textToShow.replace("[/A]", "</font>");
                            } catch (Exception e) {
                                e.printStackTrace();
                                log("Exception changing the format of a string");
                            }
                            Spanned result = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                            } else {
                                result = Html.fromHtml(textToShow);
                            }
                            currentLocationLandscape.setText(result);
                        }
                        myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    }
                    if (animateCamera) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
                    }
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * This method creates a Bitmap from a drawable
     *
     * @param drawable from which the Bitmap will be created
     * @return the Bitmap created
     */
    private Bitmap drawableBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * This method establish the results for create a message and send it to a chat.
     * It creates a view of a map that represents the location selected to send to
     * a chat and does a snapshot
     *
     * @param location address to send to chat
     */
    private void setActivityResult(final MapAddress location) {

        if (location == null) return;

        progressBar.setVisibility(View.VISIBLE);


        final Double latitude = location.getLatLng().latitude;
        final Double longitude = location.getLatLng().longitude;

        GoogleMapOptions options = new GoogleMapOptions()
                .compassEnabled(false)
                .mapToolbarEnabled(false)
                .camera(CameraPosition.fromLatLngZoom(location.getLatLng(), DEFAULT_ZOOM))
                .liteMode(true);

        mapView = new MapView(this, options);
        mapView.onCreate(null);

        final int mapWidth;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mapWidth = outMetrics.widthPixels;
        } else {
            mapWidth = outMetrics.heightPixels;
        }

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.addMarker(new MarkerOptions().position(location.getLatLng()));

                mapView.measure(View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY));
                mapView.layout(0, 0, mapWidth, mapWidth);

                LatLngBounds latLngBounds = getLatLngBounds(500, location.getLatLng());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        mapView.setDrawingCacheEnabled(true);
                        mapView.measure(View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY));
                        mapView.layout(0, 0, mapWidth, mapWidth);
                        mapView.buildDrawingCache(true);
                        Bitmap bitmap = Bitmap.createScaledBitmap(mapView.getDrawingCache(), SNAPSHOT_SIZE, SNAPSHOT_SIZE, false);
                        mapView.setDrawingCacheEnabled(false);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        int quality = 100;
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                        byte[] byteArray = stream.toByteArray();
                        log("The bitmaps has " + byteArray.length + " initial size");
                        while (byteArray.length > MAX_SIZE) {
                            stream = new ByteArrayOutputStream();
                            quality -= 10;
                            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                            byteArray = stream.toByteArray();
                        }
                        log("The bitmaps has " + byteArray.length + " final size with quality: " + quality);

                        Intent intent = new Intent();
                        intent.putExtra(SNAPSHOT, byteArray);
                        intent.putExtra(LATITUDE, latitude);
                        intent.putExtra(LONGITUDE, longitude);

                        if (getIntent() != null) {
                            intent.putExtra(EDITING_MESSAGE, getIntent().getBooleanExtra(EDITING_MESSAGE, false));
                            intent.putExtra(MSG_ID, getIntent().getLongExtra(MSG_ID, -1));
                        }
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
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

    /**
     * This method sets the attributes of the full screen marker
     * depending on if the mode is enabled or not
     */
    private void getMarkerInfo() {
        if (mMap == null) {
            return;
        }

        LatLng latLng = mMap.getCameraPosition().target;
        if (latLng == null) return;

        addresses = getAddresses(getApplicationContext(), latLng.latitude, latLng.longitude);
        String title = getString(R.string.title_marker_maps);

        if (addresses != null && addresses.size() > 0) {
            String address = addresses.get(0).getAddressLine(0);
            fullScreenAddress = new MapAddress(latLng, null, address);
            if (fullScreenMarker == null) {
                fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(address).icon(BitmapDescriptorFactory.fromBitmap(fullscreenIconMarker)));
                fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            } else {
                fullScreenMarker.setPosition(latLng);
                fullScreenMarker.setSnippet(address);
                fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            }
            if (!fullScreenMarker.isVisible()) {
                if (fullscreenMarkerIcon.getVisibility() == View.VISIBLE) {
                    fullscreenMarkerIcon.animate().translationY(0).setDuration(100L).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            fullScreenMarker.setVisible(true);
                            fullScreenMarker.showInfoWindow();
                        }
                    }).start();
                } else {
                    fullScreenMarker.setVisible(true);
                }
            }
            fullScreenMarker.showInfoWindow();
        } else {
            fullScreenAddress = new MapAddress(latLng, null, null);
            if (fullScreenMarker == null) {
                fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet("").icon(BitmapDescriptorFactory.fromBitmap(fullscreenIconMarker)));
                fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            } else {
                fullScreenMarker.setPosition(latLng);
                fullScreenMarker.setSnippet("");
                fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            }
            setAnimatingMarker(0);
        }
    }

    /**
     * This method establishes the corresponding view depending on
     * if the full screen mode is enabled or not
     */
    private void setFullScreen() {
        setLocationFabDrawable();

        if (mMap == null) {
            return;
        }

        if (isFullScreenEnabled) {
            getMarkerInfo();
        } else {
            fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
            fullscreenMarkerIconShadow.setVisibility(View.GONE);
            setMyLocation(false);

            try {
                fullScreenMarker.remove();
            } catch (Exception e) {
            }

            fullScreenMarker = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_current_location_layout_landscape:
            case R.id.send_current_location_layout: {
                setActivityResult(currentAddress);
                break;
            }
            case R.id.my_location_fab: {
                setMyLocation(true);
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
            setActivityResult(fullScreenAddress);
            return true;
        }

        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        log("onInfoWindowClick");
        if (isFullScreenEnabled && marker.equals(fullScreenMarker)) {
            setActivityResult(fullScreenAddress);
        }
    }

    @Override
    public void onCameraIdle() {
        if (isFullScreenEnabled && fullScreenMarker != null) {
            getMarkerInfo();
        }
    }

    /**
     * This method starts an animation of the full screen marker
     * with a duration received by the duration parameter
     *
     * @param duration length of the animation
     */
    private void setAnimatingMarker(long duration) {
        if (isFullScreenEnabled && fullScreenMarker != null) {
            fullScreenMarker.setVisible(false);
            fullscreenMarkerIcon.setVisibility(View.VISIBLE);
            fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            fullscreenMarkerIcon.animate().translationY(-Util.px2dp(12, outMetrics)).setDuration(duration).start();
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

        if (!provider.equals(LocationManager.GPS_PROVIDER) || mMap == null) return;

        progressBar.setVisibility(View.VISIBLE);
        mMap.clear();
        mMap.setMyLocationEnabled(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isFullScreenEnabled = false;
                setCurrentLocationVisibility();
                initMap();
            }
        }, 3000);
        return;
    }

    @Override
    public void onProviderDisabled(String provider) {
        log("LocationListener onProviderDisabled");

        if (!provider.equals(LocationManager.GPS_PROVIDER) || mMap == null) return;

        mMap.clear();
        setCurrentLocationVisibility();
        initMap();
    }
}
