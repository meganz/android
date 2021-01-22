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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.middlelayer.map.MapHandler;
import mega.privacy.android.app.middlelayer.map.MegaLatLng;
import mega.privacy.android.app.service.map.MapHandlerImpl;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

@SuppressLint("MissingPermission")
public class MapsActivity extends PinActivityLollipop implements ActivityCompat.OnRequestPermissionsResultCallback, View.OnClickListener, LocationListener {

    public static final String SNAPSHOT = "snapshot";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String EDITING_MESSAGE = "editingMessage";
    public static final String MSG_ID = "msg_id";

    private final String IS_FULL_SCREEN_ENABLED = "isFullScreenEnabled";

    private static Geocoder geocoder;

    private DisplayMetrics outMetrics;
    private Toolbar tB;
    private ActionBar aB;
    private ProgressBar progressBar;
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
    private LocationManager locationManager;
    private List<Address> addresses;
    private MapAddress currentAddress;
    private boolean isFullScreenEnabled;
    private MapHandler mapHandler;

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
            logError("Exception trying to get an address from a latitude and a longitude", e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Callback when get a location's coordinate.
     *
     * @param lati Latitude of the location.
     * @param longi Longitude of the location.
     */
    public void onGetLastLocation(double lati, double longi) {
        addresses = getAddresses(this, lati, longi);
        if (addresses != null) {
            MegaLatLng latLng = new MegaLatLng(lati, longi);
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
                    logError("Exception changing the format of a string", e);
                }
                Spanned result;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                currentLocationLandscape.setText(result);
            }
        }
        progressBar.setVisibility(View.GONE);
    }

    /**
     * This method starts an animation of the full screen marker
     * with a duration received by the duration parameter
     *
     * @param duration length of the animation
     */
    public void setAnimatingMarker(long duration) {
        if (isFullScreenEnabled && mapHandler.hideMarker()) {
            fullscreenMarkerIcon.setVisibility(View.VISIBLE);
            fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            fullscreenMarkerIcon.animate().translationY(-dp2px(12, outMetrics)).setDuration(duration).start();
        }
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

        geocoder = new Geocoder(this, Locale.getDefault());
        Bitmap icon = drawableBitmap(mutateIconSecondary(this, R.drawable.ic_send_location, R.color.dark_primary_color_secondary));

        mapHandler = new MapHandlerImpl(this, icon);
    }

    /**
     * This method determines if the view of the send current
     * location option has to be shown or hide
     */
    public void setCurrentLocationVisibility() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mapLayout.getLayoutParams();

        if (isGPSEnabled()) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                sendCurrentLocationLayout.setVisibility(View.GONE);
                sendCurrentLocationLandscapeLayout.setVisibility(View.VISIBLE);
                params.bottomMargin = dp2px(45, outMetrics);
            } else {
                sendCurrentLocationLayout.setVisibility(View.VISIBLE);
                sendCurrentLocationLandscapeLayout.setVisibility(View.GONE);
                params.bottomMargin = dp2px(72, outMetrics);
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


    public boolean isFullScreenEnabled() {
        return isFullScreenEnabled;
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
    public void enableLocationUpdates() {
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
    public void setLocationFabDrawable() {
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
    public boolean isGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean onInit() {
        if (isGPSEnabled()) {
            if (myLocationFab.getVisibility() != View.VISIBLE) {
                myLocationFab.setVisibility(View.VISIBLE);
            }
            if (setFullScreenFab.getVisibility() != View.VISIBLE) {
                setFullScreenFab.setVisibility(View.VISIBLE);
            }
            return true;
        } else {
            if (myLocationFab.getVisibility() != View.GONE) {
                myLocationFab.setVisibility(View.GONE);
            }
            if (setFullScreenFab.getVisibility() != View.GONE) {
                setFullScreenFab.setVisibility(View.GONE);
            }
            isFullScreenEnabled = true;
            return false;
        }
    }

    public void setMyLocationAnimateCamera() {
        if (isGPSEnabled()) {
            if (isFullScreenEnabled) {
                setMyLocation(false);
            } else {
                setMyLocation(true);
            }
        }
    }

    /**
     * This method shows an alert dialog advertising the device
     * does not have the GPS enabled, and permit he proceed to enable it
     */
    public void showEnableLocationDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gps_disabled)
                .setMessage(R.string.open_location_settings)
                .setCancelable(false)
                .setPositiveButton(R.string.general_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        isFullScreenEnabled = true;
                        mapHandler.initMap();
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
    public void setMyLocation(final boolean animateCamera) {
        logDebug("setMyLocation");
        mapHandler.setMyLocation(animateCamera);

    }

    public void showError() {
        showSnackbar(findViewById(R.id.parent_layout_maps), getString(R.string.general_error));
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
    public void setActivityResult(final MapAddress location) {

        if (location == null) return;

        progressBar.setVisibility(View.VISIBLE);

        final double latitude = location.getLatLng().getLatitude();
        final double longitude = location.getLatLng().getLongitude();
        final int mapWidth;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mapWidth = outMetrics.widthPixels;
        } else {
            mapWidth = outMetrics.heightPixels;
        }
        mapHandler.createSnapshot(latitude, longitude, mapWidth);
    }

    public void dismissProgressBar() {
        if (isFullScreenEnabled) {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Callback when snapshot of current location is ready.
     *
     * @param byteArray Binary data of the snapshot.
     * @param latitude Latitude of the location.
     * @param longitude Longitude of the location.
     */
    public void onSnapshotReady(byte[] byteArray, double latitude, double longitude) {
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
    public void onPointerCaptureChanged(boolean hasCapture) {

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

    /**
     * Show map marker.
     */
    public void showMarker() {
        if (fullscreenMarkerIcon.getVisibility() == View.VISIBLE) {
            fullscreenMarkerIcon.animate().translationY(0).setDuration(100L).withEndAction(() ->
                    mapHandler.displayFullScreenMarker()
            ).start();
        } else {
            mapHandler.displayFullScreenMarker();
        }
    }

    /**
     * This method establishes the corresponding view depending on
     * if the full screen mode is enabled or not
     */
    public void setFullScreen() {
        setLocationFabDrawable();
        if (isFullScreenEnabled) {
            mapHandler.getMarkerInfo();
        } else {
            fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
            fullscreenMarkerIconShadow.setVisibility(View.GONE);
            setMyLocation(false);
            mapHandler.removeMarker();
        }
    }

    public void showMarkerIconShadow() {
        fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
    }

    public void hideCustomMarker() {
        fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
        fullscreenMarkerIconShadow.setVisibility(View.GONE);
    }

    @Override
    public void onLocationChanged(Location location) {
        logDebug("LocationListener onLocationChanged");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        logDebug("LocationListener onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
        logDebug("LocationListener onProviderEnabled");

        if (!provider.equals(LocationManager.GPS_PROVIDER) || mapHandler.isMapNull()) return;

        progressBar.setVisibility(View.VISIBLE);
        mapHandler.clearMap();
        mapHandler.setMyLocationEnabled(true);
        new Handler().postDelayed(() -> {
            isFullScreenEnabled = false;
            setCurrentLocationVisibility();
            mapHandler.initMap();
        }, 3000);
    }

    @Override
    public void onProviderDisabled(String provider) {
        logDebug("LocationListener onProviderDisabled");

        if (!provider.equals(LocationManager.GPS_PROVIDER) || mapHandler.isMapNull()) return;

        mapHandler.clearMap();
        setCurrentLocationVisibility();
        mapHandler.initMap();
    }
}
