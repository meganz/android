package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

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

public class PlaylistFragment extends Fragment {

    DisplayMetrics outMetrics;

    private Toolbar tB;
    private ActionBar aB;
    Context context;

    FastScroller fastScroller;
    LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    PlayListAdapter adapter;
    TextView contentText;
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
    int adapterType;

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
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        player = ((AudioVideoPlayerLollipop) context).getPlayer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        outMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(outMetrics);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = ((AudioVideoPlayerLollipop)context).getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(0);
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
        simpleExoPlayerViewPlaylist = (SimpleExoPlayerView) v.findViewById(R.id.player_view_playlist);
        containerPlayer  =(RelativeLayout) v.findViewById(R.id.player_layout_container);
        simpleExoPlayerViewPlaylist.setUseController(true);
        simpleExoPlayerViewPlaylist.setPlayer(player);
        simpleExoPlayerViewPlaylist.setControllerAutoShow(false);
        simpleExoPlayerViewPlaylist.setControllerShowTimeoutMs(999999999);
        simpleExoPlayerViewPlaylist.setControllerHideOnTouch(false);
        simpleExoPlayerViewPlaylist.showController();

        v.findViewById(R.id.exo_content_frame).setVisibility(View.GONE);
        v.findViewById(R.id.exo_overlay).setVisibility(View.GONE);
//        simpleExoPlayerViewPlaylist.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event){
//
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    if (aB.isShowing()) {
//                        ((AudioVideoPlayerLollipop) context).hideActionStatusBar();
//                    }
//                    else {
//                        ((AudioVideoPlayerLollipop) context).showActionStatusBar();
//                    }
//                }
//                return true;
//            }
//        });

        adapterType = ((AudioVideoPlayerLollipop) context).getAdapterType();
        if (adapterType == Constants.OFFLINE_ADAPTER){
            parenPath = ((AudioVideoPlayerLollipop) context).getPathNavigation();
//            OFFLINE CODE

            adapter = new PlayListAdapter(context, this, offNodes, parenPath, recyclerView);
        }
        else {
            parentHandle = ((AudioVideoPlayerLollipop) context).getParentNodeHandle();
            handles = ((AudioVideoPlayerLollipop) context).getMediaHandles();
            contentText.setText(""+handles.size()+" "+context.getResources().getQuantityString(R.plurals.general_num_files, handles.size()));
            adapter = new PlayListAdapter(context, this, handles, parentHandle, recyclerView);
        }

        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setClipToPadding(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.setAdapter(adapter);
        fastScroller.setRecyclerView(recyclerView);

        visibilityFastScroller();
        player.setPlayWhenReady(true);

        return v;
    }

    void hideController(){
        containerPlayer.animate().translationY(220).setDuration(400L).withEndAction(new Runnable() {
            @Override
            public void run() {
                containerPlayer.setVisibility(View.GONE);
            }
        }).start();
    }

    public void showController(){
        containerPlayer.animate().translationY(0).setDuration(400L).withEndAction(new Runnable() {
            @Override
            public void run() {
                containerPlayer.setVisibility(View.VISIBLE);
            }
        }).start();
    }

    public void itemClick(int position) {
        log("item click position: " + position);


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

    public static void log(String message) {
        Util.log("PlaylistFragment", message);
    }
}
