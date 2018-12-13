package mega.privacy.android.app.lollipop.megachat;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
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
import android.widget.RelativeLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MapsAdapter;
import mega.privacy.android.app.utils.Util;

@SuppressLint("MissingPermission")
public class MapsActivity extends PinActivityLollipop implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener, View.OnClickListener, GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraIdleListener {

    private final float DEFAULT_ZOOM = 18f;
    private final float DEFAULT_FULLSCREEN_ZOOM = 19f;

    private DisplayMetrics outMetrics;
    private Toolbar tB;
    private ActionBar aB;

    private GoogleMap mMap;
    private RelativeLayout mapLayout;
    private View shadowLayout;
    private RelativeLayout listLayout;
    private FloatingActionButton setFullScreenFab;
    private FloatingActionButton myLocationFab;
    private SupportMapFragment mapFragment;
    private RecyclerView listAddresses;
    private LinearLayoutManager layoutManager;
    private MapsAdapter adapter;

    private GeoDataClient geoDataClient;
    private PlaceDetectionClient placeDetectionClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    Geocoder geocoder;
    List<Address> addresses;
    ArrayList<MapAddress> arrayListAddresses = new ArrayList<>();
    LatLng myLocation;

    MenuItem searchMenuItem;

    private boolean isFullScreenEnabled = false;
    private Marker fullScreenMarker;

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
        }
        else {
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
        aB.setTitle(getString(R.string.title_activity_maps));

        ((ViewGroup) findViewById(R.id.parent_layout_maps)).getLayoutTransition().setDuration(500);
        ((ViewGroup) findViewById(R.id.parent_layout_maps)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        mapLayout = (RelativeLayout) findViewById(R.id.map_layout);
        shadowLayout = (View) findViewById(R.id.shadow_maps_layout);
        listLayout = (RelativeLayout) findViewById(R.id.list_layout);
        setFullScreenFab = (FloatingActionButton) findViewById(R.id.set_fullscreen_fab);
        setFullScreenFab.setOnClickListener(this);
        myLocationFab = (FloatingActionButton) findViewById(R.id.my_location_fab);
        Drawable myLocationFabDrawable = (ContextCompat.getDrawable(this, R.drawable.ic_small_location));
        myLocationFabDrawable.setAlpha(143);
        myLocationFab.setImageDrawable(myLocationFabDrawable);
        myLocationFab.setOnClickListener(this);
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
            setListParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        else {
            setMapParams(Util.px2dp(300, outMetrics), ViewGroup.LayoutParams.MATCH_PARENT);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Util.px2dp(4, outMetrics), ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, mapLayout.getId());
            shadowLayout.setLayoutParams(params);
            shadowLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.maps_shadow_landscape));
            setListParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        geoDataClient = Places.getGeoDataClient(this);
        placeDetectionClient = Places.getPlaceDetectionClient(this);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(this, Locale.getDefault());
    }

    void setMapParams (int width, int height) {
        if (mapLayout == null) {
            mapLayout = (RelativeLayout) findViewById(R.id.map_layout);
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        params.addRule(RelativeLayout.BELOW, tB.getId());
        mapLayout.setLayoutParams(params);
    }

    void setListParams (int width, int height) {
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        log("onMapReady");
        mMap = googleMap;

        mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        setMyLocation();
        setFullScreen();
    }

    private void setMyLocation() {
        if (mMap == null) {
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    addresses = getAddresses(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null) {
                        arrayListAddresses.add(0, new MapAddress(getString(R.string.current_location_label), addresses.get(0).getAddressLine(0)));
                    }
                    myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (!isFullScreenEnabled) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
                    }
                }
                setAdapterAddresses();
            }
        });

        placeDetectionClient.getCurrentPlace(null).addOnSuccessListener(this, new OnSuccessListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onSuccess(PlaceLikelihoodBufferResponse placeLikelihoods) {
                int i = 0;
                Bitmap icon = drawableBitmap(getDrawable(R.drawable.ic_map_marker));
                for (PlaceLikelihood placeLikelihood : placeLikelihoods) {
                    i++;
//                    https://github.com/googlemaps/android-samples/blob/master/ApiDemos/java/app/src/main/java/com/example/mapdemo/CircleDemoActivity.java
                    mMap.addMarker(new MarkerOptions().position(placeLikelihood.getPlace().getLatLng()).icon(BitmapDescriptorFactory.fromBitmap(icon)));
                    arrayListAddresses.add(i, new MapAddress(placeLikelihood.getPlace()));
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
        log("itemClick at position: "+position+" [name] "+arrayListAddresses.get(position).getName()+" [address] "+arrayListAddresses.get(position).getAddress());
    }

    void setAdapterAddresses () {
        if (adapter == null){
            adapter = new MapsAdapter(this, arrayListAddresses);
        }
        else{
            adapter.setAddresses(arrayListAddresses);
        }
    }

    List<Address> getAddresses (double latitude, double longitude, int maxResults) {
        if (geocoder != null) {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, maxResults);
                if (addresses != null) {
                    return addresses;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_maps, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = (MenuItem) menu.findItem(R.id.action_search);
        searchMenuItem.setIcon(Util.mutateIconSecondary(this, R.drawable.ic_menu_search, R.color.white));
        final SearchView searchView = (SearchView) searchMenuItem.getActionView();
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

        return super.onCreateOptionsMenu(menu);
    }

    void onMapSearch(String search) {
        if (search != null && !search.equals("")) {

        }
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
                int heigth = outMetrics.heightPixels - rect.top - tB.getHeight() - Util.px2dp(72, outMetrics);
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
        addresses = getAddresses(latLng.latitude, latLng.longitude, 1);
        String address = addresses.get(0).getAddressLine(0);
        String title = getString(R.string.title_marker_maps);
        if (fullScreenMarker == null) {
            fullScreenMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(address));
        }
        else {
            fullScreenMarker.setPosition(latLng);
            fullScreenMarker.setSnippet(address);
        }
        fullScreenMarker.showInfoWindow();
        updateFirstPosition(title, address);
    }

    void updateFirstPosition (String title, String address) {
        MapAddress marker = (MapAddress) adapter.getItem(0);
        if (marker != null) {
            marker.setName(title);
            marker.setAddress(address);
        }
        adapter.notifyItemChanged(0);
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
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        addresses = getAddresses(location.getLatitude(), location.getLongitude(), 1);
                        if (addresses != null) {
                            arrayListAddresses.set(0, new MapAddress(getString(R.string.current_location_label), addresses.get(0).getAddressLine(0)));
                        }
                        myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        updateFirstPosition(arrayListAddresses.get(0).getName(), arrayListAddresses.get(0).getAddress());
                    }
                }
            });
            try {
                fullScreenMarker.remove();
            } catch (Exception e){}

            fullScreenMarker = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.my_location_fab: {
                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null && mMap != null) {
                            myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
                        }
                        setAdapterAddresses();
                    }
                });
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
    public void onCameraIdle() {
        if (isFullScreenEnabled) {
            getMarkerInfo();
        }
    }

    @Override
    public void onCameraMoveStarted(int i) {
        if (isFullScreenEnabled) {
            getMarkerInfo();
        }
    }

    public static void log(String message) {
        Util.log("MapsActivity", message);
    }
}
