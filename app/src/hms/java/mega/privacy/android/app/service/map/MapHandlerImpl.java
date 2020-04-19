package mega.privacy.android.app.service.map;

import android.graphics.Bitmap;
import android.location.Address;
import android.location.Location;

import com.huawei.hms.location.FusedLocationProviderClient;
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
import mega.privacy.android.app.lollipop.megachat.MapAddress;
import mega.privacy.android.app.lollipop.megachat.MapsActivity;
import mega.privacy.android.app.middlelayer.map.AbstractMapHandler;
import mega.privacy.android.app.middlelayer.map.MegaLatLng;

import static mega.privacy.android.app.lollipop.megachat.MapsActivity.getAddresses;
import static mega.privacy.android.app.utils.LogUtil.*;

public class MapHandlerImpl extends AbstractMapHandler implements OnMapReadyCallback, HuaweiMap.OnMyLocationClickListener, HuaweiMap.OnMyLocationButtonClickListener, HuaweiMap.OnCameraMoveStartedListener, HuaweiMap.OnCameraIdleListener, HuaweiMap.OnMarkerClickListener, HuaweiMap.OnInfoWindowClickListener {

    private HuaweiMap mMap;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private Marker fullScreenMarker;

    private MapAddress fullScreenAddress;


    public MapHandlerImpl(MapsActivity activity, Bitmap icon) {
        super(activity, icon);
        SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
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
        // don't create MapView for HMS, use the existing HuaweiMap object to create snapshot.
        mMap.setOnMapLoadedCallback(() -> mMap.snapshot((snapshot) -> {
            if (snapshot == null) return;
            // crop the snapshot.
            snapshot = Bitmap.createBitmap(snapshot, (snapshot.getWidth() - mapWidth) / 2,
                    (snapshot.getHeight() - mapWidth) / 2, mapWidth, mapWidth);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            int quality = 100;
            snapshot.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            byte[] byteArray = stream.toByteArray();
            logDebug("The bitmaps has " + byteArray.length + " initial size");
            while (byteArray.length > MAX_SIZE) {
                stream = new ByteArrayOutputStream();
                quality -= 10;
                snapshot.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                byteArray = stream.toByteArray();
            }
            snapshot.recycle();
            logDebug("The bitmaps has " + byteArray.length + " final size with quality: " + quality);
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
    public void setMyLocation(boolean animateCamera) {
        if (mMap == null) {
            return;
        }
        mFusedLocationProviderClient.getLastLocation().addOnFailureListener(e -> {
            logError("getLastLocation() onFailure: " + e.getMessage());
            showError();
        }).addOnSuccessListener(location -> {
            if (location != null) {
                logDebug("getLastLocation() onSuccess");
                onGetLastLocation(location.getLatitude(), location.getLongitude());
                if (animateCamera) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), DEFAULT_ZOOM));
                }
            }
        });
    }

    @Override
    public void removeMarker() {
        try {
            fullScreenMarker.remove();
        } catch (Exception e) {
            logError(e.getMessage(), e);
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
        logDebug("onMapReady");
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
        logDebug("onInfoWindowClick");
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
        logDebug("onMyLocationButtonClick");
        return false;
    }

    @Override
    public void onMyLocationClick(Location location) {
        logDebug("onMyLocationClick");
    }
}
