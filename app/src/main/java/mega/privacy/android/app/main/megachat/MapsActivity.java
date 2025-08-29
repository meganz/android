package mega.privacy.android.app.main.megachat;

import static mega.privacy.android.app.extensions.EdgeToEdgeExtensionsKt.enableEdgeToEdgeAndConsumeInsets;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.mutateIconSecondary;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.content.Context;
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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.middlelayer.map.MapHandler;
import mega.privacy.android.app.middlelayer.map.MegaLatLng;
import mega.privacy.android.app.service.map.MapHandlerImpl;
import timber.log.Timber;

@SuppressLint("MissingPermission")
public class MapsActivity extends PasscodeActivity implements ActivityCompat.OnRequestPermissionsResultCallback, View.OnClickListener, LocationListener {

    public static final int REQUEST_INTERVAL = 3000;
    public static final int ICONS_ALPHA = 143;
    public static final String SNAPSHOT = "snapshot";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String EDITING_MESSAGE = "editingMessage";
    public static final String MSG_ID = "msg_id";

    private static Geocoder geocoder;

    private DisplayMetrics outMetrics;
    private ProgressBar progressBar;
    private RelativeLayout mapLayout;
    private RelativeLayout sendCurrentLocationLayout;
    private RelativeLayout sendCurrentLocationLandscapeLayout;
    private TextView currentLocationName;
    private TextView currentLocationLandscape;
    private TextView currentLocationAddress;
    private FloatingActionButton setFullScreenFab;
    private FloatingActionButton myLocationFab;
    private ImageView fullscreenMarkerIcon;
    private ImageView fullscreenMarkerIconShadow;
    private LocationManager locationManager;
    private MapAddress currentAddress;
    private boolean isFullScreenEnabled;
    private MapHandler mapHandler;

    private boolean isGPSEnabled;
    private int screenOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        enableEdgeToEdgeAndConsumeInsets(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        screenOrientation = getResources().getConfiguration().orientation;

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        Toolbar tB = findViewById(R.id.toolbar_maps);
        setSupportActionBar(tB);

        ActionBar aB = getSupportActionBar();
        if (aB == null) {
            finish();
            return;
        }

        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(getString(R.string.title_activity_maps));

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
        myLocationFab.setImageDrawable(myLocationFabDrawable);
        myLocationFab.setOnClickListener(this);
        myLocationFab.setVisibility(View.GONE);
        sendCurrentLocationLayout = findViewById(R.id.send_current_location_layout);
        sendCurrentLocationLayout.setOnClickListener(this);
        currentLocationName = findViewById(R.id.address_name_label);
        currentLocationAddress = findViewById(R.id.address_label);
        sendCurrentLocationLandscapeLayout = findViewById(R.id.send_current_location_layout_landscape);
        sendCurrentLocationLandscapeLayout.setOnClickListener(this);
        currentLocationLandscape = findViewById(R.id.address_name_label_landscape);

        geocoder = new Geocoder(this, Locale.getDefault());
        Bitmap icon = drawableBitmap(mutateIconSecondary(this, R.drawable.ic_send_location, R.color.red_800));

        mapHandler = new MapHandlerImpl(this, icon);

        isGPSEnabled = isGPSEnabled();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (screenOrientation == newConfig.orientation) {
            return;
        }

        screenOrientation = newConfig.orientation;

        if (isGPSEnabled()) {
            setCurrentLocationVisibility();
        }
    }

    /**
     * This method gets an address from the coordinates passed like parameters
     *
     * @param context   from which the action is done
     * @param latitude  is the latitude of a coordinate
     * @param longitude is the longitude of a coordinate
     * @return A list with the address.
     */
    public static List<Address> getAddresses(Context context, double latitude, double longitude) {
        if (geocoder == null) {
            geocoder = new Geocoder(context, Locale.getDefault());
        }

        try {
            return geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            Timber.e(e, "Exception trying to get an address from a latitude and a longitude");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Callback when get a location's coordinate.
     *
     * @param latitude  Latitude of the location.
     * @param longitude Longitude of the location.
     */
    public void onGetLastLocation(double latitude, double longitude) {
        List<Address> addresses = getAddresses(this, latitude, longitude);
        if (addresses != null) {
            MegaLatLng latLng = new MegaLatLng(latitude, longitude);
            String addressLine = addresses.get(0).getAddressLine(0);

            // Portrait
            currentAddress = new MapAddress(latLng,
                    getString(R.string.current_location_label), addressLine);

            currentLocationName.setText(currentAddress.getName());
            currentLocationAddress.setText(currentAddress.getAddress());

            // Landscape
            currentAddress = new MapAddress(latLng,
                    getString(R.string.current_location_landscape_label, addressLine), addressLine);

            String textToShow = currentAddress.getName();

            try {
                textToShow = textToShow.replace("[A]", "<font color=\'#8c8c8c\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
            } catch (Exception e) {
                e.printStackTrace();
                Timber.w(e, "Exception changing the format of a string");
            }

            currentLocationLandscape.setText(HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY));
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

    /**
     * This method determines if the view of the send current
     * location option has to be shown or hide
     */
    public void setCurrentLocationVisibility() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mapLayout.getLayoutParams();

        if (!isGPSEnabled()) {
            sendCurrentLocationLayout.setVisibility(View.GONE);
            sendCurrentLocationLandscapeLayout.setVisibility(View.GONE);
            params.bottomMargin = 0;
        } else if (screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            sendCurrentLocationLayout.setVisibility(View.GONE);
            sendCurrentLocationLandscapeLayout.setVisibility(View.VISIBLE);
            params.bottomMargin = dp2px(45, outMetrics);
        } else {
            sendCurrentLocationLayout.setVisibility(View.VISIBLE);
            sendCurrentLocationLandscapeLayout.setVisibility(View.GONE);
            params.bottomMargin = dp2px(72, outMetrics);
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

        if (isGPSEnabled == isGPSEnabled()) {
            //Same state than before pause, no need to update.
            return;
        }

        isGPSEnabled = !isGPSEnabled;

        if (isGPSEnabled) {
            providerEnabled();
        } else {
            providerDisabled();
        }
    }

    public boolean isFullScreenEnabled() {
        return isFullScreenEnabled;
    }

    /**
     * This method disable the updates of the Location Manager service
     */
    private void disableLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }

        if (mapHandler != null) {
            mapHandler.disableCurrentLocationUpdates();
        }
    }

    /**
     * This method enable the updates of the Location Manager service
     */
    public void enableLocationUpdates() {
        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        if (mapHandler != null) {
            mapHandler.enableCurrentLocationUpdates();
        }
    }

    /**
     * This methods sets the correspondent icon of the full screen
     * mode button
     */
    public void setLocationFabDrawable() {
        Drawable setFullScreenFabDrawable = (ContextCompat.getDrawable(this,
                isFullScreenEnabled
                        ? R.drawable.ic_fullscreen_exit_location
                        : R.drawable.ic_fullscreen_location));

        if (setFullScreenFabDrawable != null) {
            setFullScreenFab.setImageDrawable(setFullScreenFabDrawable);
        }
    }

    /**
     * This method queries if the GPS of the devide is enabled
     *
     * @return if the GPS is enabled or not
     */
    public boolean isGPSEnabled() {
        return locationManager != null
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
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
            setMyLocation(!isFullScreenEnabled);
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
                .setPositiveButton(mega.privacy.android.shared.resources.R.string.general_ok, (dialog, id) -> startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, (dialog, id) -> {
                    isFullScreenEnabled = true;
                    mapHandler.initMap();
                    dialog.cancel();
                    if (progressBar.getVisibility() != View.GONE) {
                        progressBar.setVisibility(View.GONE);
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
        Timber.d("setMyLocation");
        mapHandler.setMyLocation(animateCamera);

    }

    public void showError() {
        showSnackbar(findViewById(R.id.parent_layout_maps),
                getString(R.string.general_error));
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

        mapHandler.createSnapshot(latitude, longitude, outMetrics.widthPixels);
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
     * @param latitude  Latitude of the location.
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
        int id = v.getId();
        if (id == R.id.send_current_location_layout_landscape || id == R.id.send_current_location_layout) {
            setActivityResult(currentAddress);
        } else if (id == R.id.my_location_fab) {
            setMyLocation(true);
        } else if (id == R.id.set_fullscreen_fab) {
            isFullScreenEnabled = !isFullScreenEnabled;
            setFullScreen();
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

    private void providerEnabled() {
        if (mapHandler == null || mapHandler.isMapNull()) {
            Timber.w("mapHandler or mMap is null");
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        mapHandler.clearMap();
        mapHandler.setMyLocationEnabled(true);
        isFullScreenEnabled = false;
        setCurrentLocationVisibility();
        mapHandler.initMap();
    }

    private void providerDisabled() {
        if (mapHandler == null || mapHandler.isMapNull()) {
            Timber.w("mapHandler or mMap is null");
            return;
        }

        mapHandler.clearMap();
        setCurrentLocationVisibility();
        mapHandler.initMap();
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("LocationListener onLocationChanged");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Timber.d("LocationListener onStatusChanged");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Timber.d("LocationListener onProviderEnabled");

        if (!provider.equals(LocationManager.GPS_PROVIDER)) {
            return;
        }

        providerEnabled();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Timber.d("LocationListener onProviderDisabled");

        if (!provider.equals(LocationManager.GPS_PROVIDER)) {
            return;
        }

        providerDisabled();
    }
}
