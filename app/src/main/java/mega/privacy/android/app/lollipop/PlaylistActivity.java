package mega.privacy.android.app.lollipop;

import android.app.SearchManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.adapters.PlayListAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

/**
 * Created by mega on 24/04/18.
 */

public class PlaylistActivity extends PinActivityLollipop {

    private Toolbar tB;
    private ActionBar aB;

    FastScroller fastScroller;
    RecyclerView recyclerView;
    PlayListAdapter adapter;

    private MegaApiAndroid megaApi;

    private MenuItem searchMenuItem;

    ArrayList<MegaNode> nodes;
    ArrayList<Long> handles = new ArrayList<>();
    Long parentHandle;
    String parenPath;
    ArrayList<MegaOffline> offNodes;
    int adapterType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playlist);

        if (getIntent() == null){
            return;
        }

        tB = (Toolbar) findViewById(R.id.toolbar);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        tB.setTitle(getString(R.string.section_playlist));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        recyclerView = (RecyclerView) findViewById(R.id.file_list_view_browser);
        fastScroller = (FastScroller) findViewById(R.id.fastscroll);

        parentHandle = getIntent().getLongExtra("parentNodeHandle", -1);
        parenPath = getIntent().getStringExtra("pathNavigation");
        if (parenPath == null) {
            adapterType = Constants.OFFLINE_ADAPTER;
//            Add offline code
        }
        else {
            adapterType = -1;
            nodes = megaApi.getChildren(megaApi.getNodeByHandle(parentHandle));
            for (int i=0; i<nodes.size(); i++){
                handles.add(nodes.get(i).getHandle());
                adapter = new PlayListAdapter(this, offNodes, parenPath, recyclerView);
            }
            adapter = new PlayListAdapter(this, handles, parentHandle, recyclerView);
        }
        recyclerView.setAdapter(adapter);
        fastScroller.setRecyclerView(recyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_playlist, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        if (searchView != null){
            searchView.setIconifiedByDefault(true);
        }

//        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
//            @Override
//            public boolean onMenuItemActionExpand(MenuItem item) {
//                textsearchQuery = false;
//                searchQuery = "";
//                selectDrawerItemLollipop(DrawerItem.SEARCH);
//                return true;
//            }
//
//            @Override
//            public boolean onMenuItemActionCollapse(MenuItem item) {
//                log("On collapse search menu item");
//                drawerItem = DrawerItem.CLOUD_DRIVE;
//                selectDrawerItemLollipop(DrawerItem.CLOUD_DRIVE);
//                textSubmitted = true;
//                return true;
//            }
//        });
//        searchView.setMaxWidth(Integer.MAX_VALUE);
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                searchQuery = "" + query;
//                selectDrawerItemLollipop(DrawerItem.SEARCH);
//                setToolbarTitle();
//                supportInvalidateOptionsMenu();
//                log("Search query: " + query);
//                textSubmitted = true;
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                log("Searching by text: "+newText);
//                if(textSubmitted){
//                    textSubmitted = false;
//                }
//                else if (textsearchQuery) {
//                    selectDrawerItemLollipop(DrawerItem.SEARCH);
//                }
//                else{
//                    searchQuery = newText;
//                    selectDrawerItemLollipop(DrawerItem.SEARCH);
//                }
//                return true;
//            }
//
//        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.action_search:{

                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public static void log(String message) {
        Util.log("PlaylistActivity", message);
    }
}
