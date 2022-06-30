package mega.privacy.android.app.service.map;

import static mega.privacy.android.app.main.megachat.MapsActivity.REQUEST_INTERVAL;
import static mega.privacy.android.app.main.megachat.MapsActivity.getAddresses;

import timber.log.Timber;

import android.graphics.Bitmap;
import android.location.Address;
import android.location.Location;
import android.os.Looper;

import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;

import java.io.ByteArrayOutputStream;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.megachat.MapAddress;
import mega.privacy.android.app.main.megachat.MapsActivity;
import mega.privacy.android.app.middlelayer.map.AbstractMapHandler;
import mega.privacy.android.app.middlelayer.map.MegaLatLng;
import timber.log.Timber;


public class MapHandlerImpl extends AbstractMapHandler implements OnMapReadyCallback, HuaweiMap.OnMyLocationClickListener, HuaweiMap.OnMyLocationButtonClickListener, HuaweiMap.OnCameraMoveStartedListener, HuaweiMap.OnCameraIdleListener, HuaweiMap.OnMarkerClickListener, HuaweiMap.OnInfoWindowClickListener {

    private HuaweiMap mMap;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private Location lastLocation;

    private Marker fullScreenMarker;

    private MapAddress fullScreenAddress;


    public MapHandlerImpl(MapsActivity activity, Bitmap icon) {
        super(activity, icon);
        SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(REQUEST_INTERVAL);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    Timber.w("locationResult is null");
                    return;
                }

                lastLocation = locationResult.getLastLocation();
                onGetLastLocation(lastLocation.getLatitude(), lastLocation.getLongitude());

                if (!activity.isFullScreenEnabled()) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()),
                            DEFAULT_ZOOM));
                }
            }
        };
    }

    @Override
    public boolean isMapNull() {
        return mMap == null;
    }

    @Override
    public void clearMap() {
        if (!isMapNull()) {
            mMap.clear();
        }
    }

    @Override
    public void setMyLocationEnabled(boolean enable) {
        if (!isMapNull()) {
            mMap.setMyLocationEnabled(enable);
        }
    }

    @Override
    public void createSnapshot(double latitude, double longitude, int mapWidth) {
        // Don't create MapView for HMS, use the existing HuaweiMap object to create snapshot.
        mMap.setOnMapLoadedCallback(() -> mMap.snapshot((snapshot) -> {
            if (snapshot == null) return;

            // Cut out the middle part of the original snapshot.
            int x = 0, y = 0;
            int w = snapshot.getWidth();
            int h = snapshot.getHeight();

            if (w > SNAPSHOT_SIZE) {
                x = (w - SNAPSHOT_SIZE) / 2;
                w = SNAPSHOT_SIZE;
            }

            if (h > SNAPSHOT_SIZE) {
                y = (h - SNAPSHOT_SIZE) / 2;
                h = SNAPSHOT_SIZE;
            }

            snapshot = Bitmap.createBitmap(snapshot, x, y, w, h);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            int quality = 100;
            snapshot.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            byte[] byteArray = stream.toByteArray();
            Timber.d("The bitmaps has %d initial size", byteArray.length);
            while (byteArray.length > MAX_SIZE) {
                stream = new ByteArrayOutputStream();
                quality -= 10;
                snapshot.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                byteArray = stream.toByteArray();
            }
            snapshot.recycle();
            Timber.d("The bitmaps has %d final size with quality: %d", byteArray.length, quality);
            sendSnapshot(byteArray, latitude, longitude);
        }));
    }

    @Override
    public void initMap() {
        if (mMap == null) {
            return;
        }
        dismissProgress();
        mMap.setMyLocationEnabled(getInitResult());

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowClickListener(this);

        setMyLocationAnimate();
        setFullScreen();
    }

    @Override
    public void disableCurrentLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void enableCurrentLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,
                Looper.getMainLooper());
    }

    @Override
    public void setMyLocation(boolean animateCamera) {
        if (mMap == null || lastLocation == null) {
            Timber.w("mMap or lastLocation is null");
            return;
        }

        if (animateCamera) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()),
                    DEFAULT_ZOOM));
        }
    }

    @Override
    public void removeMarker() {
        try {
            if (fullScreenMarker != null) {
                fullScreenMarker.remove();
            }
        } catch (Exception e) {
            Timber.e(e);
            e.printStackTrace();
        }

        fullScreenMarker = null;
    }

    @Override
    public void getMarkerInfo() {
        if (mMap == null) {
            return;
        }

        LatLng latLng = mMap.getCameraPosition().target;
        if (latLng == null) return;

        List<Address> addresses = getAddresses(activity, latLng.latitude, latLng.longitude);
        String title = getInfoTitle();

        if (addresses != null && addresses.size() > 0) {
            String address = addresses.get(0).getAddressLine(0);
            fullScreenAddress = new MapAddress(new MegaLatLng(latLng.latitude, latLng.longitude), null, address);

            if (fullScreenMarker == null) {
                fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(address).icon(BitmapDescriptorFactory.fromBitmap(fullscreenIconMarker)));
            } else {
                fullScreenMarker.setPosition(latLng);
                fullScreenMarker.setSnippet(address);
            }

            showIconShadow();

            if (!fullScreenMarker.isVisible()) {
                showMarker();
            }

            fullScreenMarker.showInfoWindow();
        } else {
            fullScreenAddress = new MapAddress(new MegaLatLng(latLng.latitude, latLng.longitude), null, null);

            if (fullScreenMarker == null) {
                fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet("").icon(BitmapDescriptorFactory.fromBitmap(fullscreenIconMarker)));
            } else {
                fullScreenMarker.setPosition(latLng);
                fullScreenMarker.setSnippet("");
            }

            showIconShadow();
            setAnimatingMarker(0);
        }
    }

    @Override
    public void displayFullScreenMarker() {
        fullScreenMarker.setVisible(true);
        fullScreenMarker.showInfoWindow();
        hideCustomMarker();
    }

    @Override
    public boolean hideMarker() {
        if (fullScreenMarker != null) {
            fullScreenMarker.setVisible(false);
            return true;
        }

        return false;
    }

    @Override
    public void onMapReady(HuaweiMap map) {
        mMap = map;
        enableLocationUpdates();

        if (!isGPSEnabled()) {
            askForGPS();
        } else {
            initMap();
        }

        setCurrentLocationVisibility();
    }

    @Override
    public void onCameraIdle() {
        if (isFullScreen() && fullScreenMarker != null) {
            getMarkerInfo();
        }
    }

    @Override
    public void onCameraMoveStarted(int i) {
        setAnimatingMarker(100L);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (isFullScreen() && marker.equals(fullScreenMarker)) {
            setAddress(fullScreenAddress);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (isFullScreen() && marker.equals(fullScreenMarker)) {
            setAddress(fullScreenAddress);
            return true;
        }

        return false;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(Location location) {
    }
}
