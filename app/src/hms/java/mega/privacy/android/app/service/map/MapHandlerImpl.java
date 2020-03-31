package mega.privacy.android.app.service.map;

import android.graphics.Bitmap;
import android.location.Address;
import android.location.Location;
import android.view.View;

import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.HuaweiMapOptions;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.CameraPosition;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.LatLngBounds;
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
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;

public class MapHandlerImpl extends AbstractMapHandler implements OnMapReadyCallback, HuaweiMap.OnMyLocationClickListener, HuaweiMap.OnMyLocationButtonClickListener, HuaweiMap.OnCameraMoveStartedListener, HuaweiMap.OnCameraIdleListener, HuaweiMap.OnMarkerClickListener, HuaweiMap.OnInfoWindowClickListener {

    private HuaweiMap mMap;

    private MapView mapView;

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
        LatLng southwestCorner = computeOffset(latLng, distanceFromCenterToCorner, 225.0);
        LatLng northeastCorner = computeOffset(latLng, distanceFromCenterToCorner, 45.0);
        return new LatLngBounds(southwestCorner, northeastCorner);
    }

    // Code from com.google.maps.android.SphericalUtil
    private static LatLng computeOffset(LatLng from, double distance, double heading) {
        distance /= 6371009.0D;
        heading = Math.toRadians(heading);
        double fromLat = Math.toRadians(from.latitude);
        double fromLng = Math.toRadians(from.longitude);
        double cosDistance = Math.cos(distance);
        double sinDistance = Math.sin(distance);
        double sinFromLat = Math.sin(fromLat);
        double cosFromLat = Math.cos(fromLat);
        double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(heading);
        double dLng = Math.atan2(sinDistance * cosFromLat * Math.sin(heading), cosDistance - sinFromLat * sinLat);
        return new LatLng(Math.toDegrees(Math.asin(sinLat)), Math.toDegrees(fromLng + dLng));
    }

    @Override
    public void createSnapshot(double latitude, double longitude, int mapWidth) {
        LatLng location = new LatLng(latitude, longitude);
        HuaweiMapOptions options = new HuaweiMapOptions()
                .compassEnabled(false)
                .mapToolbarEnabled(false)
                .camera(CameraPosition.fromLatLngZoom(location, DEFAULT_ZOOM))
                .liteMode(true);

        mapView = new MapView(getContext(), options);
        mapView.onCreate(null);
        mapView.getMapAsync(googleMap -> {
            googleMap.addMarker(new MarkerOptions().position(location));

            mapView.measure(View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(mapWidth, View.MeasureSpec.EXACTLY));
            mapView.layout(0, 0, mapWidth, mapWidth);

            LatLngBounds latLngBounds = getLatLngBounds(RADIUS, location);
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
                sendSnapshot(byteArray, latitude, longitude);
            });
        });
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

        List<Address> addresses = getAddresses(getContext(), latLng.latitude, latLng.longitude);
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
