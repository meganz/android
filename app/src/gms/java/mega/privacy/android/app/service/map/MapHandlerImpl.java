package mega.privacy.android.app.service.map;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.*;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.SphericalUtil;

import mega.privacy.android.app.R;

public class MapHandlerImpl implements OnMapReadyCallback,  OnMyLocationClickListener, OnMyLocationButtonClickListener, OnCameraMoveStartedListener, OnCameraIdleListener, OnMarkerClickListener, OnInfoWindowClickListener{

    private FragmentActivity activity;
    private GoogleMap mMap;
    private MapView mapView;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LatLng myLocation = null;
    private Marker fullScreenMarker;

    public MapHandlerImpl(FragmentActivity activity) {
        this.activity = activity;
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

    }

    @Override
    public void onCameraMoveStarted(int i) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
