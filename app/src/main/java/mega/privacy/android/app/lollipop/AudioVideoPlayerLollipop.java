package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.dragger.DraggableView;
import mega.privacy.android.app.components.dragger.ExitViewAnimator;
import mega.privacy.android.app.lollipop.adapters.MegaBrowserLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.managerSections.CameraUploadFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OfflineFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption;
import mega.privacy.android.app.utils.Constants;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static mega.privacy.android.app.lollipop.FileInfoActivityLollipop.TYPE_EXPORT_REMOVE;

public class AudioVideoPlayerLollipop extends PinActivityLollipop implements View.OnClickListener, View.OnTouchListener, MegaGlobalListenerInterface, VideoRendererEventListener, MegaRequestListenerInterface, MegaChatRequestListenerInterface, MegaTransferListenerInterface, DraggableView.DraggableListener{

    int[] screenPosition;
    int mLeftDelta;
    int mTopDelta;
    float mWidthScale;
    float mHeightScale;
    int screenWidth;
    int screenHeight;

    private final String offLineDIR = mega.privacy.android.app.utils.Util.offlineDIR;
    private final String oldMKFile = mega.privacy.android.app.utils.Util.oldMKFile;

    static AudioVideoPlayerLollipop audioVideoPlayerLollipop;
    
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH = null;
    MegaPreferences prefs = null;

    private AlertDialog alertDialogTransferOverquota;

    Handler handler;
    boolean isFolderLink = false;
    private SimpleExoPlayerView simpleExoPlayerView;
    private SimpleExoPlayer player;
    private Uri uri;
    private TextView exoPlayerName;
    private ProgressBar progressBar;
    private RelativeLayout playPauseButton;
    private RelativeLayout containerControls;

    private AppBarLayout appBarLayout;
    private Toolbar tB;
    private ActionBar aB;

    private MenuItem shareMenuItem;
    private MenuItem downloadMenuItem;
    private MenuItem propertiesMenuItem;
    private MenuItem chatMenuItem;
    private MenuItem getlinkMenuItem;
    private MenuItem renameMenuItem;
    private MenuItem moveMenuItem;
    private MenuItem copyMenuItem;
    private MenuItem moveToTrashMenuItem;
    private MenuItem removeMenuItem;
    private MenuItem removelinkMenuItem;
    private MenuItem loopMenuItem;
    public MenuItem searchMenuItem;

    private RelativeLayout audioVideoPlayerContainer;
    private RelativeLayout playerLayout;
    
    private RelativeLayout audioContainer;
    private long handle = -1;
    int countChat = 0;
    int successSent = 0;
    int errorSent = 0;
    boolean transferOverquota = false;

    private boolean video = false;
    private ProgressDialog statusDialog = null;
    private String fileName = null;
    private long currentTime;

    private RelativeLayout containerAudioVideoPlayer;

    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    private boolean isUrl;

    ArrayList<Long> handleListM = new ArrayList<Long>();
    
    private int currentPosition = 0;
    private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
    private long parentNodeHandle = -1;
    private int adapterType = 0;
    private ArrayList<Long> mediaHandles;
    private ArrayList<MegaOffline> offList;
    private ArrayList<MegaOffline> mediaOffList;
    private ArrayList<Uri> mediaUris;
    private boolean isOffLine = false;
    private boolean isPlayList;
    private int size = 0;

    private String downloadLocationDefaultPath = "";
    private boolean renamed = false;
    private boolean isOffline;
    private String path;
    private String pathNavigation;

    private DraggableView draggableView;
    private ImageView ivShadow;
    NodeController nC;
    private android.support.v7.app.AlertDialog downloadConfirmationDialog;
    private DisplayMetrics outMetrics;

    private boolean fromShared = false;
    int accountType;
    int typeExport = -1;
    private AlertDialog renameDialog;
    String regex = "[*|\\?:\"<>\\\\\\\\/]";
    boolean moveToRubbish = false;
    ProgressDialog moveToTrashStatusDialog;
    private boolean loop = false;
    private boolean isVideo = true;
    private boolean isMP4 = false;

    private ImageButton nextButton;
    private ImageButton previousButton;
    private ImageButton playList;
    private int numErrors = 0;

    private FrameLayout fragmentContainer;
    private boolean onPlaylist = false;
//    public LoopingMediaSource loopingMediaSource;
    public ConcatenatingMediaSource concatenatingMediaSource;
    PlaylistFragment playlistFragment;
    private ProgressBar playlistProgressBar;
    int currentWindowIndex;
    boolean onTracksChange = false;
    public String querySearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        setContentView(R.layout.activity_audiovideoplayer);

        audioVideoPlayerLollipop = this;
        getDownloadLocation();

        draggableView.setViewAnimator(new ExitViewAnimator());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent intent = getIntent();
        if (intent == null){
            log("intent null");
            finish();
            return;
        }

        if (savedInstanceState != null) {
            currentTime = savedInstanceState.getLong("currentTime");
            fileName = savedInstanceState.getString("fileName");
            handle = savedInstanceState.getLong("handle");
            uri = Uri.parse(savedInstanceState.getString("uri"));
            renamed = savedInstanceState.getBoolean("renamed");
            loop = savedInstanceState.getBoolean("loop");
            currentPosition = savedInstanceState.getInt("currentPosition");
            onPlaylist = savedInstanceState.getBoolean("onPlaylist");
            size = savedInstanceState.getInt("size");
            currentWindowIndex = savedInstanceState.getInt("currentWindowIndex");
            querySearch = savedInstanceState.getString("querySearch");
        }
        else {
            onPlaylist = false;
            currentTime = 0;
            currentWindowIndex = 0;
            accountType = intent.getIntExtra("typeAccount", MegaAccountDetails.ACCOUNT_TYPE_FREE);
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                handle = bundle.getLong("HANDLE");
                fileName = bundle.getString("FILENAME");
            }
            currentPosition = intent.getIntExtra("position", 0);
        }
        if (!renamed) {
            uri = intent.getData();
            if (uri == null) {
                log("uri null");
                finish();
                return;
            }
        }
        isVideo = MimeTypeList.typeForName(fileName).isVideoReproducible();
        fromShared = intent.getBooleanExtra("fromShared", false);
        path = intent.getStringExtra("path");
        adapterType = getIntent().getIntExtra("adapterType", 0);
        if (adapterType == Constants.OFFLINE_ADAPTER){
            isOffline = true;
            pathNavigation = intent.getStringExtra("pathNavigation");
        }
        else {
            isOffline = false;
            pathNavigation = null;
        }

        isFolderLink = intent.getBooleanExtra("isFolderLink", false);
        isPlayList = intent.getBooleanExtra("isPlayList", true);
        orderGetChildren = intent.getIntExtra("orderGetChildren", MegaApiJava.ORDER_DEFAULT_ASC);
        parentNodeHandle = intent.getLongExtra("parentNodeHandle", -1);
        adapterType = intent.getIntExtra("adapterType", 0);


        log("uri: "+uri);

        if (uri.toString().contains("http://")){
            isUrl = true;
        }
        else {
            isUrl = false;
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        screenHeight = outMetrics.heightPixels;
        screenWidth = outMetrics.widthPixels;

        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

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

        tB = (Toolbar) findViewById(R.id.call_toolbar);
        if (tB == null) {
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        log("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);



        exoPlayerName = (TextView) findViewById(R.id.exo_name_file);

        if (fileName != null) {
            aB.setTitle(" ");
            exoPlayerName.setText(fileName);
            setTitle(fileName);
        }
        else {
            aB.setTitle(" ");
            exoPlayerName.setText(fileName);
            setTitle(getFileName(uri));
        }

        containerAudioVideoPlayer = (RelativeLayout) findViewById(R.id.audiovideoplayer_container);
        playerLayout = (RelativeLayout) findViewById(R.id.player_layout);

        audioContainer = (RelativeLayout) findViewById(R.id.audio_container);
        audioContainer.setVisibility(View.GONE);
        
        audioVideoPlayerContainer = (RelativeLayout) findViewById(R.id.audiovideoplayer_container);
        progressBar = (ProgressBar) findViewById(R.id.full_video_viewer_progress_bar);
        playPauseButton = (RelativeLayout) findViewById(R.id.play_pause_button);
        containerControls = (RelativeLayout) findViewById(R.id.container_exo_controls);
        previousButton = (ImageButton) findViewById(R.id.exo_prev);
        previousButton.setOnTouchListener(this);
        nextButton = (ImageButton) findViewById(R.id.exo_next);
        nextButton.setOnTouchListener(this);
        playList = (ImageButton) findViewById(R.id.exo_play_list);
        playList.setOnClickListener(this);
        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        fragmentContainer.setVisibility(View.GONE);

        handler = new Handler();

        MegaApplication app = (MegaApplication)getApplication();
        if (isFolderLink){
            megaApi = app.getMegaApiFolder();
        }
        else{
            megaApi = app.getMegaApi();
        }

        if(megaApi==null||megaApi.getRootNode()==null){
            log("Refresh session - sdk");
            Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
            intentLogin.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
            intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intentLogin);
            finish();
            return;
        }

        if(mega.privacy.android.app.utils.Util.isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
            }

            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
                log("Refresh session - karere");
                Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
                intentLogin.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
                intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentLogin);
                finish();
                return;
            }
        }

        log("Overquota delay: "+megaApi.getBandwidthOverquotaDelay());
        if(megaApi.getBandwidthOverquotaDelay()>0){
            if(alertDialogTransferOverquota==null){
                showTransferOverquotaDialog();
            }
            else {
                if (!(alertDialogTransferOverquota.isShowing())) {
                    showTransferOverquotaDialog();
                }
            }
        }

        log("Add transfer listener");
        megaApi.addTransferListener(this);
        megaApi.addGlobalListener(this);

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        }

        if (megaApi.httpServerIsRunning() == 0) {
            megaApi.httpServerStart();
        }

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        if(mi.totalMem>Constants.BUFFER_COMP){
            log("Total mem: "+mi.totalMem+" allocate 32 MB");
            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
        }
        else{
            log("Total mem: "+mi.totalMem+" allocate 16 MB");
            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
        }

        MegaNode parentNode;

        if (isPlayList){
            if (adapterType == Constants.OFFLINE_ADAPTER){
                //OFFLINE
                log("OFFLINE_ADAPTER");
                isOffLine = true;
                offList = new ArrayList<>();
                String pathNavigation = intent.getStringExtra("pathNavigation");
                log("PATHNAVIGATION: " + pathNavigation);
                offList=dbH.findByPath(pathNavigation);
                log ("offList.size() = " + offList.size());

                for(int i=0; i<offList.size();i++){
                    MegaOffline checkOffline = offList.get(i);
                    File offlineDirectory = null;
                    if(checkOffline.getOrigin()==MegaOffline.INCOMING){
                        log("isIncomingOffline");

                        if (Environment.getExternalStorageDirectory() != null){
                            offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + offLineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
                            log("offlineDirectory: "+offlineDirectory);
                        }
                        else{
                            offlineDirectory = getFilesDir();
                        }
                    }
                    else if(checkOffline.getOrigin()==MegaOffline.INBOX){
                        if (Environment.getExternalStorageDirectory() != null){
                            offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + offLineDIR + "/in/" + checkOffline.getPath()+checkOffline.getName());
                            log("offlineDirectory: "+offlineDirectory);
                        }
                        else{
                            offlineDirectory = getFilesDir();
                        }
                    }
                    else{
                        log("NOT isIncomingOffline");
                        if (Environment.getExternalStorageDirectory() != null){
                            offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + offLineDIR + checkOffline.getPath()+checkOffline.getName());
                        }
                        else{
                            offlineDirectory = getFilesDir();
                        }
                    }

                    if(offlineDirectory!=null){
                        if (!offlineDirectory.exists()){
                            log("Path to remove B: "+(offList.get(i).getPath()+offList.get(i).getName()));
                            //dbH.removeById(offList.get(i).getId());
                            offList.remove(i);
                            i--;
                        }
                    }
                }

                if (offList != null){
                    if(!offList.isEmpty()) {
                        MegaOffline lastItem = offList.get(offList.size()-1);
                        if(!(lastItem.getHandle().equals("0"))){
                            String path = Environment.getExternalStorageDirectory().getAbsolutePath()+oldMKFile;
                            log("Export in: "+path);
                            File file= new File(path);
                            if(file.exists()){
                                MegaOffline masterKeyFile = new MegaOffline("0", path, "MEGARecoveryKey.txt", 0, "0", 0, "0");
                                offList.add(masterKeyFile);
                            }
                        }
                    }
                    else{
                        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+oldMKFile;
                        log("Export in: "+path);
                        File file= new File(path);
                        if(file.exists()){
                            MegaOffline masterKeyFile = new MegaOffline("0", path, "MEGARecoveryKey.txt", 0, "0", 0, "0");
                            offList.add(masterKeyFile);
                        }
                    }
                }

                if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
                    sortByNameDescending();
                }
                else{
                    sortByNameAscending();
                }

                if (offList.size() > 0){

                    mediaOffList = new ArrayList<>();
                    int mediaPosition = -1;
                    for (int i=0;i<offList.size();i++){
                        if ((MimeTypeList.typeForName(offList.get(i).getName()).isVideoReproducible() && !MimeTypeList.typeForName(offList.get(i).getName()).isVideoNotSupported())|| MimeTypeList.typeForName(offList.get(i).getName()).isAudio()){
                            mediaOffList.add(offList.get(i));
                            mediaPosition++;
                            if (i == currentPosition){
                                currentPosition = mediaPosition;
                            }
                        }
                    }

                    if (currentPosition >= mediaOffList.size()){
                        currentPosition = 0;
                    }
                }
                size = mediaOffList.size();
            }
            else if(adapterType == Constants.SEARCH_ADAPTER){
                isOffLine = false;
                mediaHandles = new ArrayList<>();

                ArrayList<MegaNode> nodes = null;
                if (parentNodeHandle == -1){
                    String query = intent.getStringExtra("searchQuery");
                    nodes = megaApi.search(query);
                }
                else{
                    parentNode =  megaApi.getNodeByHandle(parentNodeHandle);
                    nodes = megaApi.getChildren(parentNode, orderGetChildren);
                }

                int mediaNumber = 0;
                for (int i=0;i<nodes.size();i++){
                    MegaNode n = nodes.get(i);
                    if ((MimeTypeList.typeForName(n.getName()).isVideoReproducible() && !MimeTypeList.typeForName(n.getName()).isVideoNotSupported()) || MimeTypeList.typeForName(n.getName()).isAudio()){
                        mediaHandles.add(n.getHandle());
                        if (i == currentPosition){
                            currentPosition = mediaNumber;
                        }
                        mediaNumber++;
                    }
                }

                if(mediaHandles.size() == 0){
                    finish();
                    return;
                }

                if(currentPosition >= mediaHandles.size()){
                    currentPosition = 0;
                }

                size = mediaHandles.size();
            }
            else{
                isOffLine = false;
                if (parentNodeHandle == -1){

                    switch(adapterType){
                        case Constants.FILE_BROWSER_ADAPTER:{
                            parentNode = megaApi.getRootNode();
                            break;
                        }
                        case Constants.RUBBISH_BIN_ADAPTER:{
                            parentNode = megaApi.getRubbishNode();
                            break;
                        }
                        case Constants.SHARED_WITH_ME_ADAPTER:{
                            parentNode = megaApi.getInboxNode();
                            break;
                        }
                        case Constants.FOLDER_LINK_ADAPTER:{
                            parentNode = megaApi.getRootNode();
                            break;
                        }
                        default:{
                            parentNode = megaApi.getRootNode();
                            break;
                        }
                    }

                }
                else{
                    parentNode = megaApi.getNodeByHandle(parentNodeHandle);
                }

                mediaHandles = new ArrayList<>();
                ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);

                int mediaNumber = 0;
                for (int i=0;i<nodes.size();i++){
                    MegaNode n = nodes.get(i);
                    if ((MimeTypeList.typeForName(n.getName()).isVideoReproducible() && !MimeTypeList.typeForName(n.getName()).isVideoNotSupported()) || MimeTypeList.typeForName(n.getName()).isAudio()){
                        mediaHandles.add(n.getHandle());
                        if (i == currentPosition){
                            currentPosition = mediaNumber;
                        }
                        mediaNumber++;
                    }
                }

                if(mediaHandles.size() == 0)
                {
                    finish();
                    return;
                }

                if(currentPosition >= mediaHandles.size())
                {
                    currentPosition = 0;
                }

                ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                size = mediaHandles.size();
            }

            if (size > 1) {
                findSelected();
                playList.setVisibility(View.VISIBLE);
            }
            else {
                playList.setVisibility(View.GONE);
            }
        }

        if (onPlaylist){
            instantiatePlaylist();
        }

        createPlayer();

        if (savedInstanceState == null){
            ViewTreeObserver observer = simpleExoPlayerView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {

                    simpleExoPlayerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    int[] location = new int[2];
                    simpleExoPlayerView.getLocationOnScreen(location);
                    int[] getlocation = new int[2];
                    getLocationOnScreen(getlocation);
                    if (screenPosition != null){
                        mLeftDelta = getlocation[0] - location[0];
                        mTopDelta = getlocation[1] - location[1];

                        mWidthScale = (float) screenPosition[2] / simpleExoPlayerView.getWidth();
                        mHeightScale = (float) screenPosition[3] / simpleExoPlayerView.getHeight();
                    }
                    else {
                        mLeftDelta = (screenWidth/2) - location[0];
                        mTopDelta = (screenHeight/2) - location[1];

                        mWidthScale = (float) (screenWidth/4) / simpleExoPlayerView.getWidth();
                        mHeightScale = (float) (screenHeight/4) / simpleExoPlayerView.getHeight();
                    }

                    runEnterAnimation();

                    return true;
                }
            });
        }
    }

    void createPlayer (){
        log("createPlayer");
        //Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        //Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        //Create the player
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);

        //Set media controller
        simpleExoPlayerView.setUseController(true);
        simpleExoPlayerView.requestFocus();

        //Bind the player to the view
        simpleExoPlayerView.setPlayer(player);
        simpleExoPlayerView.setControllerAutoShow(false);
        simpleExoPlayerView.setControllerShowTimeoutMs(999999999);
        simpleExoPlayerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event){

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (aB.isShowing()) {
                        hideActionStatusBar();
                    }
                    else {
                        showActionStatusBar();
                    }
                }
                return true;
            }
        });

        //Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
        //Produces DataSource instances through which meida data is loaded
        //DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "android2"), defaultBandwidthMeter);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "android2"), defaultBandwidthMeter);
        //Produces Extractor instances for parsing the media data
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        MediaSource mediaSource = null;
//        loopingMediaSource = null;

        if (isPlayList && size > 1) {
            final List<MediaSource> playlist = new ArrayList<>();
            MediaSource mSource;
            String localPath;
            Uri mediaUri;
            File mediaFile;
            mediaUris = new ArrayList<>();
            if (isOffLine){
                for(int i=0; i<mediaOffList.size(); i++){
                    MegaOffline currentNode = mediaOffList.get(i);
                    if(currentNode.getOrigin()==MegaOffline.INCOMING){
                        String handleString = currentNode.getHandleIncoming();
                        mediaFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + offLineDIR + "/" + handleString + "/"+currentNode.getPath() + "/" + currentNode.getName());
                    }
                    else if(currentNode.getOrigin()==MegaOffline.INBOX){
                        String handleString = currentNode.getHandleIncoming();
                        mediaFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + offLineDIR + "/in/"+currentNode.getPath() + "/" + currentNode.getName());
                    }
                    else{
                        mediaFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + offLineDIR + currentNode.getPath() + "/" + currentNode.getName());
                    }
                    mediaUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                    mediaUris.add(mediaUri);
                    mSource = new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null, null);
                    playlist.add(mSource);
                }
            }
            else {
                MegaNode n;
                for(int i=0; i<mediaHandles.size(); i++){
                    n = megaApi.getNodeByHandle(mediaHandles.get(i));
                    boolean isOnMegaDownloads = false;
                    localPath = mega.privacy.android.app.utils.Util.getLocalFile(this, n.getName(), n.getSize(), downloadLocationDefaultPath);
                    File f = new File(downloadLocationDefaultPath, n.getName());
                    if(f.exists() && (f.length() == n.getSize())){
                        isOnMegaDownloads = true;
                    }
                    if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(n).equals(megaApi.getFingerprint(localPath))))){
                        mediaFile = new File(localPath);
                        mediaUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                        mediaUris.add(mediaUri);
                        mSource = new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null, null);
                    }
                    else {
                        mediaUri = Uri.parse(megaApi.httpServerGetLocalLink(n));
                        mediaUris.add(mediaUri);
                        mSource = new ExtractorMediaSource (mediaUri, dataSourceFactory, extractorsFactory, null, null);
                    }
                    playlist.add(mSource);
                }
            }

            concatenatingMediaSource = new ConcatenatingMediaSource(playlist.toArray(new MediaSource[playlist.size()]));
            player.prepare(concatenatingMediaSource);
//            loopingMediaSource = new LoopingMediaSource(concatenatingMediaSource);
//            player.prepare(loopingMediaSource);


        }
        else {
            mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
            player.prepare(mediaSource);
        }

//        final LoopingMediaSource finalLoopingMediaSource = loopingMediaSource;
        final ConcatenatingMediaSource finalConcatenatingMediaSource = concatenatingMediaSource;
        final MediaSource finalMediaSource = mediaSource;
        //MediaSource mediaSource = new HlsMediaSource(uri, dataSourceFactory, handler, null);
        //DashMediaSource mediaSource = new DashMediaSource(uri, dataSourceFactory, new DefaultDashChunkSource.Factory(dataSourceFactory), null, null);

        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                log("onTimelineChanged");
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                log("onTracksChanged");

                int previousIndex = currentWindowIndex;
                if (size > 1) {
                    currentWindowIndex = player.getCurrentWindowIndex();
                    if (currentWindowIndex == previousIndex && onTracksChange && !loop) {
                        currentWindowIndex++;
                    }

                    if (isOffLine){
                        MegaOffline n = mediaOffList.get(currentWindowIndex);
                        fileName = n.getName();
                        handle = Long.parseLong(n.getHandle());
                    }
                    else {
                        MegaNode n = megaApi.getNodeByHandle(mediaHandles.get(currentWindowIndex));
                        fileName = n.getName();
                        handle = n.getHandle();
                    }

                    isVideo = MimeTypeList.typeForName(fileName).isVideoReproducible();
                    String extension = fileName.substring(fileName.length()-3, fileName.length());
                    log("Extension: "+extension);
                    if (extension.equals("mp4")){
                        isMP4 = true;
                    }
                    else {
                        isMP4 = false;
                    }
                    exoPlayerName.setText(fileName);
                    uri = mediaUris.get(currentWindowIndex);
                    if (uri.toString().contains("http://")){
                        isUrl = true;
                    }
                    else {
                        isUrl = false;
                    }
                    supportInvalidateOptionsMenu();
                }
                updateScrollPosition();

                if (onPlaylist) {
                    if (playlistFragment != null && playlistFragment.isAdded()){
                        if (currentWindowIndex < playlistFragment.adapter.getItemCount() && currentWindowIndex >= 0){
                            playlistFragment.adapter.setItemChecked(currentWindowIndex);
                            playlistFragment.mLayoutManager.scrollToPosition(currentWindowIndex);
                            playlistFragment.adapter.notifyDataSetChanged();
                        }
                    }
                }
                onTracksChange = true;
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
                log("onLoadingChanged");

                if (video){
                    audioContainer.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                log("onPlayerStateChanged: "+playbackState);

                if (playbackState == Player.STATE_BUFFERING){
                    audioContainer.setVisibility(View.GONE);
                    if (onPlaylist){
                        playlistProgressBar.setVisibility(View.VISIBLE);
                    }
                    else {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    if (onPlaylist){
                        if (playlistProgressBar != null){
                            playlistProgressBar.setVisibility(View.GONE);
                        }
                        if (playlistFragment != null && playlistFragment.isAdded()){
                            if (playlistFragment.adapter != null){
                                playlistFragment.adapter.notifyDataSetChanged();
                            }
                        }
                    }
                    else {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (isVideo) {
                        if ((isMP4 && video) || !isMP4){
                            audioContainer.setVisibility(View.GONE);
                        }
                        else {
                            audioContainer.setVisibility(View.VISIBLE);
                        }
                    }
                    else {
                        audioContainer.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
                log("onRepeatModeChanged");
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                log("onShuffleModeEnabledChanged");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                log("onPlayerError");
                numErrors++;
                player.stop();
                if (numErrors <= 2){
                    if (isPlayList && size > 1){
//                        player.prepare(finalLoopingMediaSource);
                        player.prepare(finalConcatenatingMediaSource);
                    }
                    else {
                        player.prepare(finalMediaSource);
                    }
                    player.setPlayWhenReady(true);
                }
                else {
                    showErrorDialog();
                }

            }

            @Override
            public void onPositionDiscontinuity(int reason) {
                log("onPositionDiscontinuity");
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                log("onPlaybackParametersChanged");
            }

            @Override
            public void onSeekProcessed() {
                log("onSeekProcessed");
            }
        });
        numErrors = 0;
        player.setPlayWhenReady(true);
        player.seekTo(currentWindowIndex, currentTime);
        player.setVideoDebugListener(this);
        onTracksChange = false;
    }

    void showErrorDialog() {
        log("showErrorDialog: Error open video file");
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setCancelable(false);
        String accept = getResources().getString(R.string.cam_sync_ok).toUpperCase();
        builder.setMessage(R.string.corrupt_video_dialog_text)
                .setPositiveButton(accept, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
        numErrors = 0;
    }

    public void updateCurrentImage(){
        setImageDragVisibility(View.VISIBLE);
        ImageView image = null;
        if (adapterType == Constants.OFFLINE_ADAPTER){
            String name = mediaOffList.get(currentPosition).getName();
            for (int i=0; i<offList.size(); i++){
                log("Name: "+name+" mOfflist name: "+offList.get(i).getName());
                if (offList.get(i).getName().equals(name)){
                    image = getImageView(i);
                    break;
                }
            }
        }
        else if (adapterType == Constants.PHOTO_SYNC_ADAPTER || adapterType == Constants.SEARCH_BY_ADAPTER){
            if (CameraUploadFragmentLollipop.adapterList != null){
                ArrayList<CameraUploadFragmentLollipop.PhotoSyncHolder> listNodes = CameraUploadFragmentLollipop.nodesArray;
                for (int i=0; i<listNodes.size(); i++){
                    if (listNodes.get(i).getHandle() == handle){
                        image = getImageView(i);
                        break;
                    }
                }
            }
            else if (CameraUploadFragmentLollipop.adapterGrid != null){
                ArrayList<MegaMonthPicLollipop> listNodes = CameraUploadFragmentLollipop.monthPics;
                ArrayList<Long> handles;
                int count = 0;
                boolean found = false;
                for (int i=0; i<listNodes.size(); i++){
                    handles = listNodes.get(i).getNodeHandles();
                    for (int j=0; j<handles.size(); j++){
                        count++;
                        String h1 = handles.get(j).toString();
                        String h2 = Long.toString(handle);
                        if (h1.equals(h2)){
                            image = getImageView(count);
                            found = true;
                            break;
                        }
                    }
                    count++;
                    if (found){
                        break;
                    }
                }
            }
        }
        else if (adapterType == Constants.SEARCH_ADAPTER){
            ArrayList<MegaNode> listNodes = megaApi.search(ManagerActivityLollipop.searchQuery);
            for (int i=0; i<listNodes.size(); i++){
                if (listNodes.get(i).getHandle() == handle){
                    image = getImageView(i);
                    break;
                }
            }
        }
        else {
            MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(handle));
            ArrayList<MegaNode> listNodes = megaApi.getChildren(parentNode);
            for (int i=0; i<listNodes.size(); i++){
                if (listNodes.get(i).getHandle() == handle){
                    image = getImageView(i);
                    break;
                }
            }
        }
        if (image != null){
            int[] position = new int[2];
            image.getLocationOnScreen(position);
            screenPosition[0] = (image.getWidth() / 2) + position[0];
            screenPosition[1] = (image.getHeight() / 2) + position[1];
            screenPosition[2] = image.getWidth();
            screenPosition[3] = image.getHeight();
            draggableView.setScreenPosition(screenPosition);
        }
    }

    public ImageView getImageView (int i) {
        ImageView image = null;

        if (adapterType == Constants.RUBBISH_BIN_ADAPTER){
            if (RubbishBinFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                View v = RubbishBinFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null){
                    RubbishBinFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
                }
            }
            else {
                View v = RubbishBinFragmentLollipop.gridLayoutManager.findViewByPosition(i);
                if (v != null) {
                    RubbishBinFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_grid_thumbnail);
                }
            }
            image = RubbishBinFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.INBOX_ADAPTER){
            if (InboxFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                View v = InboxFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    InboxFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
                }
            }
            else {
                View v = InboxFragmentLollipop.gridLayoutManager.findViewByPosition(i);
                if (v != null) {
                    InboxFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_grid_thumbnail);
                }
            }
            image = InboxFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.INCOMING_SHARES_ADAPTER){
            if (IncomingSharesFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                View v = IncomingSharesFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    IncomingSharesFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
                }
            }
            else {
                View v = IncomingSharesFragmentLollipop.gridLayoutManager.findViewByPosition(i);
                if (v != null) {
                    IncomingSharesFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_grid_thumbnail);
                }
            }
            image = IncomingSharesFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.OUTGOING_SHARES_ADAPTER){
            if (OutgoingSharesFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                View v = OutgoingSharesFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    OutgoingSharesFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
                }
            }
            else {
                View v = OutgoingSharesFragmentLollipop.gridLayoutManager.findViewByPosition(i);
                if (v != null) {
                    OutgoingSharesFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_grid_thumbnail);
                }
            }
            image = OutgoingSharesFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.CONTACT_FILE_ADAPTER){
            View v = ContactFileListFragmentLollipop.mLayoutManager.findViewByPosition(i);
            if (v != null){
                ContactFileListFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
            }
            image = ContactFileListFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.FOLDER_LINK_ADAPTER){
            View v = FolderLinkActivityLollipop.mLayoutManager.findViewByPosition(i);
            if (v != null) {
                FolderLinkActivityLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
            }
            image = FolderLinkActivityLollipop.imageDrag;
        }
        else if (adapterType == Constants.SEARCH_ADAPTER){
            if (SearchFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                View v = SearchFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    SearchFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
                }
            }
            else {
                View v = SearchFragmentLollipop.gridLayoutManager.findViewByPosition(i);
                if (v != null){
                    SearchFragmentLollipop.imageDrag = (ImageView)v.findViewById(R.id.file_grid_thumbnail);
                }
            }
            image = SearchFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.FILE_BROWSER_ADAPTER){
            if (FileBrowserFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                View v = FileBrowserFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    FileBrowserFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_list_thumbnail);
                }
            }
            else {
                View v = FileBrowserFragmentLollipop.gridLayoutManager.findViewByPosition(i);
                if (v != null) {
                    FileBrowserFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.file_grid_thumbnail);
                }
            }
            image = FileBrowserFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.PHOTO_SYNC_ADAPTER || adapterType == Constants.SEARCH_BY_ADAPTER){
            if (CameraUploadFragmentLollipop.adapterList != null){
                View v = CameraUploadFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    CameraUploadFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.photo_sync_list_thumbnail);
                }
            }
            else if (CameraUploadFragmentLollipop.adapterGrid != null){
                View v = CameraUploadFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    CameraUploadFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.cell_photosync_grid_title_thumbnail);
                }
            }
            image = CameraUploadFragmentLollipop.imageDrag;
        }
        else if (adapterType == Constants.OFFLINE_ADAPTER){
            if (OfflineFragmentLollipop.adapter.getAdapterType() == MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                View v = OfflineFragmentLollipop.mLayoutManager.findViewByPosition(i);
                if (v != null) {
                    OfflineFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.offline_list_thumbnail);
                }
            }
            else {
                View v = OfflineFragmentLollipop.gridLayoutManager.findViewByPosition(i);
                if (v != null) {
                    OfflineFragmentLollipop.imageDrag = (ImageView) v.findViewById(R.id.offline_grid_thumbnail);
                }
            }
            image = OfflineFragmentLollipop.imageDrag;
        }

        return image;
    }

    public void updateScrollPosition(){
        if (adapterType == Constants.OFFLINE_ADAPTER){
            String name = mediaOffList.get(currentPosition).getName();

            for (int i=0; i<offList.size(); i++){
                log("Name: "+name+" mOfflist name: "+offList.get(i).getName());
                if (offList.get(i).getName().equals(name)){
                    scrollToPosition(i);
                    break;
                }
            }
        }
        else if (adapterType == Constants.PHOTO_SYNC_ADAPTER || adapterType == Constants.SEARCH_BY_ADAPTER){
            if (CameraUploadFragmentLollipop.adapterList != null){
                ArrayList<CameraUploadFragmentLollipop.PhotoSyncHolder> listNodes = CameraUploadFragmentLollipop.nodesArray;
                for (int i=0; i<listNodes.size(); i++){
                    if (listNodes.get(i).getHandle() == handle){
                        scrollToPosition(i);
                        break;
                    }
                }
            }
            else if (CameraUploadFragmentLollipop.adapterGrid != null){
                ArrayList<MegaMonthPicLollipop> listNodes = CameraUploadFragmentLollipop.monthPics;
                ArrayList<Long> handles;
                int count = 0;
                boolean found = false;
                for (int i=0; i<listNodes.size(); i++){
                    handles = listNodes.get(i).getNodeHandles();
                    for (int j=0; j<handles.size(); j++){
                        count++;
                        String h1 = handles.get(j).toString();
                        String h2 = Long.toString(handle);
                        if (h1.equals(h2)){
                            scrollToPosition(count);
                            found = true;
                            break;
                        }
                    }
                    count++;
                    if (found){
                        break;
                    }
                }
            }
        }
        else if (adapterType == Constants.SEARCH_ADAPTER){
            ArrayList<MegaNode> listNodes = megaApi.search(ManagerActivityLollipop.searchQuery);

            for (int i=0; i<listNodes.size(); i++){
                if (listNodes.get(i).getHandle() == handle){
                    scrollToPosition(i);
                    break;
                }
            }
        }
        else {
            MegaNode parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(handle));
            ArrayList<MegaNode> listNodes = megaApi.getChildren(parentNode);

            for (int i=0; i<listNodes.size(); i++){
                if (listNodes.get(i).getHandle() == handle){
                    scrollToPosition(i);
                    break;
                }
            }
        }
    }

    void scrollToPosition (int i) {
        if (adapterType == Constants.RUBBISH_BIN_ADAPTER){
            if (RubbishBinFragmentLollipop.adapter != null && RubbishBinFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                if (RubbishBinFragmentLollipop.mLayoutManager != null && RubbishBinFragmentLollipop.adapter.getItem(i) != null){
                    RubbishBinFragmentLollipop.mLayoutManager.scrollToPosition(i);
                }
            }
            else {
                if (RubbishBinFragmentLollipop.gridLayoutManager != null && RubbishBinFragmentLollipop.adapter.getItem(i) != null) {
                    RubbishBinFragmentLollipop.gridLayoutManager.scrollToPosition(i);
                }
            }
        }
        else if (adapterType == Constants.INBOX_ADAPTER){
            if (InboxFragmentLollipop.adapter != null && InboxFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                if (InboxFragmentLollipop.mLayoutManager != null && InboxFragmentLollipop.adapter.getItem(i) != null) {
                    InboxFragmentLollipop.mLayoutManager.scrollToPosition(i);
                }
            }
            else {
                if (InboxFragmentLollipop.gridLayoutManager != null && InboxFragmentLollipop.adapter.getItem(i) != null) {
                    InboxFragmentLollipop.gridLayoutManager.scrollToPosition(i);
                }
            }
        }
        else if (adapterType == Constants.INCOMING_SHARES_ADAPTER){
            if (IncomingSharesFragmentLollipop.adapter != null && IncomingSharesFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                if (IncomingSharesFragmentLollipop.mLayoutManager != null && IncomingSharesFragmentLollipop.adapter.getItem(i) != null){
                    IncomingSharesFragmentLollipop.mLayoutManager.scrollToPosition(i);
                }
            }
            else {
                if (IncomingSharesFragmentLollipop.gridLayoutManager != null && IncomingSharesFragmentLollipop.adapter.getItem(i) != null) {
                    IncomingSharesFragmentLollipop.gridLayoutManager.scrollToPosition(i);
                }
            }
        }
        else if (adapterType == Constants.OUTGOING_SHARES_ADAPTER){
            if (OutgoingSharesFragmentLollipop.adapter != null && OutgoingSharesFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST) {
                if (OutgoingSharesFragmentLollipop.mLayoutManager != null && OutgoingSharesFragmentLollipop.adapter.getItem(i) != null) {
                    OutgoingSharesFragmentLollipop.mLayoutManager.scrollToPosition(i);
                }
            }
            else {
                if (OutgoingSharesFragmentLollipop.gridLayoutManager != null && OutgoingSharesFragmentLollipop.adapter.getItem(i) != null){
                    OutgoingSharesFragmentLollipop.gridLayoutManager.scrollToPosition(i);
                }
            }
        }
        else if (adapterType == Constants.CONTACT_FILE_ADAPTER){
            if (ContactFileListFragmentLollipop.adapter != null && ContactFileListFragmentLollipop.mLayoutManager != null && ContactFileListFragmentLollipop.adapter.getItem(i) != null) {
                ContactFileListFragmentLollipop.mLayoutManager.scrollToPosition(i);
            }
        }
        else if (adapterType == Constants.FOLDER_LINK_ADAPTER){
            if (FolderLinkActivityLollipop.adapterList != null && FolderLinkActivityLollipop.mLayoutManager != null  && FolderLinkActivityLollipop.adapterList.getItem(i) != null){
                FolderLinkActivityLollipop.mLayoutManager.scrollToPosition(i);
            }
        }
        else if (adapterType == Constants.SEARCH_ADAPTER){
            if (SearchFragmentLollipop.adapter != null && SearchFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                if (SearchFragmentLollipop.mLayoutManager != null && SearchFragmentLollipop.adapter.getItem(i) != null) {
                    SearchFragmentLollipop.mLayoutManager.scrollToPosition(i);
                }
            }
            else {
                if (SearchFragmentLollipop.gridLayoutManager != null && SearchFragmentLollipop.adapter.getItem(i) != null) {
                    SearchFragmentLollipop.gridLayoutManager.scrollToPosition(i);
                }
            }
        }
        else if (adapterType == Constants.FILE_BROWSER_ADAPTER){
            if (FileBrowserFragmentLollipop.adapter != null && FileBrowserFragmentLollipop.adapter.getAdapterType() == MegaBrowserLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                if (FileBrowserFragmentLollipop.mLayoutManager != null && FileBrowserFragmentLollipop.adapter.getItem(i) != null){
                    FileBrowserFragmentLollipop.mLayoutManager.scrollToPosition(i);
                }
            }
            else {
                if (FileBrowserFragmentLollipop.gridLayoutManager != null && FileBrowserFragmentLollipop.adapter.getItem(i) != null){
                    FileBrowserFragmentLollipop.gridLayoutManager.scrollToPosition(i);
                }
            }
        }
        else if (adapterType == Constants.PHOTO_SYNC_ADAPTER || adapterType == Constants.SEARCH_BY_ADAPTER) {
            if ((CameraUploadFragmentLollipop.adapterList != null && CameraUploadFragmentLollipop.adapterList.getItem(i) != null)
                    || (CameraUploadFragmentLollipop.adapterGrid != null && CameraUploadFragmentLollipop.adapterGrid.getItem(i) != null)
                    && CameraUploadFragmentLollipop.mLayoutManager != null ) {
                CameraUploadFragmentLollipop.mLayoutManager.scrollToPosition(i);
            }
        }
        else if (adapterType == Constants.OFFLINE_ADAPTER){
            if (OfflineFragmentLollipop.adapter != null && OfflineFragmentLollipop.adapter.getAdapterType() == MegaOfflineLollipopAdapter.ITEM_VIEW_TYPE_LIST){
                if (OfflineFragmentLollipop.mLayoutManager != null && OfflineFragmentLollipop.adapter.getItem(i) != null) {
                    OfflineFragmentLollipop.mLayoutManager.scrollToPosition(i);
                }
            }
            else {
                if (OfflineFragmentLollipop.gridLayoutManager != null && OfflineFragmentLollipop.adapter.getItem(i) != null) {
                    OfflineFragmentLollipop.gridLayoutManager.scrollToPosition(i);
                }
            }
        }
    }

    public void setImageDragVisibility(int visibility){
        if (adapterType == Constants.RUBBISH_BIN_ADAPTER){
            if (RubbishBinFragmentLollipop.imageDrag != null){
                RubbishBinFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.INBOX_ADAPTER){
            if (InboxFragmentLollipop.imageDrag != null){
                InboxFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.INCOMING_SHARES_ADAPTER){
            if (IncomingSharesFragmentLollipop.imageDrag != null) {
                IncomingSharesFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.OUTGOING_SHARES_ADAPTER){
            if (OutgoingSharesFragmentLollipop.imageDrag != null){
                OutgoingSharesFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.CONTACT_FILE_ADAPTER){
            if (ContactFileListFragmentLollipop.imageDrag != null){
                ContactFileListFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.FOLDER_LINK_ADAPTER){
            if (FolderLinkActivityLollipop.imageDrag != null){
                FolderLinkActivityLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.SEARCH_ADAPTER){
            if (SearchFragmentLollipop.imageDrag != null){
                SearchFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.FILE_BROWSER_ADAPTER){
            if (FileBrowserFragmentLollipop.imageDrag != null){
                FileBrowserFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.PHOTO_SYNC_ADAPTER ||adapterType == Constants.SEARCH_BY_ADAPTER) {
            if (CameraUploadFragmentLollipop.imageDrag != null){
                CameraUploadFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == Constants.OFFLINE_ADAPTER) {
            if (OfflineFragmentLollipop.imageDrag != null){
                OfflineFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
    }

    void getLocationOnScreen(int[] location){
        if (adapterType == Constants.RUBBISH_BIN_ADAPTER){
            if (RubbishBinFragmentLollipop.imageDrag != null) {
                RubbishBinFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.INBOX_ADAPTER){
            if (InboxFragmentLollipop.imageDrag != null){
                InboxFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.INCOMING_SHARES_ADAPTER){
            if (IncomingSharesFragmentLollipop.imageDrag != null) {
                IncomingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.OUTGOING_SHARES_ADAPTER){
            if (OutgoingSharesFragmentLollipop.imageDrag != null) {
                OutgoingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.CONTACT_FILE_ADAPTER){
            if (ContactFileListFragmentLollipop.imageDrag != null) {
                ContactFileListFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.FOLDER_LINK_ADAPTER){
            if (FolderLinkActivityLollipop.imageDrag != null) {
                FolderLinkActivityLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.SEARCH_ADAPTER){
            if (SearchFragmentLollipop.imageDrag != null){
                SearchFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.FILE_BROWSER_ADAPTER){
            if (FileBrowserFragmentLollipop.imageDrag != null){
                FileBrowserFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.PHOTO_SYNC_ADAPTER || adapterType == Constants.SEARCH_BY_ADAPTER){
            if (CameraUploadFragmentLollipop.imageDrag != null) {
                CameraUploadFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == Constants.OFFLINE_ADAPTER){
            if (OfflineFragmentLollipop.imageDrag != null){
                OfflineFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
    }

    public void runEnterAnimation() {
        final long duration = 400;
        if (aB != null && aB.isShowing()) {
            if(tB != null) {
                tB.animate().translationY(-220).setDuration(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                aB.hide();
                            }
                        }).start();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                aB.hide();
            }
        }
        containerAudioVideoPlayer.setBackgroundColor(TRANSPARENT);
        playerLayout.setBackgroundColor(TRANSPARENT);
        appBarLayout.setBackgroundColor(TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            containerAudioVideoPlayer.setElevation(0);
            playerLayout.setElevation(0);
            appBarLayout.setElevation(0);

        }


        simpleExoPlayerView.setPivotX(0);
        simpleExoPlayerView.setPivotY(0);
        simpleExoPlayerView.setScaleX(mWidthScale);
        simpleExoPlayerView.setScaleY(mHeightScale);
        simpleExoPlayerView.setTranslationX(mLeftDelta);
        simpleExoPlayerView.setTranslationY(mTopDelta);

        ivShadow.setAlpha(0);

        simpleExoPlayerView.animate().setDuration(duration).scaleX(1).scaleY(1).translationX(0).translationY(0).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
            @Override
            public void run() {
                showActionStatusBar();
                containerAudioVideoPlayer.setBackgroundColor(BLACK);
                playerLayout.setBackgroundColor(BLACK);
                appBarLayout.setBackgroundColor(BLACK);
            }
        });

        ivShadow.animate().setDuration(duration).alpha(1);
    }

    public void findSelected() {
//        boolean found = false;

        if (isOffLine){
            for (int i=0; i<mediaOffList.size(); i++) {
                if (handle == Long.parseLong(mediaOffList.get(i).getHandle())) {
                    currentWindowIndex = i;
                    break;
                }
            }
        }
        else {
            for (int i=0; i<mediaHandles.size(); i++) {
                if (handle == mediaHandles.get(i)) {
                    currentWindowIndex = i;
                    break;
                }
            }
        }
        if (player != null){
            currentTime = player.getCurrentPosition();
        }

//        if (isOffLine){
//            ArrayList<MegaOffline> tempOffLine = new ArrayList<>();
//            ArrayList<MegaOffline> orderOffLine = new ArrayList<>();
//
//            for (int i=0; i<mediaOffList.size(); i++){
//                if (!found) {
//                    if (handle == Long.parseLong(mediaOffList.get(i).getHandle())) {
//                        found = true;
//                        orderOffLine.add(mediaOffList.get(i));
//                    }
//                    else {
//                        tempOffLine.add(mediaOffList.get(i));
//                    }
//                }
//                else {
//                    orderOffLine.add(mediaOffList.get(i));
//                }
//            }
//            if (tempOffLine.size() > 0) {
//                for (int i=0; i<tempOffLine.size(); i++) {
//                    orderOffLine.add(tempOffLine.get(i));
//                }
//            }
//            mediaOffList.clear();
//            mediaOffList = (ArrayList<MegaOffline>) orderOffLine.clone();
//        }
//        else {
//            ArrayList<Long> tempHandles = new ArrayList<>();
//            ArrayList<Long> orderHandles = new ArrayList<>();
//
//            for (int i=0; i<mediaHandles.size(); i++){
//                if (!found) {
//                    if (handle == mediaHandles.get(i)) {
//                        found = true;
//                        orderHandles.add(mediaHandles.get(i));
//                    }
//                    else {
//                        tempHandles.add(mediaHandles.get(i));
//                    }
//                }
//                else {
//                    orderHandles.add(mediaHandles.get(i));
//                }
//            }
//            if (tempHandles.size() > 0) {
//                for (int i=0; i<tempHandles.size(); i++) {
//                    orderHandles.add(tempHandles.get(i));
//                }
//            }
//            mediaHandles.clear();
//            mediaHandles = (ArrayList<Long>) orderHandles.clone();
//        }
    }

    public void sortByNameDescending(){

        ArrayList<String> foldersOrder = new ArrayList<String>();
        ArrayList<String> filesOrder = new ArrayList<String>();
        ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();


        for(int k = 0; k < offList.size() ; k++) {
            MegaOffline node = offList.get(k);
            if(node.getType().equals("1")){
                foldersOrder.add(node.getName());
            }
            else{
                filesOrder.add(node.getName());
            }
        }


        Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.reverse(foldersOrder);
        Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.reverse(filesOrder);

        for(int k = 0; k < foldersOrder.size() ; k++) {
            for(int j = 0; j < offList.size() ; j++) {
                String name = foldersOrder.get(k);
                String nameOffline = offList.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(offList.get(j));
                }
            }

        }

        for(int k = 0; k < filesOrder.size() ; k++) {
            for(int j = 0; j < offList.size() ; j++) {
                String name = filesOrder.get(k);
                String nameOffline = offList.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(offList.get(j));
                }
            }

        }

        offList.clear();
        offList.addAll(tempOffline);
    }


    public void sortByNameAscending(){
        log("sortByNameAscending");
        ArrayList<String> foldersOrder = new ArrayList<String>();
        ArrayList<String> filesOrder = new ArrayList<String>();
        ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();

        for(int k = 0; k < offList.size() ; k++) {
            MegaOffline node = offList.get(k);
            if(node.getType().equals("1")){
                foldersOrder.add(node.getName());
            }
            else{
                filesOrder.add(node.getName());
            }
        }

        Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
        Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);

        for(int k = 0; k < foldersOrder.size() ; k++) {
            for(int j = 0; j < offList.size() ; j++) {
                String name = foldersOrder.get(k);
                String nameOffline = offList.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(offList.get(j));
                }
            }
        }

        for(int k = 0; k < filesOrder.size() ; k++) {
            for(int j = 0; j < offList.size() ; j++) {
                String name = filesOrder.get(k);
                String nameOffline = offList.get(j).getName();
                if(name.equals(nameOffline)){
                    tempOffline.add(offList.get(j));
                }
            }

        }

        offList.clear();
        offList.addAll(tempOffline);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        currentTime = player.getCurrentPosition();
        outState.putLong("currentTime", currentTime);
        outState.putLong("currentPosition", currentPosition);
        outState.putLong("handle", handle);
        outState.putString("fileName", fileName);
        outState.putString("uri", uri.toString());
        outState.putBoolean("renamed", renamed);
        outState.putBoolean("loop", loop);
        outState.putBoolean("onPlaylist", onPlaylist);
        outState.putInt("currentWindowIndex", currentWindowIndex);
        outState.putInt("size", size);
        outState.putString("querySearch", querySearch);
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    protected void hideActionStatusBar(){
        if (aB != null && aB.isShowing()) {
            if(tB != null) {
                tB.animate().translationY(-220).setDuration(400L)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                aB.hide();
                            }
                        }).start();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            else {
                aB.hide();
            }
            if (video){
                containerControls.animate().translationY(400).setDuration(400L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        simpleExoPlayerView.hideController();
                    }
                }).start();
            }
        }
    }
    protected void showActionStatusBar(){
        if (aB != null && !aB.isShowing()) {
            aB.show();
            if(tB != null) {
                tB.animate().translationY(0).setDuration(400L).start();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            if (video){
                simpleExoPlayerView.showController();
                containerControls.animate().translationY(0).setDuration(400L).start();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_audiovideoplayer, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        if (searchView != null){
            searchView.setIconifiedByDefault(true);
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (playlistFragment != null && playlistFragment.isAdded()){
                    playlistFragment.setSearchOpen(true);
                    playlistFragment.hideController();
                }
                loopMenuItem.setVisible(false);
                querySearch = "";
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (playlistFragment != null && playlistFragment.isAdded()){
                    boolean scroll;
                    playlistFragment.setSearchOpen(false);
                    if(playlistFragment.mLayoutManager.findLastVisibleItemPosition() == playlistFragment.adapter.getItemCount()-1){
                        scroll = true;
                    }
                    else {
                        scroll = false;
                    }
                    playlistFragment.showController(scroll);
                    playlistFragment.adapter.notifyDataSetChanged();
                    if (isOffLine){
                        playlistFragment.contentText.setText(""+mediaOffList.size()+" "+getResources().getQuantityString(R.plurals.general_num_files, mediaOffList.size()));
                    }
                    else {
                        playlistFragment.contentText.setText(""+mediaHandles.size()+" "+getResources().getQuantityString(R.plurals.general_num_files, mediaHandles.size()));
                    }
                }
                loopMenuItem.setVisible(true);
                invalidateOptionsMenu();
                return true;
            }
        });
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (playlistFragment != null && playlistFragment.isAdded()){
                    playlistFragment.setNodesSearch(query);
                }
                querySearch = query;
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                log("Searching by text: "+newText);
                if (playlistFragment != null && playlistFragment.isAdded()){
                    playlistFragment.setNodesSearch(newText);
                }
                querySearch = newText;
                return true;
            }

        });

        shareMenuItem = menu.findItem(R.id.full_video_viewer_share);
        downloadMenuItem = menu.findItem(R.id.full_video_viewer_download);
        chatMenuItem = menu.findItem(R.id.full_video_viewer_chat);
        propertiesMenuItem = menu.findItem(R.id.full_video_viewer_properties);
        getlinkMenuItem = menu.findItem(R.id.full_video_viewer_get_link);
        renameMenuItem = menu.findItem(R.id.full_video_viewer_rename);
        moveMenuItem = menu.findItem(R.id.full_video_viewer_move);
        copyMenuItem = menu.findItem(R.id.full_video_viewer_copy);
        moveToTrashMenuItem = menu.findItem(R.id.full_video_viewer_move_to_trash);
        removeMenuItem = menu.findItem(R.id.full_video_viewer_remove);
        removelinkMenuItem = menu.findItem(R.id.full_video_viewer_remove_link);
        loopMenuItem = menu.findItem(R.id.full_video_viewer_loop);

        if (loop){
            loopMenuItem.setChecked(true);
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        }
        else {
            loopMenuItem.setChecked(false);
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
        }

        if (!onPlaylist){
            searchMenuItem.setVisible(false);

            if (adapterType == Constants.OFFLINE_ADAPTER){
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                shareMenuItem.setVisible(true);
                propertiesMenuItem.setVisible(true);
                downloadMenuItem.setVisible(false);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
            }
            else if(adapterType == Constants.SEARCH_ADAPTER){
                MegaNode node = megaApi.getNodeByHandle(handle);

                if (isUrl){
                    shareMenuItem.setVisible(false);
                    downloadMenuItem.setVisible(true);
                }
                else {
                    downloadMenuItem.setVisible(false);
                    shareMenuItem.setVisible(true);
                }
                if(node.isExported()){
                    removelinkMenuItem.setVisible(true);
                    getlinkMenuItem.setVisible(false);
                }
                else{
                    removelinkMenuItem.setVisible(false);
                    getlinkMenuItem.setVisible(true);
                }

                propertiesMenuItem.setVisible(true);
                renameMenuItem.setVisible(true);
                moveMenuItem.setVisible(true);
                copyMenuItem.setVisible(true);

                if(mega.privacy.android.app.utils.Util.isChatEnabled()){
                    chatMenuItem.setVisible(true);
                }
                else{
                    chatMenuItem.setVisible(false);
                }

                MegaNode parent = megaApi.getNodeByHandle(handle);
                while (megaApi.getParentNode(parent) != null){
                    parent = megaApi.getParentNode(parent);
                }

                if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
                    moveToTrashMenuItem.setVisible(true);
                    removeMenuItem.setVisible(false);
                }
                else{
                    moveToTrashMenuItem.setVisible(false);
                    removeMenuItem.setVisible(true);
                }
            }
            else {
                boolean shareVisible = true;
                shareMenuItem.setVisible(true);

                MegaNode node = megaApi.getNodeByHandle(handle);

                if(adapterType==Constants.CONTACT_FILE_ADAPTER){
                    shareMenuItem.setVisible(false);
                    shareVisible = false;
                }
                else{
                    if(fromShared){
                        shareMenuItem.setVisible(false);
                        shareVisible = false;
                    }
                    if(isFolderLink){
                        shareMenuItem.setVisible(false);
                        shareVisible = false;
                    }
                }
                copyMenuItem.setVisible(true);

                if(node.isExported()){
                    getlinkMenuItem.setVisible(false);
                    removelinkMenuItem.setVisible(true);
                }
                else{
                    if(adapterType==Constants.CONTACT_FILE_ADAPTER){
                        getlinkMenuItem.setVisible(false);
                        removelinkMenuItem.setVisible(false);
                    }
                    else{
                        if(fromShared){
                            removelinkMenuItem.setVisible(false);
                            getlinkMenuItem.setVisible(false);
                        }
                        else{
                            if(isFolderLink){
                                getlinkMenuItem.setVisible(false);
                                removelinkMenuItem.setVisible(false);

                            }
                            else{
                                getlinkMenuItem.setVisible(true);
                                removelinkMenuItem.setVisible(false);
                            }
                        }
                    }
                }
                if(fromShared){
                    removeMenuItem.setVisible(false);
                    chatMenuItem.setVisible(false);

                    node = megaApi.getNodeByHandle(handle);
                    int accessLevel = megaApi.getAccess(node);

                    switch(accessLevel){
                        case MegaShare.ACCESS_OWNER:
                        case MegaShare.ACCESS_FULL:{
                            renameMenuItem.setVisible(true);
                            moveMenuItem.setVisible(true);
                            moveToTrashMenuItem.setVisible(true);
                            break;
                        }
                        case MegaShare.ACCESS_READWRITE:
                        case MegaShare.ACCESS_READ:{
                            renameMenuItem.setVisible(false);
                            moveMenuItem.setVisible(false);
                            moveToTrashMenuItem.setVisible(false);
                            break;
                        }
                    }
                }
                else{
                    if(isFolderLink){
                        propertiesMenuItem.setVisible(false);
                        moveToTrashMenuItem.setVisible(false);
                        removeMenuItem.setVisible(false);
                        renameMenuItem.setVisible(false);
                        moveMenuItem.setVisible(false);
                        copyMenuItem.setVisible(false);
                        chatMenuItem.setVisible(false);
                    }
                    else{
                        propertiesMenuItem.setVisible(true);

                        if(adapterType==Constants.CONTACT_FILE_ADAPTER){
                            removeMenuItem.setVisible(false);
                            node = megaApi.getNodeByHandle(handle);
                            int accessLevel = megaApi.getAccess(node);
                            switch(accessLevel){

                                case MegaShare.ACCESS_OWNER:
                                case MegaShare.ACCESS_FULL:{
                                    renameMenuItem.setVisible(true);
                                    moveMenuItem.setVisible(true);
                                    moveToTrashMenuItem.setVisible(true);
                                    if(mega.privacy.android.app.utils.Util.isChatEnabled()){
                                        chatMenuItem.setVisible(true);
                                    }
                                    else{
                                        chatMenuItem.setVisible(false);
                                    }
                                    break;
                                }
                                case MegaShare.ACCESS_READWRITE:
                                case MegaShare.ACCESS_READ:{
                                    renameMenuItem.setVisible(false);
                                    moveMenuItem.setVisible(false);
                                    moveToTrashMenuItem.setVisible(false);
                                    chatMenuItem.setVisible(false);
                                    break;
                                }
                            }
                        }
                        else{
                            if(mega.privacy.android.app.utils.Util.isChatEnabled()){
                                chatMenuItem.setVisible(true);
                            }
                            else{
                                chatMenuItem.setVisible(false);
                            }
                            renameMenuItem.setVisible(true);
                            moveMenuItem.setVisible(true);

                            node = megaApi.getNodeByHandle(handle);

                            final long handle = node.getHandle();
                            MegaNode parent = megaApi.getNodeByHandle(handle);

                            while (megaApi.getParentNode(parent) != null){
                                parent = megaApi.getParentNode(parent);
                            }

                            if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){

                                moveToTrashMenuItem.setVisible(true);
                                removeMenuItem.setVisible(false);

                            }
                            else{
                                moveToTrashMenuItem.setVisible(false);
                                removeMenuItem.setVisible(true);
                                getlinkMenuItem.setVisible(false);
                                removelinkMenuItem.setVisible(false);
                            }
                        }
                    }
                }
                if (isUrl){
                    downloadMenuItem.setVisible(true);
                    shareMenuItem.setVisible(false);
                }
                else {
                    downloadMenuItem.setVisible(false);
                    if (shareVisible){
                        shareMenuItem.setVisible(true);
                    }
                }
            }
        }
        else {
            searchMenuItem.setVisible(true);
            getlinkMenuItem.setVisible(false);
            removelinkMenuItem.setVisible(false);
            shareMenuItem.setVisible(false);
            propertiesMenuItem.setVisible(false);
            downloadMenuItem.setVisible(false);
            renameMenuItem.setVisible(false);
            moveMenuItem.setVisible(false);
            copyMenuItem.setVisible(false);
            moveToTrashMenuItem.setVisible(false);
            removeMenuItem.setVisible(false);
            chatMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        log("onPrepareOptionsMenu");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                log("onBackPRess");
                onBackPressed();
                break;
            }
            case R.id.full_video_viewer_chat:{
                log("Chat option");
                long[] longArray = new long[1];
                longArray[0] = handle;

                if(nC ==null){
                    nC = new NodeController(this);
                }
                nC.selectChatsToSendNodes(longArray);

                break;
            }
            case R.id.full_video_viewer_share: {
                log("Share option");
                intentToSendFile(uri);
                break;
            }
            case R.id.full_video_viewer_properties: {
                log("Info option");
                showPropertiesActivity();
                break;
            }
            case R.id.full_video_viewer_download: {
                log("Download option");
                downloadFile();
                break;
            }
            case R.id.full_video_viewer_get_link: {
                showGetLinkActivity();
                break;
            }
            case R.id.full_video_viewer_remove_link: {
                showRemoveLink();
                break;
            }
            case R.id.full_video_viewer_rename: {
                showRenameDialog();
                break;
            }
            case R.id.full_video_viewer_move: {
                showMove();
                break;
            }
            case R.id.full_video_viewer_copy: {
                showCopy();
                break;
            }
            case R.id.full_video_viewer_move_to_trash: {
                moveToTrash();
                break;
            }
            case R.id.full_video_viewer_remove: {
                moveToTrash();
                break;
            }
            case R.id.full_video_viewer_loop: {
                if (loopMenuItem.isChecked()){
                    log("Loop NOT checked");
                    loopMenuItem.setChecked(false);
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
                    loop = false;
                }
                else {
                    loopMenuItem.setChecked(true);
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                    log("Loop checked");
                    loop = true;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    void releasePlaylist(){
//        sortBySelected();
        findSelected();
        onPlaylist = false;
        if (player != null){
            player.release();
        }
        playerLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        draggableView.setDraggable(true);

        getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentById(R.id.fragment_container)).commit();

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

        tB.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_black));
        aB.setTitle(" ");

        supportInvalidateOptionsMenu();
        showActionStatusBar();
        createPlayer();
    }

    public void moveToTrash(){
        log("moveToTrash");

        moveToRubbish = false;
        if (!mega.privacy.android.app.utils.Util.isOnline(this)){
            Snackbar.make(containerAudioVideoPlayer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
            return;
        }

        if(isFinishing()){
            return;
        }

        final MegaNode rubbishNode = megaApi.getRubbishNode();

        MegaNode parent = megaApi.getNodeByHandle(handle);
        while (megaApi.getParentNode(parent) != null){
            parent = megaApi.getParentNode(parent);
        }

        if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
            moveToRubbish = true;
        }
        else{
            moveToRubbish = false;
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Check if the node is not yet in the rubbish bin (if so, remove it)

                        if (moveToRubbish){
                            megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, AudioVideoPlayerLollipop.this);
                            ProgressDialog temp = null;
                            try{
                                temp = new ProgressDialog(AudioVideoPlayerLollipop.this);
                                temp.setMessage(getString(R.string.context_move_to_trash));
                                temp.show();
                            }
                            catch(Exception e){
                                return;
                            }
                            moveToTrashStatusDialog = temp;
                        }
                        else{
                            megaApi.remove(megaApi.getNodeByHandle(handle), AudioVideoPlayerLollipop.this);
                            ProgressDialog temp = null;
                            try{
                                temp = new ProgressDialog(AudioVideoPlayerLollipop.this);
                                temp.setMessage(getString(R.string.context_delete_from_mega));
                                temp.show();
                            }
                            catch(Exception e){
                                return;
                            }
                            moveToTrashStatusDialog = temp;
                        }


                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        if (moveToRubbish){
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
            String message= getResources().getString(R.string.confirmation_move_to_rubbish);
            builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
            String message= getResources().getString(R.string.confirmation_delete_from_mega);
            builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        }
    }


    public void showCopy(){
        log("showCopy");

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_COPY_FOLDER);
    }

    public void showMove(){
        log("showMove");

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_MOVE_FOLDER);
    }

    private void showKeyboardDelayed(final View view) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    public void showRenameDialog() {
        log("showRenameDialog");
        final MegaNode node = megaApi.getNodeByHandle(handle);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(mega.privacy.android.app.utils.Util.scaleWidthPx(20, outMetrics), mega.privacy.android.app.utils.Util.scaleHeightPx(20, outMetrics), mega.privacy.android.app.utils.Util.scaleWidthPx(17, outMetrics), 0);
        //	    layout.setLayoutParams(params);

        final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
        input.setSingleLine();
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        input.setImeActionLabel(getString(R.string.context_rename), EditorInfo.IME_ACTION_DONE);
        input.setText(node.getName());


        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                if (hasFocus) {
                    if (node.isFolder()) {
                        input.setSelection(0, input.getText().length());
                    } else {
                        String[] s = node.getName().split("\\.");
                        if (s != null) {
                            int numParts = s.length;
                            int lastSelectedPos = 0;
                            if (numParts == 1) {
                                input.setSelection(0, input.getText().length());
                            } else if (numParts > 1) {
                                for (int i = 0; i < (numParts - 1); i++) {
                                    lastSelectedPos += s[i].length();
                                    lastSelectedPos++;
                                }
                                lastSelectedPos--; //The last point should not be selected)
                                input.setSelection(0, lastSelectedPos);
                            }
                        }
                        showKeyboardDelayed(v);
                    }
                }
            }
        });


        layout.addView(input, params);

        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.setMargins(mega.privacy.android.app.utils.Util.scaleWidthPx(20, outMetrics), 0, mega.privacy.android.app.utils.Util.scaleWidthPx(17, outMetrics), 0);

        final RelativeLayout error_layout = new RelativeLayout(AudioVideoPlayerLollipop.this);
        layout.addView(error_layout, params1);

        final ImageView error_icon = new ImageView(AudioVideoPlayerLollipop.this);
        error_icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_input_warning));
        error_layout.addView(error_icon);
        RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();

        params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        error_icon.setLayoutParams(params_icon);

        error_icon.setColorFilter(ContextCompat.getColor(AudioVideoPlayerLollipop.this, R.color.login_warning));

        final TextView textError = new TextView(AudioVideoPlayerLollipop.this);
        error_layout.addView(textError);
        RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
        params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
        params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params_text_error.setMargins(mega.privacy.android.app.utils.Util.scaleWidthPx(3, outMetrics), 0, 0, 0);
        textError.setLayoutParams(params_text_error);

        textError.setTextColor(ContextCompat.getColor(AudioVideoPlayerLollipop.this, R.color.login_warning));

        error_layout.setVisibility(View.GONE);

        input.getBackground().mutate().clearColorFilter();
        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (error_layout.getVisibility() == View.VISIBLE) {
                    error_layout.setVisibility(View.GONE);
                    input.getBackground().mutate().clearColorFilter();
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String value = v.getText().toString().trim();
                    if (value.length() == 0) {
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_string));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();

                    } else {
                        boolean result = matches(regex, value);
                        if (result) {
                            input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                            textError.setText(getString(R.string.invalid_characters));
                            error_layout.setVisibility(View.VISIBLE);
                            input.requestFocus();

                        } else {
                            //						nC.renameNode(node, value);
                            renameDialog.dismiss();
                            rename(value, node);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.context_rename) + " "	+ new String(node.getName()));
        builder.setPositiveButton(getString(R.string.context_rename),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString().trim();
                        if (value.length() == 0) {
                            return;
                        }
                        rename(value, node);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                input.getBackground().clearColorFilter();
            }
        });
        builder.setView(layout);
        renameDialog = builder.create();
        renameDialog.show();
        renameDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String value = input.getText().toString().trim();

                if (value.length() == 0) {
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                    textError.setText(getString(R.string.invalid_string));
                    error_layout.setVisibility(View.VISIBLE);
                    input.requestFocus();
                }
                else{
                    boolean result=matches(regex, value);
                    if(result){
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_characters));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();

                    }else{
                        //nC.renameNode(node, value);
                        renameDialog.dismiss();
                        rename(value, node);
                    }
                }
            }
        });
    }

    private void rename(String newName, MegaNode node){
        if (newName.equals(node.getName())) {
            return;
        }

        if(!mega.privacy.android.app.utils.Util.isOnline(this)){
            Snackbar.make(containerAudioVideoPlayer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
            return;
        }

        if (isFinishing()){
            return;
        }

        ProgressDialog temp = null;
        try{
            temp = new ProgressDialog(this);
            temp.setMessage(getString(R.string.context_renaming));
            temp.show();
        }
        catch(Exception e){
            return;
        }
        statusDialog = temp;

        log("renaming " + node.getName() + " to " + newName);

        megaApi.renameNode(node, newName, this);
    }

    public static boolean matches(String regex, CharSequence input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        return m.find();
    }

    public void showRemoveLink(){
        android.support.v7.app.AlertDialog removeLinkDialog;
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
        TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
        TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
        TextView symbol = (TextView) dialoglayout.findViewById(R.id.dialog_link_symbol);
        TextView removeText = (TextView) dialoglayout.findViewById(R.id.dialog_link_text_remove);

        ((RelativeLayout.LayoutParams) removeText.getLayoutParams()).setMargins(mega.privacy.android.app.utils.Util.scaleWidthPx(25, outMetrics), mega.privacy.android.app.utils.Util.scaleHeightPx(20, outMetrics), mega.privacy.android.app.utils.Util.scaleWidthPx(10, outMetrics), 0);

        url.setVisibility(View.GONE);
        key.setVisibility(View.GONE);
        symbol.setVisibility(View.GONE);
        removeText.setVisibility(View.VISIBLE);

        removeText.setText(getString(R.string.context_remove_link_warning_text));

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;

        float scaleW = mega.privacy.android.app.utils.Util.getScaleW(outMetrics, density);
        float scaleH = mega.privacy.android.app.utils.Util.getScaleH(outMetrics, density);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (10*scaleW));
        }else{
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (15*scaleW));

        }

        builder.setView(dialoglayout);

        builder.setPositiveButton(getString(R.string.context_remove), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                typeExport=TYPE_EXPORT_REMOVE;
                megaApi.disableExport(megaApi.getNodeByHandle(handle), audioVideoPlayerLollipop);
            }
        });

        builder.setNegativeButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        removeLinkDialog = builder.create();
        removeLinkDialog.show();
    }

    public void showGetLinkActivity(){
        log("showGetLinkActivity");
        Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
        linkIntent.putExtra("handle", handle);
        linkIntent.putExtra("account", accountType);
        startActivity(linkIntent);
    }

    public void showPropertiesActivity(){
        Intent i = new Intent(this, FileInfoActivityLollipop.class);
        if (isOffline){
            i.putExtra("name", fileName);
            i.putExtra("imageId", MimeTypeMime.typeForName(fileName).getIconResourceId());
            i.putExtra("adapterType", Constants.OFFLINE_ADAPTER);
            i.putExtra("path", path);
            if (pathNavigation != null){
                i.putExtra("pathNavigation", pathNavigation);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                i.setDataAndType(uri, MimeTypeList.typeForName(fileName).getType());
            }
            else{
                i.setDataAndType(uri, MimeTypeList.typeForName(fileName).getType());
            }
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        else {
            MegaNode node = megaApi.getNodeByHandle(handle);
            i.putExtra("handle", node.getHandle());
            i.putExtra("imageId", MimeTypeMime.typeForName(node.getName()).getIconResourceId());
            i.putExtra("name", node.getName());
        }
        startActivity(i);
        renamed = false;
    }

    public void downloadFile() {

        MegaNode node = megaApi.getNodeByHandle(handle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_STORAGE);

                handleListM.add(node.getHandle());
            }
        }
        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(node.getHandle());

        if(nC==null){
            nC = new NodeController(this);
        }
        nC.prepareForDownload(handleList);
    }


    public void intentToSendFile(Uri uri){
        log("intentToSendFile");

        if(uri!=null){
            if (!isUrl) {
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType(MimeTypeList.typeForName(fileName).getType()+"/*");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    log("Use provider to share");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()));
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                }
                startActivity(Intent.createChooser(share, getString(R.string.context_share)));
            }
            else{
                Snackbar.make(audioVideoPlayerContainer, getString(R.string.not_download), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (intent == null) {
            return;
        }

        if (requestCode == Constants.REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
            long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
            log("Send to "+chatHandles.length+" chats");

            long[] nodeHandles = intent.getLongArrayExtra("NODE_HANDLES");
            log("Send "+nodeHandles.length+" nodes");

            countChat = chatHandles.length;
            if(countChat==1){
                megaChatApi.attachNode(chatHandles[0], nodeHandles[0], this);
            }
            else if(countChat>1){

                for(int i=0; i<chatHandles.length; i++){
                    megaChatApi.attachNode(chatHandles[i], nodeHandles[0], this);
                }
            }
        }
        else if (requestCode == Constants.REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
            log("local folder selected");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
            long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
            long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
            log("URL: " + url + "___SIZE: " + size);

            if(nC==null){
                nC = new NodeController(this);
            }
            nC.checkSizeBeforeDownload(parentPath, url, size, hashes);
        }
        else if (requestCode == Constants.REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

            if(!mega.privacy.android.app.utils.Util.isOnline(this)){
                Snackbar.make(containerAudioVideoPlayer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
                return;
            }

            final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
            final long toHandle = intent.getLongExtra("MOVE_TO", 0);
            final int totalMoves = moveHandles.length;

            MegaNode parent = megaApi.getNodeByHandle(toHandle);
            moveToRubbish = false;

            ProgressDialog temp = null;
            try{
                temp = new ProgressDialog(this);
                temp.setMessage(getString(R.string.context_moving));
                temp.show();
            }
            catch(Exception e){
                return;
            }
            statusDialog = temp;

            for(int i=0; i<moveHandles.length;i++){
                megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, this);
            }
        }
        else if (requestCode == Constants.REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
            if(!mega.privacy.android.app.utils.Util.isOnline(this)){
                Snackbar.make(containerAudioVideoPlayer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
                return;
            }

            final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
            final long toHandle = intent.getLongExtra("COPY_TO", 0);
            final int totalCopy = copyHandles.length;

            ProgressDialog temp = null;
            try{
                temp = new ProgressDialog(this);
                temp.setMessage(getString(R.string.context_copying));
                temp.show();
            }
            catch(Exception e){
                return;
            }
            statusDialog = temp;

            MegaNode parent = megaApi.getNodeByHandle(toHandle);
            for(int i=0; i<copyHandles.length;i++){
                MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
                if (cN != null){
                    log("cN != null, i = " + i + " of " + copyHandles.length);
                    megaApi.copyNode(cN, parent, this);
                }
                else{
                    log("cN == null, i = " + i + " of " + copyHandles.length);
                    try {
                        statusDialog.dismiss();
                        Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
                    }
                    catch (Exception ex) {}
                }
            }
        }
    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
        log("onVideoEnabled");
        video = true;
    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        log("onVideoDecoderInitialized");
        video = true;
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        log("onVideoInputFormatChanged");
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
        log("onDroppedFrames");
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        log("onVideoSizeChanged");
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        log("onRenderedFirstFrame");
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
        log("onVideoDisabled");
        video = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");
        if (megaApi.getNodeByHandle(handle) == null){
            finish();
        }
        updateFile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
    }

    @Override
    protected void onDestroy() {
        log("onDestroy()");

        if (megaApi != null) {
            megaApi.removeTransferListener(this);
        }
        if (player != null){
            player.release();
        }

        super.onDestroy();
    }

    public void updateFile (){
        log("updateFile");

        MegaNode file = null;

        if (fileName != null){
            file = megaApi.getNodeByHandle(handle);
            if (file != null){
                if (!fileName.equals(file.getName())) {
                    fileName = file.getName();
                    if (aB != null){
                        tB = (Toolbar) findViewById(R.id.call_toolbar);
                        if(tB==null){
                            log("Tb is Null");
                            return;
                        }
                        tB.setVisibility(View.VISIBLE);
                        setSupportActionBar(tB);
                        aB = getSupportActionBar();
                    }
                    aB.setTitle(" ");
                    exoPlayerName.setText(fileName);
                    setTitle(fileName);

                    boolean isOnMegaDownloads = false;
                    String localPath = mega.privacy.android.app.utils.Util.getLocalFile(this, file.getName(), file.getSize(), downloadLocationDefaultPath);
                    File f = new File(downloadLocationDefaultPath, file.getName());
                    if(f.exists() && (f.length() == file.getSize())){
                        isOnMegaDownloads = true;
                    }
                    if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
                        File mediaFile = new File(localPath);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && prefs.getStorageDownloadLocation().contains(Environment.getExternalStorageDirectory().getPath())) {
                            uri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                        }
                        else{
                            uri = Uri.fromFile(mediaFile);
                        }
                    }
                    else {
                        if (megaApi == null){
                            MegaApplication app = (MegaApplication)getApplication();
                            megaApi = app.getMegaApi();
                            megaApi.addTransferListener(this);
                            megaApi.addGlobalListener(this);
                        }
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart();
                        }

                        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        activityManager.getMemoryInfo(mi);

                        if(mi.totalMem>Constants.BUFFER_COMP){
                            log("Total mem: "+mi.totalMem+" allocate 32 MB");
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
                        }
                        else{
                            log("Total mem: "+mi.totalMem+" allocate 16 MB");
                            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
                        }

                        String url = megaApi.httpServerGetLocalLink(file);
                        if (url != null){
                            uri = Uri.parse(url);
                        }
                    }
                    if (uri.toString().contains("http://")){
                        isUrl = true;
                    }
                    else {
                        isUrl = false;
                    }
                    supportInvalidateOptionsMenu();
                    renamed = true;
                }
            }
        }
    }

    public void getDownloadLocation(){
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        }

        prefs = dbH.getPreferences();
        if (prefs != null){
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }
    }

    public static void log(String message) {
        mega.privacy.android.app.utils.Util.log("AudioVideoPlayerLollipop", message);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish");
        if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("File sent correctly");
                successSent++;

            }
            else{
                log("File NOT sent: "+e.getErrorCode()+"___"+e.getErrorString());
                errorSent++;
            }

            if(countChat==errorSent+successSent){
                if(successSent==countChat){
                    if(countChat==1){
                        long handle = request.getChatHandle();
                        MegaChatListItem chatItem = megaChatApi.getChatListItem(handle);
                        if(chatItem!=null){
                            Intent intent = new Intent(this, ManagerActivityLollipop.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
                            intent.putExtra("CHAT_ID", handle);
                            startActivity(intent);
                            finish();
                        }
                    }
                    else{
                        showSnackbar(getString(R.string.success_attaching_node_from_cloud_chats, countChat));
                    }
                }
                else if(errorSent==countChat){
                    showSnackbar(getString(R.string.error_attaching_node_from_cloud));
                }
                else{
                    showSnackbar(getString(R.string.error_attaching_node_from_cloud_chats));
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(containerAudioVideoPlayer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    @Override
    public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

    }

    @Override
    public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {

    }

    @Override
    public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {

    }

    @Override
    public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {
        log("onTransferTemporaryError");

        if(e.getErrorCode() == MegaError.API_EOVERQUOTA){
            log("API_EOVERQUOTA error!!");

            if(alertDialogTransferOverquota==null){
                showTransferOverquotaDialog();
            }
            else {
                if (!(alertDialogTransferOverquota.isShowing())) {
                    showTransferOverquotaDialog();
                }
            }
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return false;
    }


    public void showTransferOverquotaDialog(){
        log("showTransferOverquotaDialog");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.transfer_overquota_layout, null);
        dialogBuilder.setView(dialogView);

        TextView title = (TextView) dialogView.findViewById(R.id.transfer_overquota_title);
        title.setText(getString(R.string.title_depleted_transfer_overquota));

        ImageView icon = (ImageView) dialogView.findViewById(R.id.image_transfer_overquota);
        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.transfer_quota_empty));

        TextView text = (TextView) dialogView.findViewById(R.id.text_transfer_overquota);
        text.setText(getString(R.string.text_depleted_transfer_overquota));

        Button continueButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_dissmiss);

        Button paymentButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_payment);
        paymentButton.setText(getString(R.string.action_upgrade_account));

        alertDialogTransferOverquota = dialogBuilder.create();

        alertDialogTransferOverquota.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                transferOverquota = true;
                showActionStatusBar();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                alertDialogTransferOverquota.dismiss();
                transferOverquota = false;
            }

        });

        paymentButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                alertDialogTransferOverquota.dismiss();
                transferOverquota = false;
                showUpgradeAccount();
            }
        });

        alertDialogTransferOverquota.setCancelable(false);
        alertDialogTransferOverquota.setCanceledOnTouchOutside(false);
        alertDialogTransferOverquota.show();
    }

    public void showUpgradeAccount(){
        log("showUpgradeAccount");
        Intent upgradeIntent = new Intent(this, ManagerActivityLollipop.class);
        upgradeIntent.setAction(Constants.ACTION_SHOW_UPGRADE_ACCOUNT);
        startActivity(upgradeIntent);
    }

    @Override
    public void onViewPositionChanged(float fractionScreen) {
        ivShadow.setAlpha(1 - fractionScreen);
    }

    @Override
    public void onBackPressed() {
        if (!onPlaylist){
            super.onBackPressed();
            if (player != null) {
                player.release();
            }
            setImageDragVisibility(View.VISIBLE);
        }
        else {
            if (querySearch.equals("")){
                releasePlaylist();
            }
            else{
                querySearch  = "";
                aB.setTitle(getString(R.string.section_playlist));
                if (playlistFragment != null && playlistFragment.isAdded()){
                    playlistFragment.setNodesSearch("");

                }
                invalidateOptionsMenu();
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(getContainer());
        View view = LayoutInflater.from(this).inflate(layoutResID, null);
        draggableView.addView(view);
    }

    private View getContainer() {
        RelativeLayout container = new RelativeLayout(this);
        draggableView = new DraggableView(this);
        if (getIntent() != null) {
            screenPosition = getIntent().getIntArrayExtra("screenPosition");
            draggableView.setScreenPosition(screenPosition);
        }
        draggableView.setDraggableListener(this);
        ivShadow = new ImageView(this);
        ivShadow.setBackgroundColor(ContextCompat.getColor(this, R.color.black_p50));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        container.addView(ivShadow, params);
        container.addView(draggableView);
        return container;
    }

    @Override
    public void onDragActivated(boolean activated) {
        if (activated) {
            ivShadow.setBackgroundColor(ContextCompat.getColor(this, R.color.black_p50));
            updateCurrentImage();
            if (aB != null && aB.isShowing()) {
                if(tB != null) {
                    tB.animate().translationY(-220).setDuration(0)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    aB.hide();
                                }
                            }).start();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                else {
                    aB.hide();
                }
                simpleExoPlayerView.hideController();
            }
            containerAudioVideoPlayer.setBackgroundColor(TRANSPARENT);

            playerLayout.setBackgroundColor(TRANSPARENT);
            appBarLayout.setBackgroundColor(TRANSPARENT);
            draggableView.setCurrentView(simpleExoPlayerView.getVideoSurfaceView());
            setImageDragVisibility(View.GONE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                containerAudioVideoPlayer.setElevation(0);
                playerLayout.setElevation(0);
                appBarLayout.setElevation(0);
            }
        }
        else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ivShadow.setBackgroundColor(TRANSPARENT);
                    showActionStatusBar();
                    containerAudioVideoPlayer.setBackgroundColor(BLACK);
                    playerLayout.setBackgroundColor(BLACK);
                    appBarLayout.setBackgroundColor(BLACK);
                }
            }, 300);
        }
    }

    public void openAdvancedDevices (long handleToDownload){
        log("openAdvancedDevices");
//		handleToDownload = handle;
        String externalPath = mega.privacy.android.app.utils.Util.getExternalCardPath();

        if(externalPath!=null){
            log("ExternalPath for advancedDevices: "+externalPath);
            MegaNode node = megaApi.getNodeByHandle(handleToDownload);
            if(node!=null){

//				File newFile =  new File(externalPath+"/"+node.getName());
                File newFile =  new File(node.getName());
                log("File: "+newFile.getPath());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a file with the requested MIME type.
                String mimeType = MimeTypeList.getMimeType(newFile);
                log("Mimetype: "+mimeType);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, node.getName());
                intent.putExtra("handleToDownload", handleToDownload);
                try{
                    startActivityForResult(intent, Constants.WRITE_SD_CARD_REQUEST_CODE);
                }
                catch(Exception e){
                    log("Exception in External SDCARD");
                    Environment.getExternalStorageDirectory();
                    Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
        else{
            log("No external SD card");
            Environment.getExternalStorageDirectory();
            Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void showSnackbarNotSpace(){
        log("showSnackbarNotSpace");
        Snackbar mySnackbar = Snackbar.make(containerAudioVideoPlayer, R.string.error_not_enough_free_space, Snackbar.LENGTH_LONG);
        mySnackbar.setAction("Settings", new SnackbarNavigateOption(this));
        mySnackbar.show();
    }

    public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes){
        log("askSizeConfirmationBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(mega.privacy.android.app.utils.Util.scaleWidthPx(20, outMetrics), mega.privacy.android.app.utils.Util.scaleHeightPx(10, outMetrics), mega.privacy.android.app.utils.Util.scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

        builder.setMessage(getString(R.string.alert_larger_file, mega.privacy.android.app.utils.Util.getSizeString(sizeC)));
        builder.setPositiveButton(getString(R.string.general_download),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskSizeDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(AudioVideoPlayerLollipop.this);
                        }
                        nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskSizeDownload("false");
                }
            }
        });

        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }

    public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload){
        log("askConfirmationNoAppInstaledBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(mega.privacy.android.app.utils.Util.scaleWidthPx(20, outMetrics), mega.privacy.android.app.utils.Util.scaleHeightPx(10, outMetrics), mega.privacy.android.app.utils.Util.scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));
        builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
        builder.setPositiveButton(getString(R.string.general_download),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskNoAppDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(AudioVideoPlayerLollipop.this);
                        }
                        nC.download(parentPathC, urlC, sizeC, hashesC);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskNoAppDownload("false");
                }
            }
        });
        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish");

        if (request.getType() == MegaRequest.TYPE_RENAME){

            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (e.getErrorCode() == MegaError.API_OK){
                Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_correctly_renamed), Snackbar.LENGTH_LONG).show();
                updateFile();
            }
            else{
                Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_no_renamed), Snackbar.LENGTH_LONG).show();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_MOVE){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (moveToRubbish){
                if (e.getErrorCode() == MegaError.API_OK){
                    this.finish();
                }
                else{
                    Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
                }
                moveToRubbish = false;
                log("move to rubbish request finished");
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
                    finish();
                }
                else{
                    Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
                }
                log("move nodes request finished");
            }
        }
        else if (request.getType() == MegaRequest.TYPE_REMOVE){


            if (e.getErrorCode() == MegaError.API_OK){
                if (moveToTrashStatusDialog.isShowing()){
                    try {
                        moveToTrashStatusDialog.dismiss();
                    }
                    catch (Exception ex) {}
                    Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_correctly_removed), Snackbar.LENGTH_LONG).show();
                }
                finish();
            }
            else{
                Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_no_removed), Snackbar.LENGTH_LONG).show();
            }
            log("remove request finished");
        }
        else if (request.getType() == MegaRequest.TYPE_COPY){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (e.getErrorCode() == MegaError.API_OK){
                Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_correctly_copied), Snackbar.LENGTH_LONG).show();
            }
            else{
                Snackbar.make(containerAudioVideoPlayer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
            }
            log("copy nodes request finished");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {
        log("onNodesUpdate");
        if (megaApi.getNodeByHandle(handle) == null){
            return;
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {

    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {

    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN){
            if (loop){
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.exo_play_list:{
                instantiatePlaylist();
                break;
            }
        }
    }

    void instantiatePlaylist(){
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        onPlaylist = true;
        progressBar.setVisibility(View.GONE);
        playerLayout.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        draggableView.setDraggable(false);
        tB.setBackgroundColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
        aB.setTitle(getString(R.string.section_playlist));
        supportInvalidateOptionsMenu();

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null){
            getSupportFragmentManager().beginTransaction().remove(currentFragment).commitNow();
        }
        if (playlistFragment == null){
            playlistFragment = new PlaylistFragment();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, playlistFragment, "playlistFragment");
        ft.commitNowAllowingStateLoss();
    }

    public String getPathNavigation() {
        return pathNavigation;
    }

    public int[] getScreenPosition() {
        return screenPosition;
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setPlayer (SimpleExoPlayer player){
        this.player = player;
    }

    public long getParentNodeHandle() {
        return parentNodeHandle;
    }

    public int getAdapterType() {
        return adapterType;
    }

    public ArrayList<Long> getMediaHandles() {
        return mediaHandles;
    }

    public ProgressBar getPlaylistProgressBar() {
        return playlistProgressBar;
    }

    public void setPlaylistProgressBar(ProgressBar playlistProgressBar) {
        this.playlistProgressBar = playlistProgressBar;
    }

    public void setCurrentTime (long currentTime){
        this.currentTime = currentTime;
    }

    public int getCurrentWindowIndex (){
        return currentWindowIndex;
    }

    public ArrayList<MegaOffline> getMediaOffList(){
        return mediaOffList;
    }
}