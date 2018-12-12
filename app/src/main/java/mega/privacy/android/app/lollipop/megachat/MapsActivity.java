package mega.privacy.android.app.lollipop.megachat;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MapsAdapter;
import mega.privacy.android.app.utils.Util;

@SuppressLint("MissingPermission")
public class MapsActivity extends PinActivityLollipop implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener {

    private final int MAX_ENTRIES = 5;
    private final float DEFAULT_ZOOM = 18f;

    private DisplayMetrics outMetrics;
    private Toolbar tB;
    private ActionBar aB;

    private GoogleMap mMap;
    private RelativeLayout mapLayout;
    private RelativeLayout listLayout;
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

    MenuItem searchMenuItem;

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

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        tB = (Toolbar) findViewById(R.id.toolbar_maps);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(getString(R.string.title_activity_maps));

        mapLayout = (RelativeLayout) findViewById(R.id.map_layout);
        listLayout = (RelativeLayout) findViewById(R.id.list_layout);
        listAddresses = (RecyclerView) findViewById(R.id.address_list);
        listAddresses.setClipToPadding(false);
        layoutManager = new LinearLayoutManager(this);
        listAddresses.setLayoutManager(layoutManager);
        listAddresses.setHasFixedSize(true);
        listAddresses.setItemAnimator(new DefaultItemAnimator());
        adapter = new MapsAdapter(this, arrayListAddresses);
        listAddresses.setAdapter(adapter);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.px2dp(204, outMetrics));
            params1.addRule(RelativeLayout.BELOW, tB.getId());
            mapLayout.setLayoutParams(params1);
            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params2.addRule(RelativeLayout.BELOW, mapLayout.getId());
            listLayout.setLayoutParams(params2);
        }
        else {
            RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(Util.px2dp(300, outMetrics), ViewGroup.LayoutParams.MATCH_PARENT);
            params1.addRule(RelativeLayout.BELOW, tB.getId());
            mapLayout.setLayoutParams(params1);
            RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params2.addRule(RelativeLayout.BELOW, tB.getId());
            params2.addRule(RelativeLayout.RIGHT_OF, mapLayout.getId());
            listLayout.setLayoutParams(params2);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        log("onMapReady");
        mMap = googleMap;

        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        setMyLocation();
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
                    LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, DEFAULT_ZOOM));
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

    public static void log(String message) {
        Util.log("MapsActivity", message);
    }
}
