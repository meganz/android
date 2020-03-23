package mega.privacy.android.app.service.map;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Location;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.*;
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
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.MapAddress;
import mega.privacy.android.app.lollipop.megachat.MapsActivity;
import mega.privacy.android.app.middlelayer.map.MegaLatLng;

import static mega.privacy.android.app.lollipop.megachat.MapsActivity.DEFAULT_ZOOM;
import static mega.privacy.android.app.lollipop.megachat.MapsActivity.getAddresses;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.Util.px2dp;

public class MapHandlerImpl implements OnMapReadyCallback, OnMyLocationClickListener, OnMyLocationButtonClickListener, OnCameraMoveStartedListener, OnCameraIdleListener, OnMarkerClickListener, OnInfoWindowClickListener {

    private final static int MAX_SIZE = 45000;
    private final static int SNAPSHOT_SIZE = 750;

    private MapsActivity activity;
    private GoogleMap mMap;
    private MapView mapView;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng myLocation;
    private Marker fullScreenMarker;
    private MapAddress fullScreenAddress;
    private List<Address> addresses;
    private DisplayMetrics outMetrics;

    public MapHandlerImpl(MapsActivity activity, DisplayMetrics outMetrics) {
        this.activity = activity;
        this.outMetrics = outMetrics;
        mapFragment = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
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

    @Override
    public void onCameraIdle() {
        if (activity.isFullScreenEnabled && fullScreenMarker != null) {
            getMarkerInfo();
        }
    }

    @Override
    public void onCameraMoveStarted(int i) {
        setAnimatingMarker(100L);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        logDebug("onInfoWindowClick");
        if (activity.isFullScreenEnabled && marker.equals(fullScreenMarker)) {
            activity.setActivityResult(fullScreenAddress);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (activity.isFullScreenEnabled && marker.equals(fullScreenMarker)) {
            activity.setActivityResult(fullScreenAddress);
            return true;
        }
        return false;
    }

    public boolean isMapNull() {
        return mMap == null;
    }

    public void clear(){
        if(!isMapNull()) {
            mMap.clear();
        }
    }

    public void setMyLocationEnabled(boolean enable) {
        if(!isMapNull()) {
            mMap.setMyLocationEnabled(enable);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        logDebug("onMyLocationButtonClick");
        return false;
    }

    @Override
    public void onMyLocationClick(Location location) {
        logDebug("onMyLocationClick");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        logDebug("onMapReady");
        mMap = googleMap;

        activity.enableLocationUpdates();

        if (!activity.isGPSEnabled()) {
            activity.showEnableLocationDialog();
        } else {
            initMap();
        }

        activity.setCurrentLocationVisibility();
    }

    public void createSnapshot(double lati, double longi, int mapWidth, MapsActivity.OnSnapshotReady callback) {
        LatLng location = new LatLng(lati, longi);
        GoogleMapOptions options = new GoogleMapOptions()
                .compassEnabled(false)
                .mapToolbarEnabled(false)
                .camera(CameraPosition.fromLatLngZoom(location, DEFAULT_ZOOM))
                .liteMode(true);

        mapView = new MapView(activity, options);
        mapView.onCreate(null);
        mapView.getMapAsync(googleMap -> {
            googleMap.addMarker(new MarkerOptions().position(location));

            mapView.measure(View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY));
            mapView.layout(0, 0, mapWidth, mapWidth);

            LatLngBounds latLngBounds = getLatLngBounds(500, location);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 0));
            googleMap.setOnMapLoadedCallback(() -> {
                mapView.setDrawingCacheEnabled(true);
                mapView.measure(View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY));
                mapView.layout(0, 0, mapWidth, mapWidth);
                mapView.buildDrawingCache(true);
                Bitmap bitmap = Bitmap.createScaledBitmap(mapView.getDrawingCache(), SNAPSHOT_SIZE, SNAPSHOT_SIZE, true);
                mapView.setDrawingCacheEnabled(false);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                int quality = 100;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                byte[] byteArray = stream.toByteArray();
                logDebug("The bitmaps has " + byteArray.length + " initial size");
                while (byteArray.length > MAX_SIZE) {
                    stream = new ByteArrayOutputStream();
                    quality -= 10;
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                    byteArray = stream.toByteArray();
                }
                logDebug("The bitmaps has " + byteArray.length + " final size with quality: " + quality);
                activity.onSnapshotReady(byteArray, lati, longi);
                callback.onSnapshotReady(byteArray, lati, longi);
            });
        });
    }

    /**
     * This method sets all the necessary map views
     */
    public void initMap() {

        if (mMap == null) {
            return;
        }

        if (activity.isFullScreenEnabled) {
            activity.progressBar.setVisibility(View.GONE);
        }

        if (activity.isGPSEnabled()) {
            if (activity.myLocationFab.getVisibility() != View.VISIBLE) {
                activity.myLocationFab.setVisibility(View.VISIBLE);
            }
            if (activity.setFullScreenFab.getVisibility() != View.VISIBLE) {
                activity.setFullScreenFab.setVisibility(View.VISIBLE);
            }
            mMap.setMyLocationEnabled(true);
        } else {
            if (activity.myLocationFab.getVisibility() != View.GONE) {
                activity.myLocationFab.setVisibility(View.GONE);
            }
            if (activity.setFullScreenFab.getVisibility() != View.GONE) {
                activity.setFullScreenFab.setVisibility(View.GONE);
            }
            activity.isFullScreenEnabled = true;
            mMap.setMyLocationEnabled(false);
        }

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        if (activity.isGPSEnabled()) {
            if (activity.isFullScreenEnabled) {
                activity.setMyLocation(false);
            } else {
                activity.setMyLocation(true);
            }
        }
        setFullScreen();
    }

    /**
     * This method establishes the corresponding view depending on
     * if the full screen mode is enabled or not
     */
    public void setFullScreen() {
        activity.setLocationFabDrawable();
        if (activity.isFullScreenEnabled) {
            getMarkerInfo();
        } else {
            activity.fullscreenMarkerIcon.setVisibility(View.INVISIBLE);
            activity.fullscreenMarkerIconShadow.setVisibility(View.GONE);
            setMyLocation(false);

            try {
                fullScreenMarker.remove();
            } catch (Exception e) {
            }

            fullScreenMarker = null;
        }
    }

    public void setMyLocation(final boolean animateCamera) {
        if (mMap == null) {
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnFailureListener(e -> {
            logError("getLastLocation() onFailure: " + e.getMessage());
            activity.showError();
        }).addOnSuccessListener(location -> {
            if (location != null) {
                logDebug("getLastLocation() onSuccess");
                activity.onGetLastLocation(location.getLatitude(), location.getLongitude());
                if (animateCamera) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                }
            }
        });
    }

    /**
     * This method sets the attributes of the full screen marker
     * depending on if the mode is enabled or not
     */
    public void getMarkerInfo() {
        if (mMap == null) {
            return;
        }

        LatLng latLng = mMap.getCameraPosition().target;
        if (latLng == null) return;

        addresses = getAddresses(activity, latLng.latitude, latLng.longitude);
        String title = activity.getString(R.string.title_marker_maps);

        if (addresses != null && addresses.size() > 0) {
            String address = addresses.get(0).getAddressLine(0);
            fullScreenAddress = new MapAddress(new MegaLatLng(latLng.latitude, latLng.longitude), null, address);
            if (fullScreenMarker == null) {
                fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(address).icon(BitmapDescriptorFactory.fromBitmap(activity.fullscreenIconMarker)));
                activity.fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            } else {
                fullScreenMarker.setPosition(latLng);
                fullScreenMarker.setSnippet(address);
                activity.fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            }
            if (!fullScreenMarker.isVisible()) {
                if (activity.fullscreenMarkerIcon.getVisibility() == View.VISIBLE) {
                    activity.fullscreenMarkerIcon.animate().translationY(0).setDuration(100L).withEndAction(new Runnable() {
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
            fullScreenAddress = new MapAddress(new MegaLatLng(latLng.latitude, latLng.longitude), null, null);
            if (fullScreenMarker == null) {
                fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet("").icon(BitmapDescriptorFactory.fromBitmap(activity.fullscreenIconMarker)));
                activity.fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            } else {
                fullScreenMarker.setPosition(latLng);
                fullScreenMarker.setSnippet("");
                activity.fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            }
            setAnimatingMarker(0);
        }
    }

    /**
     * This method starts an animation of the full screen marker
     * with a duration received by the duration parameter
     *
     * @param duration length of the animation
     */
    public void setAnimatingMarker(long duration) {
        if (activity.isFullScreenEnabled && fullScreenMarker != null) {
            fullScreenMarker.setVisible(false);
            activity.fullscreenMarkerIcon.setVisibility(View.VISIBLE);
            activity.fullscreenMarkerIconShadow.setVisibility(View.VISIBLE);
            activity.fullscreenMarkerIcon.animate().translationY(-px2dp(12, outMetrics)).setDuration(duration).start();
        }
    }
}