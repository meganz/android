package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.adapters.PlayListAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

/**
 * Created by mega on 24/04/18.
 */

public class PlaylistFragment extends Fragment{

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    DisplayMetrics outMetrics;

    private Toolbar tB;
    private ActionBar aB;
    Context context;

    FastScroller fastScroller;
    LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    PlayListAdapter adapter;
    public TextView contentText;
    RelativeLayout containerContentText;
    private SimpleExoPlayerView simpleExoPlayerViewPlaylist;
    private SimpleExoPlayer player;
    View v;
    RelativeLayout containerPlayer;

    private MegaApiAndroid megaApi;

    ArrayList<MegaNode> nodes;
    ArrayList<Long> handles = new ArrayList<>();
    Long parentHandle;
    String parenPath;
    ArrayList<MegaOffline> offNodes;
    ArrayList<File> zipFiles;
    int adapterType;

    PlaylistFragment playlistFragment;

    boolean searchOpen = false;

    ImageButton previousButton;
    ImageButton nextButton;

    public static PlaylistFragment newInstance() {
        log("newInstance");
        PlaylistFragment fragment = new PlaylistFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);

        if (megaApi == null) {
            if (((AudioVideoPlayerLollipop) context).isFolderLink()){
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApiFolder();
            }
            else {
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
            }
        }

        player = ((AudioVideoPlayerLollipop) context).getPlayer();
        playlistFragment = this;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        outMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(outMetrics);

        if (megaApi == null){
            if (((AudioVideoPlayerLollipop) context).isFolderLink()){
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApiFolder();
            }
            else {
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
            }
        }

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = ((AudioVideoPlayerLollipop)context).getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(context, R.color.dark_primary_color_secondary));
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
            ((AudioVideoPlayerLollipop)context).requestWindowFeature(Window.FEATURE_NO_TITLE);
            ((AudioVideoPlayerLollipop)context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (player == null){
            player = ((AudioVideoPlayerLollipop) context).getPlayer();
        }

        v = inflater.inflate(R.layout.fragment_playlist, container, false);

        recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view_playlist);
        fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

        contentText = (TextView) v.findViewById(R.id.content_text);
        containerContentText = (RelativeLayout) v.findViewById(R.id.content_text_layout);
        simpleExoPlayerViewPlaylist = (SimpleExoPlayerView) v.findViewById(R.id.player_view_playlist);
        containerPlayer  =(RelativeLayout) v.findViewById(R.id.player_layout_container);
        simpleExoPlayerViewPlaylist.setUseController(true);
        simpleExoPlayerViewPlaylist.setPlayer(player);
        simpleExoPlayerViewPlaylist.setControllerAutoShow(false);
        simpleExoPlayerViewPlaylist.setControllerShowTimeoutMs(1999999999);
        simpleExoPlayerViewPlaylist.setControllerHideOnTouch(false);
        simpleExoPlayerViewPlaylist.showController();

        v.findViewById(R.id.exo_content_frame).setVisibility(View.GONE);
        v.findViewById(R.id.exo_overlay).setVisibility(View.GONE);

        previousButton = (ImageButton) v.findViewById(R.id.exo_prev);
        previousButton.setOnTouchListener((AudioVideoPlayerLollipop) context);
        nextButton = (ImageButton) v.findViewById(R.id.exo_next);
        nextButton.setOnTouchListener((AudioVideoPlayerLollipop) context);

        ((AudioVideoPlayerLollipop) context).setPlaylistProgressBar((ProgressBar) v.findViewById(R.id.playlist_progress_bar));

        adapterType = ((AudioVideoPlayerLollipop) context).getAdapterType();
        if (adapterType == Constants.OFFLINE_ADAPTER){
//            OFFLINE CODE
            parenPath = ((AudioVideoPlayerLollipop) context).getPathNavigation();
            offNodes = ((AudioVideoPlayerLollipop) context).getMediaOffList();
            contentText.setText(""+offNodes.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_files, offNodes.size()));
            adapter = new PlayListAdapter(context, this, offNodes, parenPath, recyclerView, adapterType);
        }
        else if (adapterType == Constants.ZIP_ADAPTER) {
            zipFiles = ((AudioVideoPlayerLollipop) context).getZipMediaFiles();
            contentText.setText(""+zipFiles.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_files, zipFiles.size()));
            adapter = new PlayListAdapter(context, this, zipFiles, recyclerView, adapterType);
        }
        else {
            parentHandle = ((AudioVideoPlayerLollipop) context).getParentNodeHandle();
            handles = ((AudioVideoPlayerLollipop) context).getMediaHandles();
            contentText.setText(""+handles.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_files, handles.size()));
            adapter = new PlayListAdapter(context, this, handles, parentHandle, recyclerView, adapterType);
        }

        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setClipToPadding(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter.setItemChecked(((AudioVideoPlayerLollipop) context).getCurrentWindowIndex());
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                boolean scroll;


                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:{
                        if(mLayoutManager.findLastVisibleItemPosition() == adapter.getItemCount()-1){
                            scroll = true;
                        }
                        else {
                            scroll = false;
                        }
                        if (!isSearchOpen()){
                            showController(scroll);
                        }
                        break;
                    }
                    case RecyclerView.SCROLL_STATE_DRAGGING:{
                        hideController();
                        break;
                    }
                }
            }
        });
        scrollTo(((AudioVideoPlayerLollipop) context).getCurrentWindowIndex());
        fastScroller.setRecyclerView(recyclerView);

        visibilityFastScroller();
        ((AudioVideoPlayerLollipop) context).showToolbar();
        if (player != null) {
            player.setPlayWhenReady(((AudioVideoPlayerLollipop) context).playWhenReady);
        }
        String querySearch = ((AudioVideoPlayerLollipop) context).getQuerySearch();
        if (!querySearch.equals("")) {
            aB.setTitle(getString(R.string.action_search) + ": " + querySearch);
            setNodesSearch(querySearch);
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AudioVideoPlayerLollipop) context).showToolbar();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(recyclerView.getLayoutManager()!=null){
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, recyclerView.getLayoutManager().onSaveInstanceState());
        }
    }

    void hideController(){
        containerPlayer.animate().translationY(220).setDuration(200L).withEndAction(new Runnable() {
            @Override
            public void run() {
                containerPlayer.setVisibility(View.GONE);
            }
        }).start();
    }

    public void showController(final boolean scroll){

        containerPlayer.setVisibility(View.VISIBLE);
        containerPlayer.animate().translationY(0).setDuration(200L).start();

        ((AudioVideoPlayerLollipop) context).getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scroll){
                    mLayoutManager.scrollToPosition(mLayoutManager.findLastVisibleItemPosition()+1);
                }
            }
        }, 300L);
    }

    public void itemClick(int position) {
        log("item click position: " + position);
        if (player != null && !((AudioVideoPlayerLollipop) context).isCreatingPlaylist()) {
            player.seekTo(position, 0);
        }
    }

    public void visibilityFastScroller(){
        if(adapter == null){
            fastScroller.setVisibility(View.GONE);
        }else{
            if(adapter.getItemCount() < Constants.MIN_ITEMS_SCROLLBAR){
                fastScroller.setVisibility(View.GONE);
            }else{
                fastScroller.setVisibility(View.VISIBLE);
            }
        }

    }

    public void setNodesSearch(String query){
        if (adapterType == Constants.OFFLINE_ADAPTER){
            ArrayList<MegaOffline> offNodesSearch = new ArrayList<>();
            MegaOffline offNode;
            for (int i=0; i<offNodes.size(); i++){
                offNode = offNodes.get(i);
                if (offNode.getName().toLowerCase().contains(query.toLowerCase())){
                    offNodesSearch.add(offNode);
                }
            }
            adapter.setOffNodes(offNodesSearch);
            if (offNodesSearch.size() == 0){
                containerContentText.setVisibility(View.GONE);
            }
            else {
                containerContentText.setVisibility(View.VISIBLE);
                contentText.setText(""+offNodesSearch.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_files, offNodesSearch.size()));
            }
        }
        else if (adapterType == Constants.ZIP_ADAPTER) {
            ArrayList<File> zipFilesSearch = new ArrayList<>();
            File zipFile;
            for (int i=0; i<zipFiles.size(); i++) {
                zipFile = zipFiles.get(i);
                if (zipFile.getName().toLowerCase().contains(query.toLowerCase())) {
                    zipFilesSearch.add(zipFile);
                }
            }
            adapter.setZipNodes(zipFilesSearch);
            if (zipFilesSearch.size() == 0) {
                containerContentText.setVisibility(View.GONE);
            }
            else {
                containerContentText.setVisibility(View.VISIBLE);
                contentText.setText(""+zipFilesSearch.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_files, zipFilesSearch.size()));
            }
        }
        else {
            ArrayList<MegaNode> nodesSearch = new ArrayList<>();
            MegaNode node;
            for (int i=0; i<handles.size(); i++){
                node = megaApi.getNodeByHandle(handles.get(i));
                if (node.getName().toLowerCase().contains(query.toLowerCase())){
                    nodesSearch.add(node);
                }
            }
            adapter.setNodes(nodesSearch);
            if (nodesSearch.size() == 0){
                containerContentText.setVisibility(View.GONE);
            }
            else {
                containerContentText.setVisibility(View.VISIBLE);
                contentText.setText(""+nodesSearch.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_files, nodesSearch.size()));
            }
        }
    }

    @Override
    public void onPause() {
        log("onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        log("onResume");
        super.onResume();
        ((AudioVideoPlayerLollipop) context).setPlaylistProgressBar((ProgressBar) v.findViewById(R.id.playlist_progress_bar));
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        log("onAttach1");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    @Override
    public void onAttach(Context context) {
        log("onAttach2");

        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
    }

    public void scrollTo(final int position) {

        ((AudioVideoPlayerLollipop) context).getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPosition(position);
            }
        }, 350L);
    }

    public ExoPlayer getPlayer(){
        return player;
    }

    public boolean isSearchOpen() {
        return searchOpen;
    }

    public void setSearchOpen(boolean searchOpen) {
        this.searchOpen = searchOpen;
    }

    public static void log(String message) {
        Util.log("PlaylistFragment", message);
    }
}
