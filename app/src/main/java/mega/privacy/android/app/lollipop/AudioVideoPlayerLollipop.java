package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Pair;
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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
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
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.Unit;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.dragger.DraggableView;
import mega.privacy.android.app.components.dragger.ExitViewAnimator;
import mega.privacy.android.app.components.saver.OfflineNodeSaver;
import mega.privacy.android.app.fragments.homepage.audio.AudioFragment;
import mega.privacy.android.app.fragments.homepage.video.VideoFragment;
import mega.privacy.android.app.fragments.managerFragments.LinksFragment;
import mega.privacy.android.app.fragments.offline.OfflineFragment;
import mega.privacy.android.app.fragments.managerFragments.cu.CameraUploadsFragment;
import mega.privacy.android.app.fragments.recent.RecentsBucketFragment;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.lollipop.listeners.AudioFocusListener;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.DraggingThumbnailCallback;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
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
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.SearchNodesTask.getSearchedNodes;
import static mega.privacy.android.app.components.transferWidget.TransfersManagement.*;
import static mega.privacy.android.app.lollipop.FileInfoActivityLollipop.TYPE_EXPORT_REMOVE;
import static mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop.ARRAY_SEARCH;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.NodeTakenDownAlertHandler.showTakenDownAlert;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static android.graphics.Color.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;

public class AudioVideoPlayerLollipop extends PinActivityLollipop implements View.OnClickListener, View.OnTouchListener, MegaGlobalListenerInterface, VideoRendererEventListener, MegaRequestListenerInterface,
        MegaChatRequestListenerInterface, MegaTransferListenerInterface, DraggableView.DraggableListener {

    public static final String PLAY_WHEN_READY = "PLAY_WHEN_READY";
    public static final String IS_PLAYLIST = "IS_PLAYLIST";

    private static final Map<Class<?>, DraggingThumbnailCallback> DRAGGING_THUMBNAIL_CALLBACKS
            = new HashMap<>(DraggingThumbnailCallback.DRAGGING_THUMBNAIL_CALLBACKS_SIZE);

    private static final int DIALOG_VIEW_MARGIN_LEFT_DP = 20;
    private static final int DIALOG_VIEW_MARGIN_RIGHT_DP = 17;
    private static final int DIALOG_VIEW_MARGIN_TOP_DP = 10;
    private static final int DIALOG_VIEW_MARGIN_TOP_LARGE_DP = 20;
    private static final int FILE_NAME_MAX_WIDTH_DP = 300;
    private static final int RENAME_DIALOG_ERROR_TEXT_MARGIN_LEFT_DP = 3;
    private static final int REMOVE_LINK_DIALOG_REMOVE_TEXT_MARGIN_LEFT_DP = 25;
    private static final int REMOVE_LINK_DIALOG_REMOVE_TEXT_MARGIN_TOP_DP = 20;
    private static final int REMOVE_LINK_DIALOG_REMOVE_TEXT_MARGIN_RIGHT_DP = 10;

    private boolean fromChatSavedInstance = false;
    private int[] screenPosition;
    private int mLeftDelta;
    private int mTopDelta;
    private float mWidthScale;
    private float mHeightScale;
    private int screenWidth;
    private int screenHeight;
    private int placeholderCount;

    private AudioVideoPlayerLollipop audioVideoPlayerLollipop;

    private MegaApiAndroid megaApi;
    private MegaApiAndroid megaApiFolder;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH = null;
    private MegaPreferences prefs = null;

    private Handler handler;
    private Runnable runnableActionStatusBar = new Runnable() {
        @Override
        public void run() {
            hideActionStatusBar(400L);
        }
    };
    private boolean isFolderLink = false;
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private Uri uri;
    private TextView exoPlayerName;
    private ProgressBar progressBar;
    private RelativeLayout containerControls;
    private RelativeLayout controlsButtonsLayout;

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
    private MenuItem searchMenuItem;
    private MenuItem importMenuItem;
    private MenuItem saveForOfflineMenuItem;
    private MenuItem chatRemoveMenuItem;

    private RelativeLayout playerLayout;

    private RelativeLayout audioContainer;
    private long handle = -1;
    private int countChat = 0;
    private int successSent = 0;
    private int errorSent = 0;
    private boolean transferOverquota = false;

    private boolean video = false;
    private ProgressDialog statusDialog = null;
    private String fileName = null;
    private long currentTime;

    private RelativeLayout containerAudioVideoPlayer;

    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    private boolean isUrl;

    private ArrayList<Long> handleListM = new ArrayList<Long>();

    private int currentPosition = 0;
    private int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
    private long parentNodeHandle = -1;
    private int adapterType = 0;
    private ArrayList<Long> mediaHandles;
    private ArrayList<MegaOffline> offList;
    private ArrayList<MegaOffline> mediaOffList;
    private ArrayList<Uri> mediaUris;
    private boolean isPlayList;
    private int size = 0;

    private String downloadLocationDefaultPath = "";
    private boolean renamed = false;
    private boolean isOffline = false;
    private String path;
    private String pathNavigation;

    private DraggableView draggableView;
    private ImageView ivShadow;
    private NodeController nC;
    private OfflineNodeSaver offlineNodeSaver;
    private AlertDialog downloadConfirmationDialog;
    private DisplayMetrics outMetrics;

    private boolean fromShared = false;
    private int typeExport = -1;
    private AlertDialog renameDialog;
    private String regex = "[*|\\?:\"<>\\\\\\\\/]";
    boolean moveToRubbish = false;
    private ProgressDialog moveToTrashStatusDialog;
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
    public ConcatenatingMediaSource concatenatingMediaSource = null;
    private PlaylistFragment playlistFragment;
    private ProgressBar playlistProgressBar;
    private int currentWindowIndex;
    private String querySearch = "";

    boolean playWhenReady = true;
    boolean searchExpand = false;
    boolean fromChat = false;
    boolean isDeleteDialogShow = false;
    boolean isAbHide = false;
    boolean fromDownload = false;

    private ChatController chatC;
    private long msgId = -1;
    private long chatId = -1;
    private MegaNode nodeChat;
    private MegaChatMessage msgChat;

    private MegaNode currentDocument;
    private int playbackStateSaved;

    private ProgressBar createPlaylistProgressBar;
    private DefaultBandwidthMeter defaultBandwidthMeter;
    private DefaultDataSourceFactory dataSourceFactory;
    private ExtractorsFactory extractorsFactory;
    private MediaSource mediaSource = null;
    private boolean creatingPlaylist = false;
    private boolean playListCreated = false;
    private CreatePlayListTask createPlayListTask;
    private List<MediaSource> mediaSourcePlaylist = new ArrayList<>();
    private int createPlayListErrorCounter = 0;

    private String query;
    private File zipFile;
    private ArrayList<File> zipFiles = new ArrayList<>();
    private ArrayList<File> zipMediaFiles = new ArrayList<>();
    private boolean isZip = false;
    private GetMediaFilesTask getMediaFilesTask;
    private long [] nodeHandles;

    private AudioFocusRequest request;
    private AudioManager mAudioManager;
    private AudioFocusListener audioFocusListener;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null){
                screenPosition = intent.getIntArrayExtra("screenPosition");
                draggableView.setScreenPosition(screenPosition);
            }
        }
    };

    private BroadcastReceiver receiverToFinish = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                finish();
            }
        }
    };

    private BroadcastReceiver chatCallUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE)) {
                int callStatus = intent.getIntExtra(UPDATE_CALL_STATUS, -1);
                if ((callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT || callStatus == MegaChatCall.CALL_STATUS_RING_IN)
                        && player != null && player.getPlayWhenReady()) {
                    stopPlayback();
                }
            }
        }
    };

    public static void addDraggingThumbnailCallback(Class<?> clazz, DraggingThumbnailCallback cb) {
        DRAGGING_THUMBNAIL_CALLBACKS.put(clazz, cb);
    }

    public static void removeDraggingThumbnailCallback(Class<?> clazz) {
        DRAGGING_THUMBNAIL_CALLBACKS.remove(clazz);
    }

    @Override
    protected boolean shouldSetStatusBarTextColor() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_audiovideoplayer);

        audioVideoPlayerLollipop = this;

        registerReceiver(chatCallUpdateReceiver, new IntentFilter(ACTION_CALL_STATUS_UPDATE));
        registerReceiver(receiver, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG));
        registerReceiver(receiverToFinish, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN));

        downloadLocationDefaultPath = getDownloadLocation();

        draggableView.setViewAnimator(new ExitViewAnimator<>());

        Intent intent = getIntent();
        if (intent == null){
            logWarning("intent null");
            finish();
            return;
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (savedInstanceState != null) {
            logDebug("savedInstanceState NOT null");
            currentTime = savedInstanceState.getLong("currentTime");
            fileName = savedInstanceState.getString("fileName");
            handle = savedInstanceState.getLong("handle");
            uri = Uri.parse(savedInstanceState.getString("uri"));
            logDebug("savedInstanceState uri: " + uri);

            renamed = savedInstanceState.getBoolean("renamed");
            loop = savedInstanceState.getBoolean("loop");
            currentPosition = savedInstanceState.getInt("currentPosition");
            onPlaylist = savedInstanceState.getBoolean("onPlaylist");
            size = savedInstanceState.getInt("size");
            currentWindowIndex = savedInstanceState.getInt("currentWindowIndex");
            querySearch = savedInstanceState.getString("querySearch");
            playWhenReady = savedInstanceState.getBoolean("playWhenReady", true);
            isDeleteDialogShow = savedInstanceState.getBoolean("isDeleteDialogShow", false);
            isAbHide = savedInstanceState.getBoolean("isAbHide", false);
            placeholderCount = savedInstanceState.getInt("placeholder", 0);
        }
        else {
            logDebug("savedInstanceState null");

            isDeleteDialogShow = false;
            onPlaylist = false;
            currentTime = 0;
            currentWindowIndex = 0;
            handle = getIntent().getLongExtra("HANDLE", -1);
            fileName = getIntent().getStringExtra("FILENAME");
            currentPosition = intent.getIntExtra("position", 0);
            placeholderCount = intent.getIntExtra("placeholder", 0);
            playWhenReady = intent.getBooleanExtra(PLAY_WHEN_READY,true);
        }
        if (!renamed) {
            uri = intent.getData();
            if (uri == null) {
                logWarning("uri null");
                finish();
                return;
            }
        }
        fromDownload = intent.getBooleanExtra("fromDownloadService", false);
        fromShared = intent.getBooleanExtra("fromShared", false);
        path = intent.getStringExtra("path");
        adapterType = getIntent().getIntExtra("adapterType", 0);
        isPlayList = intent.getBooleanExtra(IS_PLAYLIST, true);

        if (adapterType == OFFLINE_ADAPTER){
            isOffline = true;
            pathNavigation = intent.getStringExtra("pathNavigation");
        }
        else if (adapterType == FILE_LINK_ADAPTER) {
            String serialize = intent.getStringExtra(EXTRA_SERIALIZE_STRING);
            if(serialize!=null) {
                currentDocument = MegaNode.unserialize(serialize);
                if (currentDocument != null) {
                    logDebug("currentDocument NOT NULL");
                }
                else {
                    logDebug("currentDocument is NULL");
                }
            }
        }
        else if (adapterType == ZIP_ADAPTER) {
            isZip = true;
            pathNavigation = intent.getStringExtra("offlinePathDirectory");
            if (pathNavigation != null) {
                zipFile = new File(pathNavigation);
                if (zipFile != null || !zipFile.exists() || !zipFile.isDirectory()) {
                    pathNavigation = zipFile.getParent();
                }
                else {
                    return;
                }
            }
        }
        else if (adapterType == SEARCH_ADAPTER) {
            query = intent.getStringExtra("searchQuery");
        }
        else if (adapterType == FROM_CHAT){
            fromChat = true;
            chatC = new ChatController(this);
            msgId = intent.getLongExtra("msgId", -1);
            chatId = intent.getLongExtra("chatId", -1);
        }
        else if (adapterType == RECENTS_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
            nodeHandles = intent.getLongArrayExtra(NODE_HANDLES);
            if (nodeHandles == null || nodeHandles.length <= 0) isPlayList = false;
        }

        isFolderLink = intent.getBooleanExtra("isFolderLink", false);
        orderGetChildren = intent.getIntExtra("orderGetChildren", MegaApiJava.ORDER_DEFAULT_ASC);
        parentNodeHandle = intent.getLongExtra("parentNodeHandle", -1);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        screenHeight = outMetrics.heightPixels;
        screenWidth = outMetrics.widthPixels;

        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        tB = (Toolbar) findViewById(R.id.call_toolbar);
        if (tB == null) {
            logWarning("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        exoPlayerName = (TextView) findViewById(R.id.exo_name_file);

        exoPlayerName.setMaxWidth(scaleWidthPx(FILE_NAME_MAX_WIDTH_DP, outMetrics));

        if (fileName == null) {
            fileName = getFileName(uri);
        }
        if (fileName != null) {
            aB.setTitle(" ");
            exoPlayerName.setText(fileName);
            setTitle(fileName);
            isVideo = MimeTypeList.typeForName(fileName).isVideoReproducible();
            String extension = fileName.substring(fileName.length() - 3, fileName.length());
            logDebug("Extension: " + extension);
            if (extension.equals("mp4")) {
                isMP4 = true;
            } else {
                isMP4 = false;
            }
        }
        containerAudioVideoPlayer = (RelativeLayout) findViewById(R.id.audiovideoplayer_container);
        playerLayout = (RelativeLayout) findViewById(R.id.player_layout);

        audioContainer = (RelativeLayout) findViewById(R.id.audio_container);
        audioContainer.setVisibility(View.GONE);

        progressBar = (ProgressBar) findViewById(R.id.full_video_viewer_progress_bar);
        containerControls = (RelativeLayout) findViewById(R.id.container_exo_controls);
        controlsButtonsLayout = (RelativeLayout) findViewById(R.id.container_control_buttons);

        setControllerLayoutParam();

        previousButton = (ImageButton) findViewById(R.id.exo_prev);
        previousButton.setOnTouchListener(this);
        nextButton = (ImageButton) findViewById(R.id.exo_next);
        nextButton.setOnTouchListener(this);
        ImageButton pauseButton = findViewById(R.id.exo_pause);
        pauseButton.setOnTouchListener(this);
        ImageButton playButton = findViewById(R.id.exo_play);
        playButton.setOnTouchListener(this);
        playList = (ImageButton) findViewById(R.id.exo_play_list);
        playList.setVisibility(View.GONE);
        playList.setOnClickListener(this);
        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        fragmentContainer.setVisibility(View.GONE);
        createPlaylistProgressBar = (ProgressBar) findViewById(R.id.create_playlist_progress_bar);
        createPlaylistProgressBar.setVisibility(View.GONE);
        playerView = findViewById(R.id.player_view);

        handler = new Handler();

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        }

        if (!isOffline && !isZip){
            MegaApplication app = (MegaApplication)getApplication();
            megaApi = app.getMegaApi();

            logDebug("Add transfer listener");
            megaApi.addTransferListener(this);
            megaApi.addGlobalListener(this);

            if (isOnline(this)){
                if(megaApi==null){
                    logDebug("Refresh session - sdk");
                    Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
                    intentLogin.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
                    intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intentLogin);
                    finish();
                    return;
                }
                else{
                    if(megaApi.isLoggedIn()>0){
                        if(megaApi.getRootNode()==null){
                            logDebug("Refresh session logged in but no fetch - sdk");
                            Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
                            intentLogin.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
                            intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intentLogin);
                            finish();
                            return;
                        }
                    }
                }

                if (megaChatApi == null) {
                    megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                }
                if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
                    logDebug("Refresh session - karere");
                    Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
                    intentLogin.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                    intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intentLogin);
                    finish();
                    return;
                }

                if (isFolderLink) {
                    megaApiFolder = app.getMegaApiFolder();
                }

                if (dbH != null && dbH.getCredentials() != null) {
                    if (megaApi.httpServerIsRunning() == 0) {
                        megaApi.httpServerStart();
                    }

                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);

                    if(mi.totalMem>BUFFER_COMP){
                        logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                    }
                    else{
                        logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                    }
                }
                else if (isFolderLink) {
                    if (megaApiFolder.httpServerIsRunning() == 0) {
                        megaApiFolder.httpServerStart();
                    }

                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);

                    if(mi.totalMem>BUFFER_COMP){
                        logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                        megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                    }
                    else{
                        logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                        megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                    }
                }

                if (megaChatApi != null){
                    if (msgId != -1 && chatId != -1){
                        msgChat = megaChatApi.getMessage(chatId, msgId);
                        if(msgChat==null){
                            msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId);
                        }
                        if (msgChat != null){
                            nodeChat = chatC.authorizeNodeIfPreview(msgChat.getMegaNodeList().get(0), megaChatApi.getChatRoom(chatId));
                            if (isDeleteDialogShow) {
                                showConfirmationDeleteNode(chatId, msgChat);
                            }
                        }
                    }
                    else {
                        logWarning("msgId or chatId null");
                    }
                }

                if (savedInstanceState != null && uri.toString().contains("http://") && !isFolderLink){
                    MegaNode node = null;
                    if (fromChat) {
                        node = nodeChat;
                    }
                    else if (adapterType == FILE_LINK_ADAPTER) {
                        node = currentDocument;
                    }
                    else {
                        node = megaApi.getNodeByHandle(handle);
                    }
                    if (node != null){
                        String url = megaApi.httpServerGetLocalLink(node);
                        if (url != null) {
                            uri = Uri.parse(url);
                        }
                    }
                    else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_streaming), -1);
                    }

                }

                if (isFolderLink){
                    logDebug("Folder link node");
                    MegaNode currentDocumentAuth = megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(handle));
                    if (dbH == null){
                        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
                    }
                    if (currentDocumentAuth == null){
                        logDebug("CurrentDocumentAuth is null");
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_streaming) + ": node not authorized", -1);
                    }
                    else{
                        logDebug("CurrentDocumentAuth is not null");
                        String url;
                        if (dbH != null && dbH.getCredentials() != null) {
                            url = megaApi.httpServerGetLocalLink(currentDocumentAuth);
                        }
                        else {
                            url = megaApiFolder.httpServerGetLocalLink(currentDocumentAuth);
                        }
                        if (url != null) {
                            uri = Uri.parse(url);
                        }
                    }
                }

                if (isOnTransferOverQuota()) {
                    showGeneralTransferOverQuotaWarning();
                }
            }
        }
        logDebug("uri: " + uri);

        if (uri.toString().contains("http://")){
            isUrl = true;
        }
        else {
            isUrl = false;
        }

        if (isAbHide) {
            hideActionStatusBar(0);
        }

        if (isPlayList) {
            getMediaFilesTask = new GetMediaFilesTask();
            getMediaFilesTask.execute();
        }
        else {
            createPlayer();
        }

        if (savedInstanceState == null){
            ViewTreeObserver observer = playerView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {

                    playerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    int[] location = new int[2];
                    playerView.getLocationOnScreen(location);
                    int[] getlocation = new int[2];
                    getLocationOnScreen(getlocation);
                    if (screenPosition != null){
                        if (fromChat) {
                            mLeftDelta = screenPosition[0] - (screenPosition[2]/2) - location[0];
                            mTopDelta = screenPosition[1] - (screenPosition[3]/2) - location[1];
                        }
                        else {
                            mLeftDelta = getlocation[0] - location[0];
                            mTopDelta = getlocation[1] - location[1];
                        }

                        mWidthScale = (float) screenPosition[2] / playerView.getWidth();
                        mHeightScale = (float) screenPosition[3] / playerView.getHeight();
                    }
                    else {
                        mLeftDelta = (screenWidth/2) - location[0];
                        mTopDelta = (screenHeight/2) - location[1];

                        mWidthScale = (float) (screenWidth/4) / playerView.getWidth();
                        mHeightScale = (float) (screenHeight/4) / playerView.getHeight();
                    }

                    runEnterAnimation();

                    return true;
                }
            });
        }
        else {
            if (fromChat) {
                fromChatSavedInstance = true;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setControllerLayoutParam();
    }

    /**
     * Update controller layout parameters according to screen orientation.
     */
    private void setControllerLayoutParam() {
        RelativeLayout.LayoutParams paramsName = (RelativeLayout.LayoutParams) exoPlayerName.getLayoutParams();
        RelativeLayout.LayoutParams paramsControlButtons = (RelativeLayout.LayoutParams) controlsButtonsLayout.getLayoutParams();
        RelativeLayout.LayoutParams paramsAudioContainer = (RelativeLayout.LayoutParams) audioContainer.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            paramsName.setMargins(0, 0, 0,
                    getResources().getDimensionPixelSize(R.dimen.av_player_file_name_margin_bottom_landscape));
            paramsControlButtons.setMargins(0, 0, 0,
                    getResources().getDimensionPixelSize(R.dimen.av_player_control_buttons_margin_bottom_landscape));
            paramsAudioContainer.addRule(RelativeLayout.ABOVE, containerControls.getId());
        } else {
            paramsName.setMargins(0,
                    getResources().getDimensionPixelSize(R.dimen.av_player_file_name_margin_top_portrait),
                    0,
                    getResources().getDimensionPixelSize(R.dimen.av_player_file_name_margin_bottom_portrait));
            paramsControlButtons.setMargins(0,
                    getResources().getDimensionPixelSize(R.dimen.av_player_control_buttons_margin_top_portrait),
                    0,
                    getResources().getDimensionPixelSize(R.dimen.av_player_control_buttons_margin_bottom_portrait));
            paramsAudioContainer.removeRule(RelativeLayout.ABOVE);
        }
        exoPlayerName.setLayoutParams(paramsName);
        controlsButtonsLayout.setLayoutParams(paramsControlButtons);
        audioContainer.setLayoutParams(paramsAudioContainer);
    }

    class GetMediaFilesTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            logDebug("GetMediaFilesTask");
            MegaNode parentNode;

            if (adapterType == OFFLINE_ADAPTER){
                offList = getIntent().getParcelableArrayListExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE);
                logDebug ("offList.size() = " + offList.size());

                for(int i=0; i<offList.size();i++){
                    MegaOffline checkOffline = offList.get(i);
                    File offlineFile = getOfflineFile(audioVideoPlayerLollipop, checkOffline);
                    if (!isFileAvailable(offlineFile)) {
                        offList.remove(i);
                        i--;
                    }
                }

                if (offList.size() > 0){

                    mediaOffList = new ArrayList<>();
                    int mediaPosition = -1;
                    for (int i=0;i<offList.size();i++){
                        if ((MimeTypeList.typeForName(offList.get(i).getName()).isVideoReproducible() && !MimeTypeList.typeForName(offList.get(i).getName()).isVideoNotSupported())
                                || (MimeTypeList.typeForName(offList.get(i).getName()).isAudio() && !MimeTypeList.typeForName(offList.get(i).getName()).isAudioNotSupported())){
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
            else if(adapterType == SEARCH_ADAPTER){
                mediaHandles = new ArrayList<>();
                ArrayList<String> handles = getIntent().getStringArrayListExtra(ARRAY_SEARCH);
                getMediaHandles(getSearchedNodes(handles));
            } else if (adapterType == SEARCH_BY_ADAPTER || adapterType == AUDIO_SEARCH_ADAPTER || adapterType  == VIDEO_SEARCH_ADAPTER) {
                long[] handles = getIntent().getLongArrayExtra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH);
                getMediaHandles(getSearchedNodes(handles));
            } else if (adapterType == AUDIO_BROWSE_ADAPTER) {
                getMediaHandles(megaApi.searchByType(orderGetChildren, MegaApiJava.FILE_TYPE_AUDIO, MegaApiJava.SEARCH_TARGET_ROOTNODE));
            } else if (adapterType == VIDEO_BROWSE_ADAPTER) {
                getMediaHandles(megaApi.searchByType(orderGetChildren, MegaApiJava.FILE_TYPE_VIDEO, MegaApiJava.SEARCH_TARGET_ROOTNODE));
            } else if (adapterType == FILE_LINK_ADAPTER) {
                if (currentDocument != null) {
                    logDebug("File link node NOT null");
                    size = 1;
                }
                else {
                    size = 0;
                }
            }
            else if (adapterType == ZIP_ADAPTER) {
                logDebug("ZIP_ADAPTER");
                if (pathNavigation != null) {
                    File[] files = new File(zipFile.getParent()).listFiles();

                    if (files != null && files.length > 1){
                        for (int i=0; i<files.length; i++) {
                            zipFiles.add(files[i]);
                        }
                        Collections.sort(zipFiles, new Comparator<File>(){

                            public int compare(File z1, File z2) {
                                String name1 = z1.getName();
                                String name2 = z2.getName();
                                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                                if (res == 0) {
                                    res = name1.compareTo(name2);
                                }
                                return res;
                            }
                        });
                        for (int i=0; i<zipFiles.size();i++) {
                            if (zipFiles.get(i).isFile() && (MimeTypeList.typeForName(zipFiles.get(i).getName()).isVideoReproducible() && !MimeTypeList.typeForName(zipFiles.get(i).getName()).isVideoNotSupported())
                                    || (MimeTypeList.typeForName(zipFiles.get(i).getName()).isAudio() && !MimeTypeList.typeForName(zipFiles.get(i).getName()).isAudioNotSupported())) {
                                zipMediaFiles.add(zipFiles.get(i));
                            }
                        }
                        size = zipMediaFiles.size();
                    }
                    else {
                        size = 1;
                    }
                }
            }
            else if (adapterType == RECENTS_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
                ArrayList<MegaNode> nodes = new ArrayList<>();
                MegaNode node;

                for (int i=0; i<nodeHandles.length; i++) {
                    if (nodeHandles[i] != -1) {
                        node = megaApi.getNodeByHandle(nodeHandles[i]);
                        if (node != null) {
                            nodes.add(node);
                        }
                    }
                }

                getMediaHandles(nodes);
            }
            else{
                ArrayList<MegaNode> nodes = null;
                if (adapterType == FOLDER_LINK_ADAPTER) {
                    if(megaApiFolder == null){
                        MegaApplication app = (MegaApplication)getApplication();
                        megaApiFolder = app.getMegaApiFolder();
                    }
                    if (parentNodeHandle == -1) {
                        parentNode = megaApiFolder.getRootNode();
                    }
                    else {
                        parentNode = megaApiFolder.getNodeByHandle(parentNodeHandle);
                    }
                    if (parentNode != null) {
                        nodes = megaApiFolder.getChildren(parentNode, orderGetChildren);
                    }
                } else if (isInRootLinksLevel(adapterType, parentNodeHandle)) {
                    nodes = megaApi.getPublicLinks(orderGetChildren);
                } else {
                    if (parentNodeHandle == -1) {
                        switch (adapterType) {
                            case FILE_BROWSER_ADAPTER: {
                                parentNode = megaApi.getRootNode();
                                break;
                            }
                            case RUBBISH_BIN_ADAPTER: {
                                parentNode = megaApi.getRubbishNode();
                                break;
                            }
                            case SHARED_WITH_ME_ADAPTER: {
                                parentNode = megaApi.getInboxNode();
                                break;
                            }
                            default: {
                                parentNode = megaApi.getRootNode();
                                break;
                            }
                        }

                    }
                    else {
                        parentNode = megaApi.getNodeByHandle(parentNodeHandle);
                    }
                    if (parentNode != null) {
                        nodes = megaApi.getChildren(parentNode, orderGetChildren);
                    }
                }

                getMediaHandles(nodes);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (size > 1) {
                createPlaylistProgressBar.setVisibility(View.VISIBLE);
            }

            mediaSourcePlaylist.clear();
            createPlayer();
        }

        private void getMediaHandles(ArrayList<MegaNode> nodes) {
            mediaHandles = new ArrayList<>();

            int mediaNumber = 0;
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    MegaNode n = nodes.get(i);
                    if (isNodeTakenDown(n)) continue;

                    if ((MimeTypeList.typeForName(n.getName()).isVideoReproducible() && !MimeTypeList.typeForName(n.getName()).isVideoNotSupported())
                            || (MimeTypeList.typeForName(n.getName()).isAudio() && !MimeTypeList.typeForName(n.getName()).isAudioNotSupported())) {
                        mediaHandles.add(n.getHandle());
                        if (i == currentPosition) {
                            currentPosition = mediaNumber;
                        }
                        mediaNumber++;
                    }
                }
            }

            if (mediaHandles.size() == 0) {
                finish();
            }

            if (currentPosition >= mediaHandles.size()) {
                currentPosition = 0;
            }

            size = mediaHandles.size();
        }
    }

    void createPlayer () {
        logDebug("createPlayer");
        createPlayListErrorCounter = 0;
        //Create the player
        MappingTrackSelector trackSelector = new DefaultTrackSelector(this);
        player = new SimpleExoPlayer.Builder(this,
                new DefaultRenderersFactory(this)
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON))
                .setTrackSelector(trackSelector)
                .build();

        //Set media controller
        playerView.setUseController(true);
        playerView.requestFocus();

        if (player != null) {
            //Bind the player to the view
            playerView.setPlayer(player);
            playerView.setControllerAutoShow(false);
            playerView.setControllerShowTimeoutMs(999999999);
            playerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (aB.isShowing()) {
                            hideActionStatusBar(400L);
                        }
                        else {
                            showActionStatusBar();
                        }
                    }
                    return true;
                }
            });

            //Measures bandwidth during playback. Can be null if not required.
           defaultBandwidthMeter = new DefaultBandwidthMeter();
            //Produces DataSource instances through which meida data is loaded
            //DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "android2"), defaultBandwidthMeter);
            dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "android2"), defaultBandwidthMeter);
            //Produces Extractor instances for parsing the media data
            extractorsFactory = new DefaultExtractorsFactory();

//            MediaSource mediaSource = null;
//            loopingMediaSource = null;

            if (isPlayList && size > 1) {
                createPlayListTask = new CreatePlayListTask();
                createPlayListTask.execute();
                createPlaylistProgressBar.setVisibility(View.VISIBLE);
            }
            else {
                creatingPlaylist = false;
            }
            mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
            player.prepare(mediaSource);

    //        final LoopingMediaSource finalLoopingMediaSource = loopingMediaSource;
//            final ConcatenatingMediaSource finalConcatenatingMediaSource = concatenatingMediaSource;
//            final MediaSource finalMediaSource = mediaSource;
            //MediaSource mediaSource = new HlsMediaSource(uri, dataSourceFactory, handler, null);
            //DashMediaSource mediaSource = new DashMediaSource(uri, dataSourceFactory, new DefaultDashChunkSource.Factory(dataSourceFactory), null, null);

            player.addListener(new Player.EventListener() {
                @Override
                public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {
                    logDebug("playerListener: onTimelineChanged");
                    updateContainers();
                    enableNextButton();
                }

                @Override
                public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
                    logDebug("playerListener: onTracksChanged");

                    updateContainers();
                    enableNextButton();
                    int newIndex = player.getCurrentWindowIndex();
                    if (currentWindowIndex != newIndex && playListCreated) {
                        updateFileProperties();
                    }
                }

                @Override
                public void onLoadingChanged(boolean isLoading) {
                    logDebug("playerListener: onLoadingChanged");
                    updateContainers();
                    enableNextButton();
                }

                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    logDebug("playbackState: " + playbackState);

                    if (playWhenReady && participatingInACall()) {
                        //Not allow to play content when a call is in progress
                        stopPlayback();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.not_allow_play_alert), -1);
                    }

                    playbackStateSaved = playbackState;
                    if (playbackState == Player.STATE_BUFFERING) {
                        audioContainer.setVisibility(View.GONE);
                        if (onPlaylist) {
                            playlistProgressBar.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }
                        else {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                    }
                    else if (playbackState == Player.STATE_ENDED) {
                        if (creatingPlaylist && playListCreated){
                           setPlaylist(currentWindowIndex + 1, 0);
                        }
                        else if (!loop && !onPlaylist && !creatingPlaylist) {
                            showActionStatusBar();
                        }
                    }
                    else {
                        if (onPlaylist) {
                            progressBar.setVisibility(View.GONE);
                            if (playlistProgressBar != null) {
                                playlistProgressBar.setVisibility(View.GONE);
                            }
                            if (playlistFragment != null && playlistFragment.isAdded()) {
                                if (playlistFragment.adapter != null) {
                                    playlistFragment.adapter.notifyDataSetChanged();
                                }
                            }
                        }
                        else {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                    enableNextButton();
                    updateContainers();
                }

                @Override
                public void onRepeatModeChanged(int repeatMode) {
                    logDebug("repeatMode: " + repeatMode);
                    updateContainers();
                    enableNextButton();
                }

                @Override
                public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                    logDebug("shuffleModeEnabled: " + shuffleModeEnabled);
                    updateContainers();
                    enableNextButton();
                }

                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    logWarning("Audio/Video player error", error);
                    playerError();
                }

                @Override
                public void onPositionDiscontinuity(int reason) {
                    logDebug("reason: " + reason);
                    updateContainers();
                    enableNextButton();

                    if (!creatingPlaylist) {
                        int oldWindowIndex = currentWindowIndex;
                        currentWindowIndex = player.getCurrentWindowIndex();

                        if (currentWindowIndex != oldWindowIndex && playListCreated) {
                            updateFileProperties();
                        }
                    }
                }

                @Override
                public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                    logDebug("playerListener: onPlaybackParametersChanged");
                    updateContainers();
                    enableNextButton();
                }

                @Override
                public void onSeekProcessed() {
                    logDebug("playerListener: onSeekProcessed");
                    updateContainers();
                    enableNextButton();
                }
            });
            numErrors = 0;
            if (playWhenReady) {
                startPlayback();
            } else {
                stopPlayback();
            }
            player.seekTo(currentTime);
            player.setVideoDebugListener(this);
        }
        else {
            logWarning("Error creating player");
        }
    }

    void setPlaylist (int index, long time) {
        boolean next = false;
        if (isOffline) {
            if (index < mediaOffList.size()) {
                next = true;
            }
        }
        else if (isZip) {
            if (index < zipMediaFiles.size()) {
                next = true;
            }
        }
        else {
            if (index < mediaHandles.size()) {
                next = true;
            }
        }
        if (next) {
            initPlaylist(index, time);
        }
    }

    void updateFileProperties () {
        logDebug("updateFileProperties");
        if (!creatingPlaylist && size > 1) {
            if (isOffline) {
                MegaOffline n = mediaOffList.get(currentWindowIndex);
                fileName = n.getName();
                handle = Long.parseLong(n.getHandle());
            }
            else if (isZip) {
                File zip = zipMediaFiles.get(currentWindowIndex);
                fileName = zip.getName();
                handle = -1;
            }
            else {
                MegaNode n;
                if (isFolderLink) {
                    n = megaApiFolder.getNodeByHandle(mediaHandles.get(currentWindowIndex));
                }
                else {
                    n = megaApi.getNodeByHandle(mediaHandles.get(currentWindowIndex));
                }
                fileName = n.getName();
                handle = n.getHandle();
            }

            isVideo = MimeTypeList.typeForName(fileName).isVideoReproducible();
            String extension = fileName.substring(fileName.length() - 3, fileName.length());
            if (extension.equals("mp4")) {
                isMP4 = true;
            } else {
                isMP4 = false;
            }
            exoPlayerName.setText(fileName);
            if (mediaUris != null && mediaUris.size() > currentWindowIndex) {
                uri = mediaUris.get(currentWindowIndex);
                if (uri.toString().contains("http://")) {
                    isUrl = true;
                } else {
                    isUrl = false;
                }
            }
            supportInvalidateOptionsMenu();
        }
        updateScrollPosition();

        if (onPlaylist) {
            if (playlistFragment != null && playlistFragment.isAdded() && playlistFragment.adapter != null) {
                if (currentWindowIndex < playlistFragment.adapter.getItemCount() && currentWindowIndex >= 0) {
                    playlistFragment.adapter.setItemChecked(currentWindowIndex);
                    playlistFragment.mLayoutManager.scrollToPosition(currentWindowIndex);
                    playlistFragment.adapter.notifyDataSetChanged();
                }
            }
        }
    }

    void enableNextButton() {
        if (playListCreated){
            if (isOffline) {
                if (currentWindowIndex == mediaOffList.size() -1)
                    return;
            }
            else if (isZip) {
                if (currentWindowIndex == zipMediaFiles.size() -1)
                    return;
            }
            else {
                if (currentWindowIndex == mediaHandles.size() -1)
                    return;
            }

            if (onPlaylist){
                if (playlistFragment != null && playlistFragment.isAdded()){
                    playlistFragment.nextButton.setEnabled(true);
                    playlistFragment.nextButton.setAlpha(1F);
                }
            }
            else {
                nextButton.setEnabled(true);
                nextButton.setAlpha(1F);
            }
            playerView.refreshDrawableState();
        }
    }

    void playerError() {
        numErrors++;
        player.stop();
        if (numErrors <= 6) {
            if (isPlayList && size > 1 && playListCreated && concatenatingMediaSource != null) {
//                player.prepare(finalLoopingMediaSource);
                player.prepare(concatenatingMediaSource);
            }
            else {
                player.prepare(mediaSource);
            }
            startPlayback();
        }
        else {
            showErrorDialog();
        }
    }

    void showErrorDialog() {
        logWarning("Error open video file");
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog)
            .setCancelable(false)
            .setMessage(isOnline(this) ? R.string.unsupported_file_type
                : R.string.error_fail_to_open_file_no_network)
            .setPositiveButton(getResources().getString(R.string.general_ok).toUpperCase(),
                (dialog, which) -> finish())
            .show();

        numErrors = 0;
    }

    void updateContainers() {
        if (isVideo) {
            if ((isMP4 && video) || !isMP4) {
                audioContainer.setVisibility(View.GONE);
                if (isAbHide){
                    containerControls.animate().translationY(400).setDuration(0).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            playerView.hideController();
                        }
                    }).start();
                }
            }
            else if (playbackStateSaved != Player.STATE_BUFFERING){
                playerView.showController();
                containerControls.animate().translationY(0).setDuration(0).start();
                audioContainer.setVisibility(View.VISIBLE);
            }
        }
        else if (playbackStateSaved != Player.STATE_BUFFERING){
            playerView.showController();
            containerControls.animate().translationY(0).setDuration(0).start();
            audioContainer.setVisibility(View.VISIBLE);
        }
    }

    public void updateCurrentImage(){

        if (adapterType == OFFLINE_ADAPTER){
            for (int i=0; i<offList.size(); i++){
                logDebug("Name: "+fileName+" mOfflist name: "+offList.get(i).getName());
                if (offList.get(i).getName().equals(fileName)){
                    getImageView(i, Long.parseLong(offList.get(i).getHandle()));
                    break;
                }
            }
        }
        else if (adapterType == ZIP_ADAPTER) {
            for (int i = 0; i< zipFiles.size(); i++) {
                if (zipFiles.get(i).getName().equals(fileName)) {
                    getImageView(i, -1);
                }
            }
        }
        else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER){
            getImageView(0, handle);
        }
        else if (adapterType == SEARCH_ADAPTER){
            getImageView(0, handle);
        }
        else {
            ArrayList<MegaNode> listNodes;
            if (isInRootLinksLevel(adapterType, parentNodeHandle)) {
                listNodes = megaApi.getPublicLinks(orderGetChildren);
            } else {
                MegaNode parentNode;
                if (isFolderLink) {
                    parentNode = megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(handle));
                } else {
                    parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(handle));
                }
                listNodes = megaApi.getChildren(parentNode, orderGetChildren);
            }

            for (int i=0; i<listNodes.size(); i++){
                if (listNodes.get(i).getHandle() == handle){
                    getImageView(i, handle);
                    break;
                }
            }
        }
    }

    public void getImageView (int i, long handle) {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION);
        intent.putExtra("position", i);
        intent.putExtra("actionType", UPDATE_IMAGE_DRAG);
        intent.putExtra("adapterType", adapterType);
        intent.putExtra("placeholder",placeholderCount);
        intent.putExtra("handle", handle);
        sendBroadcast(intent);
    }

    public void updateScrollPosition(){
        if (adapterType == OFFLINE_ADAPTER){
            for (int i=0; i<offList.size(); i++){
                logDebug("Name: " + fileName + " mOfflist name: " + offList.get(i).getName());
                if (offList.get(i).getName().equals(fileName)){
                    scrollToPosition(i, Long.parseLong(offList.get(i).getHandle()));
                    break;
                }
            }
        }
        else if (adapterType == ZIP_ADAPTER) {
            for (int i = 0; i< zipFiles.size(); i++) {
                if (zipFiles.get(i).getName().equals(fileName)) {
                    scrollToPosition(i, -1);
                }
            }
        }
        else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER){
            scrollToPosition(0, handle);
        }
        else if (adapterType == SEARCH_ADAPTER){
            scrollToPosition(0, handle);
        }
        else {
            ArrayList<MegaNode> listNodes;
            if (isInRootLinksLevel(adapterType, parentNodeHandle)) {
                listNodes = megaApi.getPublicLinks(orderGetChildren);
            } else {
                MegaNode parentNode;
                if (isFolderLink) {
                    parentNode = megaApiFolder.getParentNode(megaApiFolder.getNodeByHandle(handle));
                } else {
                    parentNode = megaApi.getParentNode(megaApi.getNodeByHandle(handle));
                }
                listNodes = megaApi.getChildren(parentNode, orderGetChildren);
            }

            for (int i = 0; i < listNodes.size(); i++) {
                if (listNodes.get(i).getHandle() == handle) {
                    scrollToPosition(i, handle);
                    break;
                }
            }
        }
    }

    void scrollToPosition (int i, long handle) {
        getImageView(i, handle);
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION);
        intent.putExtra("position", i);
        intent.putExtra("actionType", SCROLL_TO_POSITION);
        intent.putExtra("adapterType", adapterType);
        intent.putExtra("handle", handle);
        intent.putExtra("placeholder",placeholderCount);
        sendBroadcast(intent);
    }

    public void setImageDragVisibility(int visibility){
        if (adapterType == RUBBISH_BIN_ADAPTER){
            if (RubbishBinFragmentLollipop.imageDrag != null){
                RubbishBinFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == INBOX_ADAPTER){
            if (InboxFragmentLollipop.imageDrag != null){
                InboxFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == INCOMING_SHARES_ADAPTER){
            if (IncomingSharesFragmentLollipop.imageDrag != null) {
                IncomingSharesFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == OUTGOING_SHARES_ADAPTER){
            if (OutgoingSharesFragmentLollipop.imageDrag != null){
                OutgoingSharesFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == CONTACT_FILE_ADAPTER){
            if (ContactFileListFragmentLollipop.imageDrag != null){
                ContactFileListFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == FOLDER_LINK_ADAPTER){
            if (FolderLinkActivityLollipop.imageDrag != null){
                FolderLinkActivityLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == SEARCH_ADAPTER){
            if (SearchFragmentLollipop.imageDrag != null){
                SearchFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == FILE_BROWSER_ADAPTER){
            if (FileBrowserFragmentLollipop.imageDrag != null){
                FileBrowserFragmentLollipop.imageDrag.setVisibility(visibility);
            }
        }
        else if (adapterType == PHOTO_SYNC_ADAPTER ||adapterType == SEARCH_BY_ADAPTER) {
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(CameraUploadsFragment.class);
            if (callback != null) {
                callback.setVisibility(visibility);
            }
        } else if (adapterType == AUDIO_BROWSE_ADAPTER ||adapterType == AUDIO_SEARCH_ADAPTER) {
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(AudioFragment.class);
            if (callback != null) {
                callback.setVisibility(visibility);
            }
        } else if (adapterType == VIDEO_BROWSE_ADAPTER ||adapterType == VIDEO_SEARCH_ADAPTER) {
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(VideoFragment.class);
            if (callback != null) {
                callback.setVisibility(visibility);
            }
        }
        else if (adapterType == OFFLINE_ADAPTER) {
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(OfflineFragment.class);
            if (callback != null) {
                callback.setVisibility(visibility);
            }
        }
        else if (adapterType == ZIP_ADAPTER) {
            if (ZipBrowserActivityLollipop.imageDrag != null){
                ZipBrowserActivityLollipop.imageDrag.setVisibility(visibility);
            }
        } else if (adapterType == LINKS_ADAPTER) {
            if (LinksFragment.imageDrag != null){
                LinksFragment.imageDrag.setVisibility(visibility);
            }
        } else if (adapterType == RECENTS_ADAPTER && RecentsFragment.imageDrag != null) {
            RecentsFragment.imageDrag.setVisibility(visibility);
        } else if (adapterType == RECENTS_BUCKET_ADAPTER) {
            DraggingThumbnailCallback callback = DRAGGING_THUMBNAIL_CALLBACKS.get(RecentsBucketFragment.class);
            if (callback != null) {
                callback.setVisibility(visibility);
            }
        }
    }

    void getLocationOnScreen(int[] location){
        if (adapterType == RUBBISH_BIN_ADAPTER){
            if (RubbishBinFragmentLollipop.imageDrag != null) {
                RubbishBinFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == INBOX_ADAPTER){
            if (InboxFragmentLollipop.imageDrag != null){
                InboxFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == INCOMING_SHARES_ADAPTER){
            if (IncomingSharesFragmentLollipop.imageDrag != null) {
                IncomingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == OUTGOING_SHARES_ADAPTER){
            if (OutgoingSharesFragmentLollipop.imageDrag != null) {
                OutgoingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == CONTACT_FILE_ADAPTER){
            if (ContactFileListFragmentLollipop.imageDrag != null) {
                ContactFileListFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == FOLDER_LINK_ADAPTER){
            if (FolderLinkActivityLollipop.imageDrag != null) {
                FolderLinkActivityLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == SEARCH_ADAPTER){
            if (SearchFragmentLollipop.imageDrag != null){
                SearchFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == FILE_BROWSER_ADAPTER){
            if (FileBrowserFragmentLollipop.imageDrag != null){
                FileBrowserFragmentLollipop.imageDrag.getLocationOnScreen(location);
            }
        }
        else if (adapterType == PHOTO_SYNC_ADAPTER || adapterType == SEARCH_BY_ADAPTER) {
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(CameraUploadsFragment.class);
            if (callback != null) {
                callback.getLocationOnScreen(location);
            }
        } else if (adapterType == AUDIO_BROWSE_ADAPTER || adapterType == AUDIO_SEARCH_ADAPTER) {
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(AudioFragment.class);
            if (callback != null) {
                callback.getLocationOnScreen(location);
            }
        } else if (adapterType == VIDEO_BROWSE_ADAPTER || adapterType == VIDEO_SEARCH_ADAPTER) {
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(VideoFragment.class);
            if (callback != null) {
                callback.getLocationOnScreen(location);
            }
        }
        else if (adapterType == OFFLINE_ADAPTER){
            DraggingThumbnailCallback callback
                    = DRAGGING_THUMBNAIL_CALLBACKS.get(OfflineFragment.class);
            if (callback != null) {
                callback.getLocationOnScreen(location);
            }
        }
        else if (adapterType == ZIP_ADAPTER){
            if (ZipBrowserActivityLollipop.imageDrag != null){
                ZipBrowserActivityLollipop.imageDrag.getLocationOnScreen(location);
            }
        } else if (adapterType == LINKS_ADAPTER){
            if (LinksFragment.imageDrag != null){
                LinksFragment.imageDrag.getLocationOnScreen(location);
            }
        } else if (adapterType == RECENTS_ADAPTER && RecentsFragment.imageDrag != null){
            RecentsFragment.imageDrag.getLocationOnScreen(location);
        } else if(adapterType == RECENTS_BUCKET_ADAPTER) {
            DraggingThumbnailCallback callback = DRAGGING_THUMBNAIL_CALLBACKS.get(RecentsBucketFragment.class);
            if (callback != null) {
                callback.getLocationOnScreen(location);
            }
        }
    }

    public void runEnterAnimation() {
        final long duration = 600;

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
                try{
                    containerControls.animate().translationY(400).setDuration(0).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            playerView.hideController();
                        }
                    }).start();
                }
                catch(Exception e){
                    logWarning("Exception" + e.getMessage());
                }

            }
            else {
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


        playerView.setPivotX(0);
        playerView.setPivotY(0);
        playerView.setScaleX(mWidthScale);
        playerView.setScaleY(mHeightScale);
        playerView.setTranslationX(mLeftDelta);
        playerView.setTranslationY(mTopDelta);

        ivShadow.setAlpha(0);

        playerView.animate().setDuration(duration).scaleX(1).scaleY(1).translationX(0).translationY(0).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
            @Override
            public void run() {
                showActionStatusBar();
                playerView.showController();
                containerControls.animate().translationY(0).setDuration(400L).start();
                containerAudioVideoPlayer.setBackgroundColor(BLACK);
                playerLayout.setBackgroundColor(BLACK);
                appBarLayout.setBackgroundColor(BLACK);
            }
        });

        ivShadow.animate().setDuration(duration).alpha(1);

        handler.postDelayed(runnableActionStatusBar, 3000);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        logDebug("onSaveInstanceState");
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();

            // Pause either video or audio as per UX advise
            if (playWhenReady) {
                stopPlayback();
            }

            currentTime = player.getCurrentPosition();
        }
        if (createPlayListTask != null && createPlayListTask.getStatus() == AsyncTask.Status.RUNNING){
            createPlayListTask.cancel(true);
        }
        if (getMediaFilesTask != null && getMediaFilesTask.getStatus() == AsyncTask.Status.RUNNING) {
            getMediaFilesTask.cancel(true);
        }
        outState.putLong("currentTime", currentTime);
        outState.putInt("currentPosition", currentPosition);
        outState.putInt("placeholder",placeholderCount );
        outState.putLong("handle", handle);
        outState.putString("fileName", fileName);
        outState.putString("uri", uri.toString());
        if (getIntent() != null) {
            getIntent().setData(uri);
        }
        outState.putBoolean("renamed", renamed);
        outState.putBoolean("loop", loop);
        outState.putBoolean("onPlaylist", onPlaylist);
        outState.putInt("currentWindowIndex", currentWindowIndex);
        outState.putInt("size", size);
        outState.putString("querySearch", querySearch);
        outState.putBoolean("playWhenReady", playWhenReady);
        outState.putBoolean("isDeleteDialogShow", isDeleteDialogShow);
        outState.putBoolean("isAbHide", isAbHide);
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

    protected void hideActionStatusBar(long duration){
        isAbHide = true;
        if (aB != null && aB.isShowing()) {
            if(tB != null) {
                tB.animate().translationY(-220).setDuration(duration)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                aB.hide();
                            }
                        }).start();
            }
            else {
                aB.hide();
            }
            if (video){
                containerControls.animate().translationY(400).setDuration(duration).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        playerView.hideController();
                    }
                }).start();
            }
        }
    }
    protected void showActionStatusBar(){
        isAbHide = false;
        if (aB != null && !aB.isShowing()) {
            aB.show();
            if(tB != null) {
                tB.animate().translationY(0).setDuration(400L).start();
            }
            playerView.showController();
            if (creatingPlaylist){
                enableNextButton();
            }
            containerControls.animate().translationY(0).setDuration(400L).start();
        }
    }

    public void showToolbar() {
        if (tB == null) {
            tB = (Toolbar) findViewById(R.id.call_toolbar);
            if (tB == null) {
                logWarning("Tb is Null");
                return;
            }
            tB.setVisibility(View.VISIBLE);
            setSupportActionBar(tB);
        }
        if (aB != null && !aB.isShowing()) {
            aB.show();
            if(tB != null) {
                tB.animate().translationY(0).setDuration(0).start();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
        else {
            aB = getSupportActionBar();
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
            aB.show();
            if(tB != null) {
                tB.animate().translationY(0).setDuration(0).start();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");

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
                stopPlayback();
                if (playlistFragment != null && playlistFragment.isAdded()){
                    playlistFragment.setSearchOpen(true);
                    playlistFragment.hideController();
                }
                loopMenuItem.setVisible(false);
                querySearch = "";
                searchExpand = true;
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchExpand = false;
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
                    if (isOffline){
                        playlistFragment.contentText.setText(""+mediaOffList.size()+" "+getResources().getQuantityString(R.plurals.general_num_files, mediaOffList.size()));
                    }
                    else if (isZip) {
                        playlistFragment.contentText.setText(""+ zipMediaFiles.size()+" "+getResources().getQuantityString(R.plurals.general_num_files, zipMediaFiles.size()));
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
                logDebug("newText: " + newText);
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
        chatMenuItem.setIcon(mutateIconSecondary(this, R.drawable.ic_send_to_contact, R.color.white));
        propertiesMenuItem = menu.findItem(R.id.full_video_viewer_properties);
        getlinkMenuItem = menu.findItem(R.id.full_video_viewer_get_link);
        renameMenuItem = menu.findItem(R.id.full_video_viewer_rename);
        moveMenuItem = menu.findItem(R.id.full_video_viewer_move);
        copyMenuItem = menu.findItem(R.id.full_video_viewer_copy);
        moveToTrashMenuItem = menu.findItem(R.id.full_video_viewer_move_to_trash);
        removeMenuItem = menu.findItem(R.id.full_video_viewer_remove);
        removelinkMenuItem = menu.findItem(R.id.full_video_viewer_remove_link);
        loopMenuItem = menu.findItem(R.id.full_video_viewer_loop);
        importMenuItem = menu.findItem(R.id.chat_full_video_viewer_import);
        saveForOfflineMenuItem = menu.findItem(R.id.chat_full_video_viewer_save_for_offline);
        saveForOfflineMenuItem.setIcon(mutateIconSecondary(this, R.drawable.ic_b_save_offline, R.color.white));
        chatRemoveMenuItem = menu.findItem(R.id.chat_full_video_viewer_remove);

        if (nC == null) {
            nC = new NodeController(this);
        }
        boolean fromIncoming = false;
        if (adapterType == SEARCH_ADAPTER) {
            fromIncoming = nC.nodeComesFromIncoming(megaApi.getNodeByHandle(handle));
        }

        if (loop){
            loopMenuItem.setChecked(true);
            if (player != null) {
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
            }
        }
        else {
            loopMenuItem.setChecked(false);
            if (player != null) {
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        }

        if (!onPlaylist){
            logDebug("NOT on Playlist mode");
            searchMenuItem.setVisible(false);
            shareMenuItem.setVisible(showShareOption(adapterType, isFolderLink, handle));

            if (adapterType == OFFLINE_ADAPTER){
                logDebug("OFFLINE_ADAPTER");
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(true);
                downloadMenuItem.setVisible(true);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if(adapterType == SEARCH_ADAPTER && !fromIncoming){
                logDebug("SEARCH_ADAPTER");
                MegaNode node = megaApi.getNodeByHandle(handle);

                if(node.isExported()){
                    removelinkMenuItem.setVisible(true);
                    getlinkMenuItem.setVisible(false);
                }
                else{
                    removelinkMenuItem.setVisible(false);
                    getlinkMenuItem.setVisible(true);
                }

                downloadMenuItem.setVisible(true);
                propertiesMenuItem.setVisible(true);
                renameMenuItem.setVisible(true);
                moveMenuItem.setVisible(true);
                copyMenuItem.setVisible(true);
                chatMenuItem.setVisible(true);

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

                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if (adapterType == FROM_CHAT){
                logDebug("FROM_CHAT");
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(false);

                if(megaApi==null || !isOnline(this)) {
                    downloadMenuItem.setVisible(false);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);

                    if (MegaApiJava.userHandleToBase64(msgChat.getUserHandle()).equals(megaChatApi.getMyUserHandle()) && msgChat.isDeletable()) {
                        chatRemoveMenuItem.setVisible(true);
                    }
                    else {
                        chatRemoveMenuItem.setVisible(false);
                    }
                }
                else if (nodeChat != null){
                    downloadMenuItem.setVisible(true);
                    if (chatC.isInAnonymousMode()) {
                        importMenuItem.setVisible(false);
                        saveForOfflineMenuItem.setVisible(false);
                    }
                    else {
                        importMenuItem.setVisible(true);
                        saveForOfflineMenuItem.setVisible(true);
                    }

                    if (msgChat.getUserHandle() == megaChatApi.getMyUserHandle() && msgChat.isDeletable()) {
                        chatRemoveMenuItem.setVisible(true);
                    }
                    else {
                        chatRemoveMenuItem.setVisible(false);
                    }
                }
                else {
                    downloadMenuItem.setVisible(false);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);
                    chatRemoveMenuItem.setVisible(false);
                }
            }
            else if (adapterType == FILE_LINK_ADAPTER) {
                logDebug("FILE_LINK_ADAPTER");
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(false);
                downloadMenuItem.setVisible(true);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if (adapterType == ZIP_ADAPTER) {
                propertiesMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
                downloadMenuItem.setVisible(false);
                getlinkMenuItem.setVisible(false);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if (adapterType == INCOMING_SHARES_ADAPTER || fromIncoming) {
                propertiesMenuItem.setVisible(true);
                chatMenuItem.setVisible(true);
                copyMenuItem.setVisible(true);
                removeMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                downloadMenuItem.setVisible(true);

                MegaNode node = megaApi.getNodeByHandle(handle);
                int accessLevel = megaApi.getAccess(node);

                switch (accessLevel) {
                    case MegaShare.ACCESS_FULL: {
                        logDebug("Access FULL");
                        renameMenuItem.setVisible(true);
                        moveMenuItem.setVisible(true);
                        moveToTrashMenuItem.setVisible(true);

                        break;
                    }
                    case MegaShare.ACCESS_READ:
                        logDebug("Access read");
                    case MegaShare.ACCESS_READWRITE: {
                        logDebug("Read & Write");
                        renameMenuItem.setVisible(false);
                        moveMenuItem.setVisible(false);
                        moveToTrashMenuItem.setVisible(false);
                        break;
                    }
                }
            }
            else if (adapterType == RECENTS_ADAPTER || adapterType == RECENTS_BUCKET_ADAPTER) {
                MegaNode node = megaApi.getNodeByHandle(handle);
                chatRemoveMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);

                int accessLevel = megaApi.getAccess(node);
                switch (accessLevel) {
                    case MegaShare.ACCESS_READWRITE:
                    case MegaShare.ACCESS_READ:
                    case MegaShare.ACCESS_UNKNOWN: {
                        renameMenuItem.setVisible(false);
                        moveMenuItem.setVisible(false);
                        moveToTrashMenuItem.setVisible(false);
                        break;
                    }
                    case MegaShare.ACCESS_FULL:
                    case MegaShare.ACCESS_OWNER: {
                        renameMenuItem.setVisible(true);
                        moveMenuItem.setVisible(true);
                        moveToTrashMenuItem.setVisible(true);
                        break;
                    }
                }
            }
            else {
                logDebug("else");
                boolean shareVisible = true;

                MegaNode node = megaApi.getNodeByHandle(handle);

                if (node == null){
                    getlinkMenuItem.setVisible(false);
                    removelinkMenuItem.setVisible(false);
                    propertiesMenuItem.setVisible(false);
                    downloadMenuItem.setVisible(false);
                    renameMenuItem.setVisible(false);
                    moveMenuItem.setVisible(false);
                    copyMenuItem.setVisible(false);
                    moveToTrashMenuItem.setVisible(false);
                    removeMenuItem.setVisible(false);
                    chatMenuItem.setVisible(false);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);
                    chatRemoveMenuItem.setVisible(false);
                }
                else {
                    copyMenuItem.setVisible(true);

                    if(node.isExported()){
                        getlinkMenuItem.setVisible(false);
                        removelinkMenuItem.setVisible(true);
                    }
                    else{
                        if(adapterType==CONTACT_FILE_ADAPTER){
                            getlinkMenuItem.setVisible(false);
                            removelinkMenuItem.setVisible(false);
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

                        if(adapterType==CONTACT_FILE_ADAPTER){
                            removeMenuItem.setVisible(false);
                            node = megaApi.getNodeByHandle(handle);
                            int accessLevel = megaApi.getAccess(node);
                            switch(accessLevel){

                                case MegaShare.ACCESS_OWNER:
                                case MegaShare.ACCESS_FULL:{
                                    renameMenuItem.setVisible(true);
                                    moveMenuItem.setVisible(true);
                                    moveToTrashMenuItem.setVisible(true);
                                    chatMenuItem.setVisible(true);
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
                            chatMenuItem.setVisible(true);
                            renameMenuItem.setVisible(true);
                            moveMenuItem.setVisible(true);

//                                node = megaApi.getNodeByHandle(handle);
//
//                                final long handle = node.getHandle();
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

                    downloadMenuItem.setVisible(true);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);
                    chatRemoveMenuItem.setVisible(false);
                }
            }
        }
        else {
            logDebug("On Playlist mode");
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
            importMenuItem.setVisible(false);
            saveForOfflineMenuItem.setVisible(false);
            chatRemoveMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        logDebug("onPrepareOptionsMenu");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                logDebug("onBackPRess");
                onBackPressed();
                break;
            }
            case R.id.full_video_viewer_chat:{
                logDebug("Chat option");

                if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                    showOverDiskQuotaPaywallWarning();
                    break;
                }

                long[] longArray = new long[1];
                longArray[0] = handle;

                if(nC ==null){
                    nC = new NodeController(this, isFolderLink);
                }

                MegaNode attachNode = megaApi.getNodeByHandle(longArray[0]);
                if (attachNode != null) {
                    nC.checkIfNodeIsMineAndSelectChatsToSendNode(attachNode);
                }

                break;
            }
            case R.id.full_video_viewer_share: {
                logDebug("Share option");
                if (adapterType == OFFLINE_ADAPTER) {
                    shareFile(this, getOfflineFile(this, mediaOffList.get(currentWindowIndex)));
                } else if (adapterType == ZIP_ADAPTER) {
                    shareFile(this, zipMediaFiles.get(currentWindowIndex));
                } else if (adapterType == FILE_LINK_ADAPTER) {
                    shareLink(this, getIntent().getStringExtra(URL_FILE_LINK));
                } else {
                    shareNode(this, megaApi.getNodeByHandle(handle));
                }
                break;
            }
            case R.id.full_video_viewer_properties: {
                logDebug("Info option");
                showPropertiesActivity();
                break;
            }
            case R.id.full_video_viewer_download: {
                logDebug("Download option");
                downloadFile();
                break;
            }
            case R.id.full_video_viewer_get_link: {
                if (showTakenDownNodeActionNotAvailableDialog(megaApi.getNodeByHandle(handle), this)){
                    break;
                }

                showGetLinkActivity();
                break;
            }
            case R.id.full_video_viewer_remove_link: {
                if (showTakenDownNodeActionNotAvailableDialog(megaApi.getNodeByHandle(handle), this)){
                    break;
                }

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
                    logDebug("Loop NOT checked");
                    loopMenuItem.setChecked(false);
                    if (player != null) {
                        player.setRepeatMode(Player.REPEAT_MODE_OFF);
                    }
                    loop = false;
                }
                else {
                    loopMenuItem.setChecked(true);
                    if (player != null) {
                        player.setRepeatMode(Player.REPEAT_MODE_ONE);
                    }
                    logDebug("Loop checked");
                    loop = true;
                }
                break;
            }
            case R.id.chat_full_video_viewer_import:{
                if (nodeChat != null){
                    importNode();
                }
                break;
            }
            case R.id.chat_full_video_viewer_save_for_offline:{
                if (chatC == null){
                    chatC = new ChatController(this);
                }
                if (msgChat != null){
                    chatC.saveForOffline(msgChat.getMegaNodeList(), megaChatApi.getChatRoom(chatId));
                }
                break;
            }
            case R.id.chat_full_video_viewer_remove:{
                if (msgChat != null && chatId != -1){
                    showConfirmationDeleteNode(chatId, msgChat);
                }
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void importNode(){
        logDebug("importNode");

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
    }

     public void showConfirmationDeleteNode(final long chatId, final MegaChatMessage message){
         logDebug("Chat ID: " + chatId + ", Message ID: " + message.getMsgId());

         DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 switch (which){
                     case DialogInterface.BUTTON_POSITIVE:
                         if (chatC == null){
                             chatC = new ChatController(audioVideoPlayerLollipop);
                         }
                         chatC.deleteMessage(message, chatId);
                         isDeleteDialogShow = false;
                         finish();
                         break;

                     case DialogInterface.BUTTON_NEGATIVE:
                         //No button clicked
                         isDeleteDialogShow = false;
                         break;
                 }
             }
         };

         MaterialAlertDialogBuilder builder =
                 new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
         builder.setMessage(R.string.confirmation_delete_one_attachment);

         builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                 .setNegativeButton(R.string.general_cancel, dialogClickListener).show();

         isDeleteDialogShow = true;

         builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
             @Override
             public void onDismiss(DialogInterface dialog) {
                 isDeleteDialogShow = false;
             }
         });
     }

     public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
         logDebug("Nodes: " + nodeList.size() + ", Size: " + size);

         final String parentPathC = parentPath;
         final ArrayList<MegaNode> nodeListC = nodeList;
         final long sizeC = size;
         final ChatController chatC = new ChatController(this);

         Pair<MaterialAlertDialogBuilder, CheckBox> pair = confirmationDialog();
         MaterialAlertDialogBuilder builder = pair.first;
         CheckBox dontShowAgain = pair.second;

         builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
         builder.setPositiveButton(getString(R.string.general_save_to_device),
                 new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskSizeDownload("false");
                        }
                        chatC.download(parentPathC, nodeListC);
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

    /**
     * Create an MaterialAlertDialogBuilder with a "Do not show again" CheckBox.
     *
     * @return the first is MaterialAlertDialogBuilder, the second is CheckBox
     */
    private Pair<MaterialAlertDialogBuilder, CheckBox> confirmationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                scaleWidthPx(DIALOG_VIEW_MARGIN_LEFT_DP, outMetrics),
                scaleHeightPx(DIALOG_VIEW_MARGIN_TOP_DP, outMetrics),
                scaleWidthPx(DIALOG_VIEW_MARGIN_RIGHT_DP, outMetrics),
                0);

        CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ColorUtils.getThemeColor(this, android.R.attr.textColorSecondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

        return Pair.create(builder, dontShowAgain);
    }

    void releasePlaylist(){
        onPlaylist = false;
        if (player != null){
            playWhenReady = player.getPlayWhenReady();
            currentTime = player.getCurrentPosition();
            player.release();
        }
        playerLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        draggableView.setDraggable(true);

        getSupportFragmentManager().beginTransaction().remove(getSupportFragmentManager().findFragmentById(R.id.fragment_container)).commitNowAllowingStateLoss();

        tB.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_alpha_050));
        aB.setTitle(" ");

        supportInvalidateOptionsMenu();
        showActionStatusBar();
        createPlayer();
    }

    private boolean checkNoNetwork() {
        if (!isOnline(this)) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
            return true;
        }
        return false;
    }

    public void moveToTrash(){
        logDebug("moveToTrash");

        moveToRubbish = false;
        if (checkNoNetwork()) {
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
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
            String message= getResources().getString(R.string.confirmation_move_to_rubbish);
            builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        }
        else{
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
            String message= getResources().getString(R.string.confirmation_delete_from_mega);
            builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        }
    }


    public void showCopy(){
        logDebug("showCopy");

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
    }

    public void showMove(){
        logDebug("showMove");

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
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
        logDebug("showRenameDialog");
        final MegaNode node = megaApi.getNodeByHandle(handle);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
                scaleWidthPx(DIALOG_VIEW_MARGIN_LEFT_DP, outMetrics),
                scaleHeightPx(DIALOG_VIEW_MARGIN_TOP_LARGE_DP, outMetrics),
                scaleWidthPx(DIALOG_VIEW_MARGIN_RIGHT_DP, outMetrics),
                0);

        final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
        input.setSingleLine();
        input.setTextColor(ColorUtils.getThemeColor(this, android.R.attr.textColorSecondary));
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
        params1.setMargins(scaleWidthPx(DIALOG_VIEW_MARGIN_LEFT_DP, outMetrics), 0,
                scaleWidthPx(DIALOG_VIEW_MARGIN_RIGHT_DP, outMetrics), 0);

        final RelativeLayout error_layout = new RelativeLayout(AudioVideoPlayerLollipop.this);
        layout.addView(error_layout, params1);

        final ImageView error_icon = new ImageView(AudioVideoPlayerLollipop.this);
        error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
        error_layout.addView(error_icon);
        RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();

        params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        error_icon.setLayoutParams(params_icon);

        error_icon.setColorFilter(ContextCompat.getColor(AudioVideoPlayerLollipop.this, R.color.red_600));

        final TextView textError = new TextView(AudioVideoPlayerLollipop.this);
        error_layout.addView(textError);
        RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
        params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
        params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params_text_error.setMargins(scaleWidthPx(RENAME_DIALOG_ERROR_TEXT_MARGIN_LEFT_DP, outMetrics),
                0, 0, 0);
        textError.setLayoutParams(params_text_error);

        textError.setTextColor(ContextCompat.getColor(AudioVideoPlayerLollipop.this, R.color.red_600));

        error_layout.setVisibility(View.GONE);

        input.getBackground().mutate().clearColorFilter();
        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.teal_300), PorterDuff.Mode.SRC_ATOP);
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
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.teal_300), PorterDuff.Mode.SRC_ATOP);
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
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.red_600), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_string));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();

                    } else {
                        boolean result = matches(regex, value);
                        if (result) {
                            input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.red_600), PorterDuff.Mode.SRC_ATOP);
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.context_rename) + " "	+ node.getName());
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
        renameDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String value = input.getText().toString().trim();

                if (value.length() == 0) {
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.red_600), PorterDuff.Mode.SRC_ATOP);
                    textError.setText(getString(R.string.invalid_string));
                    error_layout.setVisibility(View.VISIBLE);
                    input.requestFocus();
                }
                else{
                    boolean result=matches(regex, value);
                    if(result){
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(audioVideoPlayerLollipop, R.color.red_600), PorterDuff.Mode.SRC_ATOP);
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

        if (checkNoNetwork()) {
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

        logDebug("Renaming " + node.getName() + " to " + newName);

        megaApi.renameNode(node, newName, this);
    }

    public static boolean matches(String regex, CharSequence input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        return m.find();
    }

    public void showRemoveLink(){
        AlertDialog removeLinkDialog;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
        TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
        TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
        TextView symbol = (TextView) dialoglayout.findViewById(R.id.dialog_link_symbol);
        TextView removeText = (TextView) dialoglayout.findViewById(R.id.dialog_link_text_remove);

        ((RelativeLayout.LayoutParams) removeText.getLayoutParams()).setMargins(
                scaleWidthPx(REMOVE_LINK_DIALOG_REMOVE_TEXT_MARGIN_LEFT_DP, outMetrics),
                scaleHeightPx(REMOVE_LINK_DIALOG_REMOVE_TEXT_MARGIN_TOP_DP, outMetrics),
                scaleWidthPx(REMOVE_LINK_DIALOG_REMOVE_TEXT_MARGIN_RIGHT_DP, outMetrics), 0);

        url.setVisibility(View.GONE);
        key.setVisibility(View.GONE);
        symbol.setVisibility(View.GONE);
        removeText.setVisibility(View.VISIBLE);

        removeText.setText(getString(R.string.context_remove_link_warning_text));

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;

        float scaleW = getScaleW(outMetrics, density);
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
        logDebug("showGetLinkActivity");
        Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
        linkIntent.putExtra("handle", handle);
        startActivity(linkIntent);
    }

    public void showPropertiesActivity(){
        Intent i = new Intent(this, FileInfoActivityLollipop.class);
        if (isOffline){
            i.putExtra(NAME, fileName);
            i.putExtra("adapterType", OFFLINE_ADAPTER);
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
            if (nC == null) {
                nC = new NodeController(this);
            }
            boolean fromIncoming = false;

            if (adapterType == SEARCH_ADAPTER || adapterType == RECENTS_ADAPTER) {
                fromIncoming = nC.nodeComesFromIncoming(node);
            }
            if (adapterType == INCOMING_SHARES_ADAPTER || fromIncoming) {
                i.putExtra("from", FROM_INCOMING_SHARES);
                i.putExtra("firstLevel", false);
            }
            else if(adapterType == INBOX_ADAPTER){
                i.putExtra("from", FROM_INBOX);
            }
            i.putExtra(NAME, node.getName());
        }
        startActivity(i);
        renamed = false;
    }

    public void downloadFile() {
        if (adapterType == OFFLINE_ADAPTER) {
            if (offlineNodeSaver == null) {
                offlineNodeSaver = new OfflineNodeSaver(this, dbH);
            }
            offlineNodeSaver.save(Collections.singletonList(mediaOffList.get(currentWindowIndex)), false, (intent, code) -> {
                startActivityForResult(intent, code);
                return Unit.INSTANCE;
            });
        } else if (adapterType == FILE_LINK_ADAPTER) {
            if (nC == null) {
                nC = new NodeController(this);
            }
            nC.downloadFileLink(currentDocument, uri.toString());
        }
        else if (fromChat){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasStoragePermission) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                    handleListM.add(nodeChat.getHandle());
                    return;
                }
            }

            if (chatC == null){
                chatC = new ChatController(this);
            }
            if (nodeChat != null){
                chatC.prepareForChatDownload(nodeChat);
            }
        }
        else {
            MegaNode node = megaApi.getNodeByHandle(handle);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasStoragePermission) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);

                    handleListM.add(node.getHandle());
                    return;
                }
            }

            ArrayList<Long> handleList = new ArrayList<Long>();
            handleList.add(node.getHandle());

            if(nC==null){
                nC = new NodeController(this, isFolderLink);
            }
            nC.prepareForDownload(handleList, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case REQUEST_WRITE_STORAGE:{
                boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (hasStoragePermission) {
                    if (adapterType == FILE_LINK_ADAPTER) {
                        if(nC==null){
                        nC = new NodeController(this, isFolderLink);
                    }
                        nC.downloadFileLink(currentDocument, uri.toString());
                    }
                    else if (fromChat) {
                        if (chatC == null){
                            chatC = new ChatController(this);
                        }
                        if (nodeChat != null){
                            chatC.prepareForChatDownload(nodeChat);
                        }
                    }
                    else{
                        if(nC==null){
                            nC = new NodeController(this, isFolderLink);
                        }
                        nC.prepareForDownload(handleListM, false);
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (intent == null) {
            return;
        }

        if (offlineNodeSaver != null && offlineNodeSaver.handleActivityResult(requestCode, resultCode, intent)) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
            long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
            long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);
            long[] nodeHandles = intent.getLongArrayExtra(NODE_HANDLES);

            if ((chatHandles != null && chatHandles.length > 0) || (contactHandles != null && contactHandles.length > 0)) {
                if (contactHandles != null && contactHandles.length > 0) {
                    ArrayList<MegaChatRoom> chats = new ArrayList<>();
                    ArrayList<MegaUser> users = new ArrayList<>();

                    for (int i=0; i<contactHandles.length; i++) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    if (chatHandles != null) {
                        for (int i = 0; i < chatHandles.length; i++) {
                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
                            if (chatRoom != null) {
                                chats.add(chatRoom);
                            }
                        }
                    }

                    if(nodeHandles!=null){
                        CreateChatListener listener = new CreateChatListener(chats, users, nodeHandles[0], this, CreateChatListener.SEND_FILE);
                        for (MegaUser user : users) {
                            MegaChatPeerList peers = MegaChatPeerList.createInstance();
                            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                            megaChatApi.createChat(false, peers, listener);
                        }
                    }
                    else{
                        logWarning("Error on sending to chat");
                    }
                }
                else {
                    countChat = chatHandles.length;
                    for (int i = 0; i < chatHandles.length; i++) {
                        megaChatApi.attachNode(chatHandles[i], nodeHandles[0], this);
                    }
                }
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
            logDebug("Local folder selected");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (adapterType == FILE_LINK_ADAPTER){
                if (nC == null) {
                    nC = new NodeController(this);
                }
                nC.downloadTo(currentDocument, parentPath, uri.toString());
            }
            else if (adapterType == FROM_CHAT) {
                chatC.prepareForDownload(intent, parentPath);
            }
            else {
                String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
                long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
                long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
                logDebug("URL: " + url + ", SIZE: " + size);

                if(nC==null){
                    nC = new NodeController(this, isFolderLink);
                }
                nC.checkSizeBeforeDownload(parentPath,url, size, hashes, false);
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
            if (checkNoNetwork()) {
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
        else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
            if (checkNoNetwork()) {
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
                    logDebug("cN != null, i = " + i + " of " + copyHandles.length);
                    megaApi.copyNode(cN, parent, this);
                }
                else{
                    logDebug("cN == null, i = " + i + " of " + copyHandles.length);
                    try {
                        statusDialog.dismiss();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
                    }
                    catch (Exception ex) {}
                }
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK){
            logDebug("REQUEST_CODE_SELECT_IMPORT_FOLDER OK");

            if(!isOnline(this)||megaApi==null) {
                try{
                    statusDialog.dismiss();
                } catch(Exception ex) {};
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);

            MegaNode target = null;
            target = megaApi.getNodeByHandle(toHandle);
            if(target == null){
                target = megaApi.getRootNode();
            }
            logDebug("TARGET: " + target.getName() + "and handle: " + target.getHandle());
            if (nodeChat != null) {
                logDebug("DOCUMENT: " + nodeChat.getName() + "_" + nodeChat.getHandle());
                if (target != null) {
                    megaApi.copyNode(nodeChat, target, this);
                }
                else {
                    logWarning("TARGET: null");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), -1);
                }
            }
            else{
                logWarning("DOCUMENT: null");
                showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), -1);
            }
        }
    }

    @Override
    public void onVideoEnabled(DecoderCounters counters) {
        logDebug("onVideoEnabled");
        video = true;
        updateContainers();
    }

    @Override
    public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        logDebug("decoderName: " + decoderName + ", initializedTimestampMs:" +
                initializedTimestampMs + ", initializationDurationMs:" + initializationDurationMs);
        video = true;
        updateContainers();
    }

    @Override
    public void onVideoInputFormatChanged(Format format) {
        logDebug("onVideoInputFormatChanged");
        video = true;
        updateContainers();
    }

    @Override
    public void onDroppedFrames(int count, long elapsedMs) {
        logDebug("count: " + count + ", elapsedMs: " + elapsedMs);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        logDebug("width: " + width + ", height: " + height + ", unappliedRotationDegrees: " +
                unappliedRotationDegrees + ", pixelWidthHeightRatio" + pixelWidthHeightRatio);
        video = true;
        updateContainers();
    }

    @Override
    public void onRenderedFirstFrame(Surface surface) {
        logDebug("onRenderedFirstFrame");
        video = true;
        updateContainers();
    }

    @Override
    public void onVideoDisabled(DecoderCounters counters) {
        logDebug("onVideoDisabled");
        video = false;
        updateContainers();
    }

    @Override
    protected void onStop() {
        super.onStop();
        logDebug("onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        logDebug("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        logDebug("onResume");
        if (!isOffline && !fromChat && !isFolderLink
                && adapterType != FILE_LINK_ADAPTER
                && !isZip && !fromDownload) {
            if (megaApi.getNodeByHandle(handle) == null) {
                finish();
            }
            updateFile();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        logDebug("onPause");
        abandonAudioFocus(audioFocusListener, mAudioManager, request);
    }

    @Override
    protected void onDestroy() {
        logDebug("onDestroy()");

        setImageDragVisibility(View.VISIBLE);

        if (megaApi != null) {
            megaApi.removeTransferListener(this);
            megaApi.removeGlobalListener(this);
            megaApi.httpServerStop();
        }

        if (megaApiFolder != null) {
            megaApiFolder.httpServerStop();
        }
        abandonAudioFocus(audioFocusListener, mAudioManager, request);

        if (player != null){
            player.release();
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        unregisterReceiver(receiver);
        unregisterReceiver(receiverToFinish);
        unregisterReceiver(chatCallUpdateReceiver);

        DRAGGING_THUMBNAIL_CALLBACKS.clear();

        super.onDestroy();
    }

    public void updateFile (){
        logDebug("updateFile");

        MegaNode file = null;

        if (fileName != null){
            file = megaApi.getNodeByHandle(handle);
            if (file != null){
                if (!fileName.equals(file.getName())) {
                    fileName = file.getName();
                    if (aB != null){
                        tB = (Toolbar) findViewById(R.id.call_toolbar);
                        if(tB==null){
                            logWarning("Tb is Null");
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
                    String localPath = getLocalFile(this, file.getName(), file.getSize());
                    File f = new File(downloadLocationDefaultPath, file.getName());
                    if(f.exists() && (f.length() == file.getSize())){
                        isOnMegaDownloads = true;
                    }
                    if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(file) != null && megaApi.getFingerprint(file).equals(megaApi.getFingerprint(localPath))))){
                        File mediaFile = new File(localPath);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
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

                        if(mi.totalMem>BUFFER_COMP){
                            logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                        }
                        else{
                            logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
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

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish");
        if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("File sent correctly");
                successSent++;

            }
            else{
                logWarning("File NOT sent: "+e.getErrorCode()+"___"+e.getErrorString());
                errorSent++;
            }

            if(countChat==errorSent+successSent){
                if(successSent==countChat){
                    if(countChat==1){
                        showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.sent_as_message), request.getChatHandle());
                    }
                    else{
                        showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.sent_as_message), -1);
                    }
                }
                else if(errorSent==countChat){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_attaching_node_from_cloud), -1);
                }
                else{
                    showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.error_sent_as_message), -1);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    public void showSnackbar(int type, String s, long idChat){
        showSnackbar(type, containerAudioVideoPlayer, s, idChat);
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
        logDebug("onTransferTemporaryError");

        if(e.getErrorCode() == MegaError.API_EOVERQUOTA){
            if (e.getValue() != 0) {
                logWarning("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
                showGeneralTransferOverQuotaWarning();
            }
        } else if (e.getErrorCode() == MegaError.API_EBLOCKED) {
            showTakenDownAlert(this);
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return false;
    }

    @Override
    public void onViewPositionChanged(float fractionScreen) {
        ivShadow.setAlpha(1 - fractionScreen);
    }

    @Override
    public void onBackPressed() {
        retryConnectionsAndSignalPresence();
        abandonAudioFocus(audioFocusListener, mAudioManager, request);

        if (!onPlaylist){
            super.onBackPressed();
            if (megaApi != null) {
                megaApi.removeTransferListener(this);
                megaApi.removeGlobalListener(this);
                megaApi.httpServerStop();
            }

            if (player != null){
                player.release();
            }

            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
            }

            unregisterReceiver(receiver);
            unregisterReceiver(receiverToFinish);

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
            int[] screenPositionForSwipeDismiss = getIntent().getIntArrayExtra(INTENT_EXTRA_KEY_SCREEN_POSITION_FOR_SWIPE_DISMISS);
            if (screenPositionForSwipeDismiss != null) {
                screenPosition = screenPositionForSwipeDismiss;
                draggableView.setScreenPosition(screenPosition);
            }
        }
        draggableView.setDraggableListener(this);
        ivShadow = new ImageView(this);
        ivShadow.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_alpha_060));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        container.addView(ivShadow, params);
        container.addView(draggableView);
        return container;
    }

    @Override
    public void onDragActivated(boolean activated) {
        if (activated) {
            ivShadow.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_alpha_060));
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
                playerView.hideController();
            }
            containerAudioVideoPlayer.setBackgroundColor(TRANSPARENT);

            playerLayout.setBackgroundColor(TRANSPARENT);
            appBarLayout.setBackgroundColor(TRANSPARENT);
            if (fromChatSavedInstance) {
                draggableView.setCurrentView(null);
            }
            else {
                draggableView.setCurrentView(playerView.getVideoSurfaceView());
            }
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
                    if (!isAbHide) {
                        showActionStatusBar();
                    }
                    containerAudioVideoPlayer.setBackgroundColor(BLACK);
                    playerLayout.setBackgroundColor(BLACK);
                    appBarLayout.setBackgroundColor(BLACK);
                }
            }, 300);
        }
    }

    public void openAdvancedDevices (long handleToDownload, boolean highPriority){
        logDebug("handleToDownload: " + handleToDownload + ", highPriority: " + highPriority);
        String externalPath = getExternalCardPath();

        if(externalPath!=null){
            logDebug("ExternalPath for advancedDevices: " + externalPath);
            MegaNode node = megaApi.getNodeByHandle(handleToDownload);
            if(node!=null){

                File newFile =  new File(node.getName());
                logDebug("File: " + newFile.getPath());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a file with the requested MIME type.
                String mimeType = MimeTypeList.getMimeType(newFile);
                logDebug("Mimetype: " + mimeType);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, node.getName());
                intent.putExtra("handleToDownload", handleToDownload);
                intent.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);
                try{
                    startActivityForResult(intent, WRITE_SD_CARD_REQUEST_CODE);
                }
                catch(Exception e){
                    logWarning("Exception in External SDCARD", e);
                    Environment.getExternalStorageDirectory();
                    Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
        else{
            logWarning("No external SD card");
            Environment.getExternalStorageDirectory();
            Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes, final boolean highPriority){
        logDebug("askSizeConfirmationBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;


        Pair<MaterialAlertDialogBuilder, CheckBox> pair = confirmationDialog();
        MaterialAlertDialogBuilder builder = pair.first;
        CheckBox dontShowAgain = pair.second;

        builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskSizeDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(audioVideoPlayerLollipop, isFolderLink);
                        }
                        nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC, highPriority);
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

    private void startPlayback() {
        audioFocusListener = new AudioFocusListener(this);
        request = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT);
        if (getAudioFocus(mAudioManager, audioFocusListener, request, AUDIOFOCUS_DEFAULT, STREAM_MUSIC_DEFAULT) && player != null) {
            player.setPlayWhenReady(true);
        }
    }

    public void stopPlayback() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
        abandonAudioFocus(audioFocusListener, mAudioManager, request);
    }

    public void askConfirmationNoAppInstaledBeforeDownload (String parentPath,String url, long size, long [] hashes, String nodeToDownload, final boolean highPriority){
        logDebug("askConfirmationNoAppInstaledBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        Pair<MaterialAlertDialogBuilder, CheckBox> pair = confirmationDialog();
        MaterialAlertDialogBuilder builder = pair.first;
        CheckBox dontShowAgain = pair.second;

        builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskNoAppDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(audioVideoPlayerLollipop, isFolderLink);
                        }
                        nC.download(parentPathC, urlC, sizeC, hashesC, highPriority);
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
        logDebug("onRequestFinish");

        if (request.getType() == MegaRequest.TYPE_RENAME){

            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (e.getErrorCode() == MegaError.API_OK){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_renamed), -1);
                updateFile();
            }
            else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_renamed), -1);
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
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
                }
                moveToRubbish = false;
                logDebug("Move to rubbish request finished");
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved), -1);
                    finish();
                }
                else{
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
                }
                logDebug("Move nodes request finished");
            }
        }
        else if (request.getType() == MegaRequest.TYPE_REMOVE){


            if (e.getErrorCode() == MegaError.API_OK){
                if (moveToTrashStatusDialog.isShowing()){
                    try {
                        moveToTrashStatusDialog.dismiss();
                    }
                    catch (Exception ex) {}
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_removed), -1);
                }
                finish();
            }
            else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_removed), -1);
            }
            logDebug("Remove request finished");
        }
        else if (request.getType() == MegaRequest.TYPE_COPY){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (e.getErrorCode() == MegaError.API_OK){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);
            }
            else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
                logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_OVERQUOTA_STORAGE);
                startActivity(intent);
                finish();
            }
            else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
                logWarning("PRE OVERQUOTA ERROR: " + e.getErrorCode());
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                startActivity(intent);
                finish();
            }
            else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
            }
            logDebug("copy nodes request finished");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporaryError");
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        logDebug("onUserAlertsUpdate");
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {
        logDebug("onNodesUpdate");
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

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (v.getId() == R.id.exo_play) {
                    getAudioFocus(mAudioManager, audioFocusListener, request, AUDIOFOCUS_DEFAULT, STREAM_MUSIC_DEFAULT);

                } else if (v.getId() == R.id.exo_pause) {
                    abandonAudioFocus(audioFocusListener, mAudioManager, request);

                }else if (loop && player != null){
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
                }
                break;

            case MotionEvent.ACTION_UP:
                if(v.getId() != R.id.exo_play && v.getId() != R.id.exo_pause && creatingPlaylist && player != null) {
                    if (v.getId() == R.id.exo_next) {
                        currentWindowIndex++;
                    }
                    setPlaylist(currentWindowIndex, 0);
                }
                break;

        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.exo_play_list:{
//              Ignore click before instantiate playlist if something was wrong obtaining the files
                if (!canProceedWithPlayList()) {
                    break;
                }
                handler.removeCallbacks(runnableActionStatusBar);
                instantiatePlaylist();
                break;
            }
        }
    }

    private boolean canProceedWithPlayList() {
        switch (adapterType)  {
            case OFFLINE_ADAPTER:
                return getMediaOffList() != null && !getMediaOffList().isEmpty();
            case ZIP_ADAPTER:
                return getZipMediaFiles() != null && !getZipMediaFiles().isEmpty();
            default:
                return getMediaHandles() != null && !getMediaHandles().isEmpty();
        }
    }

    void initPlaylist (int index, long time) {
        if (concatenatingMediaSource == null) return;

        creatingPlaylist = false;
        player.prepare(concatenatingMediaSource);
        player.seekTo(index, time);
    }

    void instantiatePlaylist(){
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            if (creatingPlaylist){
                currentTime = player.getCurrentPosition();
                setPlaylist(currentWindowIndex, currentTime);
            }
        }
        onPlaylist = true;
        progressBar.setVisibility(View.GONE);
        playerLayout.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);
        draggableView.setDraggable(false);
        tB.setBackgroundColor(ContextCompat.getColor(this, R.color.red_600));
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

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        showToolbar();
    }

    public class CreatePlayListTask extends AsyncTask<Void, Void, Void> {

        boolean errorCreatingPlaylist = false;

        @Override
        protected Void doInBackground(Void... voids) {
            logDebug("CreatePlayList doInBackground");
            playListCreated = false;
            creatingPlaylist = true;
            if (mediaSourcePlaylist == null || mediaSourcePlaylist.isEmpty() || concatenatingMediaSource == null) {
                MediaSource mSource = null;
                String localPath;
                Uri mediaUri;
                File mediaFile;
                mediaUris = new ArrayList<>();
                if (mediaSourcePlaylist == null) {
                    mediaSourcePlaylist = new ArrayList<>();
                }
                else {
                    mediaSourcePlaylist.clear();
                }
                downloadLocationDefaultPath = getDownloadLocation();
                if (isOffline) {
                    for (int i = 0; i < mediaOffList.size(); i++) {
                        MegaOffline currentNode = mediaOffList.get(i);
                        mSource = null;
                        mediaFile = getOfflineFile(audioVideoPlayerLollipop, currentNode);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && currentNode.getPath().contains(Environment.getExternalStorageDirectory().getPath())) {
                            mediaUri = FileProvider.getUriForFile(audioVideoPlayerLollipop, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                        }
                        else {
                            mediaUri = Uri.fromFile(mediaFile);
                        }
                        if (mediaUri != null) {
                            mediaUris.add(mediaUri);
                            mSource = new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null, null);
                        }
                        if(mSource != null) {
                            if (handle == Long.parseLong(mediaOffList.get(i).getHandle())) {
                                currentWindowIndex = i;
                            }
                            mediaSourcePlaylist.add(mSource);
                        }
                    }
                }
                else if (isZip) {
                    for (int i = 0; i< zipMediaFiles.size(); i++) {
                        mSource = null;
                        mediaUri = Uri.fromFile(zipMediaFiles.get(i));
                        if (mediaUri != null) {
                            mediaUris.add(mediaUri);
                            mSource = new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null, null);
                            if (mSource != null) {
                                if (fileName.equals(zipMediaFiles.get(i).getName())) {
                                    currentWindowIndex = i;
                                }
                                mediaSourcePlaylist.add(mSource);
                            }
                        }
                    }
                }
                else {
                    MegaNode n;
                    for (int i = 0; i < mediaHandles.size(); i++) {
                        if (isFolderLink) {
                            n = megaApiFolder.authorizeNode(megaApi.getNodeByHandle(mediaHandles.get(i)));
                            if (n == null){
                                n = megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(mediaHandles.get(i)));
                            }
                        }
                        else {
                            n = megaApi.getNodeByHandle(mediaHandles.get(i));
                        }
                        //either authorizeNode or getNodeByHandle can return null, so need to check
                        if(n == null){
                            continue;
                        }
                        mSource = null;
                        boolean isOnMegaDownloads = false;
                        localPath = getLocalFile(audioVideoPlayerLollipop, n.getName(), n.getSize());
                        File f = new File(downloadLocationDefaultPath, n.getName());
                        if (f.exists() && (f.length() == n.getSize())) {
                            isOnMegaDownloads = true;
                        }
                        String nodeFingerPrint;
                        String localPathFingerPrint;
                        if (isFolderLink) {
                            nodeFingerPrint = megaApiFolder.getFingerprint(n);
                            localPathFingerPrint = megaApiFolder.getFingerprint(localPath);
                        }
                        else {
                            nodeFingerPrint = megaApi.getFingerprint(n);
                            localPathFingerPrint = megaApi.getFingerprint(localPath);
                        }
                        if (localPath != null && (isOnMegaDownloads || (nodeFingerPrint != null && nodeFingerPrint.equals(localPathFingerPrint)))) {
                            mediaFile = new File(localPath);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                mediaUri = FileProvider.getUriForFile(audioVideoPlayerLollipop, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                            }
                            else {
                                mediaUri = Uri.fromFile(mediaFile);
                            }
                            if (mediaUri != null) {
                                mediaUris.add(mediaUri);
                                mSource = new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null, null);
                            }
                        }
                        else {
                            String url = null;
                            if (dbH != null && dbH.getCredentials() != null) {
                                url = megaApi.httpServerGetLocalLink(n);
                            }
                            else if (isFolderLink) {
                                url = megaApiFolder.httpServerGetLocalLink(n);
                            }
                            if (url != null) {
                                mediaUri = Uri.parse(url);
                                mediaUris.add(mediaUri);
                                mSource = new ExtractorMediaSource(mediaUri, dataSourceFactory, extractorsFactory, null, null);
                            }
                        }
                        if (mSource != null) {
                            if (handle == n.getHandle()) {
                                currentWindowIndex = i;
                            }
                            mediaSourcePlaylist.add(mSource);
                        }
                    }
                }

//                concatenatingMediaSource = new ConcatenatingMediaSource(mediaSourcePlaylist.toArray(new MediaSource[mediaSourcePlaylist.size()]));

                if (!mediaSourcePlaylist.isEmpty()) {
                    MediaSource[] arrayMediaSource = mediaSourcePlaylist.toArray(new MediaSource[mediaSourcePlaylist.size()]);
                    if (arrayMediaSource != null) {
                        concatenatingMediaSource = new ConcatenatingMediaSource(arrayMediaSource);
                    }
                    else {
                        errorCreatingPlaylist = true;
                    }
                }
                else {
                    errorCreatingPlaylist = true;
                }

                //            loopingMediaSource = new LoopingMediaSource(concatenatingMediaSource);
                //            player.prepare(loopingMediaSource);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            super.onPostExecute(avoid);
            logDebug("CreatePlayList onPostExecute");
            if (errorCreatingPlaylist && createPlayListErrorCounter < 2) {
                createPlayListErrorCounter++;
                logWarning("Error creating Playlist num: " + createPlayListErrorCounter);
                createPlayListTask = new CreatePlayListTask();
                createPlayListTask.execute();
            }
            else if (errorCreatingPlaylist && createPlayListErrorCounter == 2) {
                createPlaylistProgressBar.setVisibility(View.GONE);
            }
            else {
                createPlaylistProgressBar.setVisibility(View.GONE);
                playListCreated = true;
                enableNextButton();
                playList.setVisibility(View.VISIBLE);
                if (onPlaylist){
                    instantiatePlaylist();
                }
                if (playbackStateSaved == Player.STATE_ENDED){
                    setPlaylist(currentWindowIndex + 1, 0);
                }
            }
        }
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

    public void setPlaylistProgressBar(ProgressBar playlistProgressBar) {
        this.playlistProgressBar = playlistProgressBar;
    }

    public ArrayList<File> getZipMediaFiles() {
        return zipMediaFiles;
    }

    public int getCurrentWindowIndex (){
        return currentWindowIndex;
    }

    public ArrayList<MegaOffline> getMediaOffList(){
        return mediaOffList;
    }

    public boolean isFolderLink (){
        return isFolderLink;
    }

    public Handler getHandler () {
        return handler;
    }

    public String getQuerySearch() {
        return querySearch;
    }

    public MenuItem getSearchMenuItem() {
        return searchMenuItem;
    }
    public boolean isCreatingPlaylist () {
        return creatingPlaylist;
    }
}