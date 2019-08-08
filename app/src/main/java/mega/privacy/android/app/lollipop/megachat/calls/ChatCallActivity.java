package mega.privacy.android.app.lollipop.megachat.calls;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView;
import mega.privacy.android.app.components.OnSwipeTouchListener;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.CallNonContactNameListener;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.GroupCallAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.provider.Settings.System.DEFAULT_RINGTONE_URI;
import static android.view.View.GONE;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.showErrorAlertDialogGroupCall;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Util.context;

public class ChatCallActivity extends BaseActivity implements MegaChatRequestListenerInterface, MegaChatCallListenerInterface, MegaRequestListenerInterface, View.OnClickListener, SensorEventListener, KeyEvent.Callback {

    DatabaseHandler dbH = null;
    MegaUser myUser;
    Chronometer myChrono;

    public static int REMOTE_VIDEO_NOT_INIT = -1;
    public static int REMOTE_VIDEO_ENABLED = 1;
    public static int REMOTE_VIDEO_DISABLED = 0;
    final private int MAX_PEERS_GRID = 7;

    float widthScreenPX, heightScreenPX;

    long chatId;
    MegaChatRoom chat;
    MegaChatCall callChat;
    private MegaApiAndroid megaApi = null;
    MegaChatApiAndroid megaChatApi = null;
    private Handler handlerArrow1, handlerArrow2, handlerArrow3, handlerArrow4, handlerArrow5, handlerArrow6;
    Display display;
    DisplayMetrics outMetrics;
    float density;
    float scaleW;
    float scaleH;
    AppBarLayout appBarLayout;
    Toolbar tB;
    LinearLayout toolbarElements;
    TextView titleToolbar;
    TextView subtitleToobar;
    Chronometer callInProgressChrono;

    RelativeLayout mutateCallLayout;
    TextView mutateCallText;
    RelativeLayout mutateOwnCallLayout;

    LinearLayout linearParticipants;
    TextView participantText;
    TextView infoUsersBar;

    ActionBar aB;
    boolean avatarRequested = false;

    ArrayList<InfoPeerGroupCall> peersOnCall = new ArrayList<>();
    ArrayList<InfoPeerGroupCall> peersBeforeCall = new ArrayList<>();

    Timer timer = null;
    Timer ringerTimer = null;

    RelativeLayout smallElementsIndividualCallLayout;
    RelativeLayout bigElementsIndividualCallLayout;
    RelativeLayout bigElementsGroupCallLayout;

    RelativeLayout recyclerViewLayout;
    CustomizedGridCallRecyclerView recyclerView;

    RelativeLayout bigRecyclerViewLayout;
    LinearLayoutManager layoutManager;
    RecyclerView bigRecyclerView;

    GroupCallAdapter adapterGrid;
    GroupCallAdapter adapterList;

    int isRemoteVideo = REMOTE_VIDEO_NOT_INIT;

    Ringtone ringtone = null;
    Vibrator vibrator = null;
    ToneGenerator toneGenerator = null;

    //my avatar
    RelativeLayout myAvatarLayout;
    RoundedImageView myImage;
    TextView myInitialLetter;

    //contact avatar
    RelativeLayout contactAvatarLayout;
    RoundedImageView contactImage;
    TextView contactInitialLetter;

    RelativeLayout fragmentContainer;

    int totalVideosAllowed = 0;

    static ChatCallActivity chatCallActivityActivity = null;

    FloatingActionButton videoFAB;
    FloatingActionButton microFAB;
    FloatingActionButton rejectFAB;
    FloatingActionButton hangFAB;
    FloatingActionButton speakerFAB;
    FloatingActionButton answerCallFAB;
    boolean isNecessaryCreateAdapter = true;
    AudioManager audioManager;
    MediaPlayer thePlayer;

    FrameLayout fragmentContainerLocalCamera;
    FrameLayout fragmentContainerLocalCameraFS;
    FrameLayout fragmentContainerRemoteCameraFS;

    ViewGroup parentLocal;
    ViewGroup parentLocalFS;
    ViewGroup parentRemoteFS;

    private LocalCameraCallFragment localCameraFragment = null;
    private LocalCameraCallFullScreenFragment localCameraFragmentFS = null;
    private RemoteCameraCallFullScreenFragment remoteCameraFragmentFS = null;

    //Big elements for group call (more than 6 users)
    FrameLayout fragmentBigCameraGroupCall;
    ImageView microFragmentBigCameraGroupCall;
    ViewGroup parentBigCameraGroupCall;
    private BigCameraGroupCallFragment bigCameraGroupCallFragment = null;
    RelativeLayout avatarBigCameraGroupCallLayout;
    ImageView avatarBigCameraGroupCallMicro;
    RoundedImageView avatarBigCameraGroupCallImage;
    TextView avatarBigCameraGroupCallInitialLetter;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;

    AppRTCAudioManager rtcAudioManager;

    Animation shake;

    long translationAnimationDuration = 500;
    long alphaAnimationDuration = 600;
    long alphaAnimationDurationArrow = 1000;

    LinearLayout linearFAB;
    LinearLayout linearHangFAB;

    RelativeLayout relativeCall;
    LinearLayout linearArrowCall;
    ImageView firstArrowCall;
    ImageView secondArrowCall;
    ImageView thirdArrowCall;
    ImageView fourArrowCall;

    RelativeLayout relativeVideo;
    LinearLayout linearArrowVideo;
    ImageView firstArrowVideo;
    ImageView secondArrowVideo;
    ImageView thirdArrowVideo;
    ImageView fourArrowVideo;

    InfoPeerGroupCall peerSelected = null;
    boolean isManualMode = false;
    int statusBarHeight = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");
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
                onBackPressed();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateScreenStatus() {
        log("updateScreenStatusInProgress:");
        if ((chatId != -1) && (megaChatApi != null)) {
            chat = megaChatApi.getChatRoom(chatId);
            if (chat.isGroup()) {
                log("updateScreenStatusInProgress:group");
                if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
                    peersBeforeCall.clear();
                }
                callChat = megaChatApi.getChatCall(chatId);
                if (callChat != null) {
                    int callStatus = callChat.getStatus();
                    if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                        log("updateScreenStatusInProgress:group:RING_IN");
                        RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                        layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutExtend);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                    }else if((callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS)||(callStatus == MegaChatCall.CALL_STATUS_JOINING)){
                        log("updateScreenStatusInProgress:group:IN_PROGRESS||JOINING");
                        RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutExtend);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                    }else if(callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                        log("updateScreenStatusInProgress:REQUEST_SENT");
                        RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutExtend);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                    }else{
                        log("updateScreenStatusInProgress:group:OTHER = "+callStatus);
                    }
                    checkParticipants(callChat);
                    updateSubtitleNumberOfVideos();
                }

            } else {
                log("updateScreenStatusInProgress:individual");
                callChat = megaChatApi.getChatCall(chatId);
                if (callChat != null) {
                    int callStatus = callChat.getStatus();
                    if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                        log("updateScreenStatusInProgress:RING_IN");
                        RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                        layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutExtend);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                        setProfileAvatar(megaChatApi.getMyUserHandle());
                        setProfileAvatar(chat.getPeerHandle(0));
                        myAvatarLayout.setVisibility(View.VISIBLE);
                        contactAvatarLayout.setVisibility(View.VISIBLE);

                    } else if(callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                        log("updateScreenStatusInProgress:IN_PROGRESS");

                        RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutExtend);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                        setProfileAvatar(megaChatApi.getMyUserHandle());
                        setProfileAvatar(chat.getPeerHandle(0));
                        myAvatarLayout.setVisibility(View.VISIBLE);
                        contactAvatarLayout.setVisibility(View.VISIBLE);

                    }  else if(callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                        log("updateScreenStatusInProgress:OTHER: "+callStatus);

                        RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutExtend);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                        setProfileAvatar(megaChatApi.getMyUserHandle());
                        setProfileAvatar(chat.getPeerHandle(0));
                        myAvatarLayout.setVisibility(View.VISIBLE);
                    }

                }
                updateLocalVideoStatus();
                updateLocalAudioStatus();
                updateRemoteVideoStatus(-1, -1);
                updateRemoteAudioStatus(-1, -1);
            }

            stopAudioSignals();
            updateSubTitle();
        }

    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log("onNewIntent");

        Bundle extras = intent.getExtras();
        log(getIntent().getAction());
        if (extras != null) {
            long newChatId = extras.getLong("chatHandle", -1);
            log("onNewIntent:New newChatId: "+newChatId);
            if ((chatId != -1) && (chatId == newChatId) && (megaChatApi != null)) {
                chat = megaChatApi.getChatRoom(chatId);

                if(rtcAudioManager==null){
                    rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());
                }
                if(rtcAudioManager!=null){
                    rtcAudioManager.stop();
                    rtcAudioManager.start(null);
                }
                updateLocalSpeakerStatus();
                updateScreenStatus();
//                if (chat.isGroup()) {
//                    log("onNewIntent:group: SAME call");
//                    updateScreenStatus();
//                } else {
//                    log("onNewIntent:individual: SAME call");
//                }

            } else {
                log("onNewIntent: DIFFERENT call");

                //Check the new call if in progress
                if (megaChatApi != null) {
                    chatId = newChatId;
                    chat = megaChatApi.getChatRoom(chatId);
                    callChat = megaChatApi.getChatCall(chatId);
                    titleToolbar.setText(chat.getTitle());
                    updateSubTitle();
                    updateScreenStatus();
                    updateLocalSpeakerStatus();

                    if ((callChat != null) && (callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS)) {
                        log("onNewIntent:Start call Service");
                        Intent intentService = new Intent(this, CallService.class);
                        intentService.putExtra("chatHandle", callChat.getChatid());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            this.startForegroundService(intentService);
                        } else {
                            this.startService(intentService);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calls_chat);

        chatCallActivityActivity = this;

        MegaApplication.setShowPinScreen(true);

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        widthScreenPX = outMetrics.widthPixels;
        heightScreenPX = outMetrics.heightPixels - statusBarHeight;

        MegaApplication app = (MegaApplication) getApplication();
        if (megaApi == null) {
            megaApi = app.getMegaApi();
        }

        log("retryPendingConnections()");
        if (megaApi != null) {
            log("---------retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        if (megaChatApi != null) {
            megaChatApi.retryPendingConnections(false, null);
        }

        if (megaChatApi == null) {
            megaChatApi = app.getMegaChatApi();
        }

        if (megaApi == null || megaApi.getRootNode() == null) {
            log("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
            log("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        megaChatApi.addChatCallListener(this);
        myUser = megaApi.getMyUser();

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        try {
            field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable ignored) { }

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fragmentContainer = (RelativeLayout) findViewById(R.id.file_info_fragment_container);

        tB = (Toolbar) findViewById(R.id.call_toolbar);
        if (tB == null) {
            log("Toolbar is Null");
            return;
        }
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setTitle(null);
        aB.setSubtitle(null);

        toolbarElements = (LinearLayout) tB.findViewById(R.id.toolbar_elements);
        titleToolbar = (TextView) tB.findViewById(R.id.title_toolbar);
        titleToolbar.setText(" ");
        subtitleToobar = (TextView) tB.findViewById(R.id.subtitle_toolbar);
        callInProgressChrono = (Chronometer) tB.findViewById(R.id.simple_chronometer);
        linearParticipants = (LinearLayout) tB.findViewById(R.id.ll_participants);
        participantText = (TextView) tB.findViewById(R.id.participants_text);
        linearParticipants.setVisibility(View.GONE);

        myChrono = new Chronometer(context);

        totalVideosAllowed = megaChatApi.getMaxVideoCallParticipants();

        mutateOwnCallLayout = (RelativeLayout) findViewById(R.id.mutate_own_call);
        mutateOwnCallLayout.setVisibility(GONE);
        mutateCallLayout = (RelativeLayout) findViewById(R.id.mutate_contact_call);
        mutateCallLayout.setVisibility(GONE);
        mutateCallText = (TextView) findViewById(R.id.text_mutate_contact_call);

        smallElementsIndividualCallLayout = (RelativeLayout) findViewById(R.id.small_elements_individual_call);
        smallElementsIndividualCallLayout.setVisibility(GONE);

        bigElementsIndividualCallLayout = (RelativeLayout) findViewById(R.id.big_elements_individual_call);
        bigElementsIndividualCallLayout.setVisibility(GONE);

        linearFAB = (LinearLayout) findViewById(R.id.linear_buttons);
        RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        linearFAB.setLayoutParams(layoutCompress);
        linearFAB.requestLayout();
        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

        linearHangFAB = (LinearLayout) findViewById(R.id.linear_buttons_hang);

        infoUsersBar = (TextView) findViewById(R.id.info_users_bar);
        infoUsersBar.setVisibility(GONE);

        isManualMode = false;

        relativeCall = (RelativeLayout) findViewById(R.id.relative_answer_call_fab);
        relativeCall.requestLayout();
        relativeCall.setVisibility(GONE);

        linearArrowCall = (LinearLayout) findViewById(R.id.linear_arrow_call);
        linearArrowCall.setVisibility(GONE);
        firstArrowCall = (ImageView) findViewById(R.id.first_arrow_call);
        secondArrowCall = (ImageView) findViewById(R.id.second_arrow_call);
        thirdArrowCall = (ImageView) findViewById(R.id.third_arrow_call);
        fourArrowCall = (ImageView) findViewById(R.id.four_arrow_call);

        relativeVideo = (RelativeLayout) findViewById(R.id.relative_video_fab);
        relativeVideo.requestLayout();
        relativeVideo.setVisibility(GONE);

        linearArrowVideo = (LinearLayout) findViewById(R.id.linear_arrow_video);
        linearArrowVideo.setVisibility(GONE);
        firstArrowVideo = (ImageView) findViewById(R.id.first_arrow_video);
        secondArrowVideo = (ImageView) findViewById(R.id.second_arrow_video);
        thirdArrowVideo = (ImageView) findViewById(R.id.third_arrow_video);
        fourArrowVideo = (ImageView) findViewById(R.id.four_arrow_video);

        answerCallFAB = (FloatingActionButton) findViewById(R.id.answer_call_fab);
        answerCallFAB.setVisibility(GONE);

        videoFAB = (FloatingActionButton) findViewById(R.id.video_fab);
        videoFAB.setOnClickListener(this);
        videoFAB.setVisibility(GONE);

        rejectFAB = (FloatingActionButton) findViewById(R.id.reject_fab);
        rejectFAB.setOnClickListener(this);
        rejectFAB.setVisibility(GONE);

        speakerFAB = (FloatingActionButton) findViewById(R.id.speaker_fab);
        speakerFAB.setOnClickListener(this);
        speakerFAB.setVisibility(GONE);

        microFAB = (FloatingActionButton) findViewById(R.id.micro_fab);
        microFAB.setOnClickListener(this);
        microFAB.setVisibility(GONE);

        hangFAB = (FloatingActionButton) findViewById(R.id.hang_fab);
        hangFAB.setOnClickListener(this);
        hangFAB.setVisibility(GONE);

        shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());

        //Cameras in Group call
        bigElementsGroupCallLayout = (RelativeLayout) findViewById(R.id.big_elements_group_call);
        bigElementsGroupCallLayout.setVisibility(GONE);

        //Recycler View for 1-6 peers
        recyclerViewLayout = (RelativeLayout) findViewById(R.id.rl_recycler_view);
        recyclerViewLayout.setVisibility(GONE);
        recyclerView = (CustomizedGridCallRecyclerView) findViewById(R.id.recycler_view_cameras);
        recyclerView.setPadding(0, 0, 0, 0);
        recyclerView.setColumnWidth((int) widthScreenPX);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setVisibility(GONE);

        //Big elements group calls
        parentBigCameraGroupCall = (ViewGroup) findViewById(R.id.parent_layout_big_camera_group_call);
        ViewGroup.LayoutParams paramsBigCameraGroupCall = (ViewGroup.LayoutParams) parentBigCameraGroupCall.getLayoutParams();
        if (widthScreenPX < heightScreenPX) {
            paramsBigCameraGroupCall.width = (int) widthScreenPX;
            paramsBigCameraGroupCall.height = (int) widthScreenPX;
        } else {
            paramsBigCameraGroupCall.width = (int) heightScreenPX;
            paramsBigCameraGroupCall.height = (int) heightScreenPX;
        }

        parentBigCameraGroupCall.setLayoutParams(paramsBigCameraGroupCall);
        fragmentBigCameraGroupCall = (FrameLayout) findViewById(R.id.fragment_big_camera_group_call);
        fragmentBigCameraGroupCall.setVisibility(View.GONE);
        microFragmentBigCameraGroupCall = (ImageView) findViewById(R.id.micro_fragment_big_camera_group_call);
        microFragmentBigCameraGroupCall.setVisibility(View.GONE);

        avatarBigCameraGroupCallLayout = (RelativeLayout) findViewById(R.id.rl_avatar_big_camera_group_call);
        avatarBigCameraGroupCallMicro = (ImageView) findViewById(R.id.micro_avatar_big_camera_group_call);
        avatarBigCameraGroupCallImage = (RoundedImageView) findViewById(R.id.image_big_camera_group_call);
        avatarBigCameraGroupCallInitialLetter = (TextView) findViewById(R.id.initial_letter_big_camera_group_call);
        avatarBigCameraGroupCallMicro.setVisibility(GONE);
        avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
        parentBigCameraGroupCall.setVisibility(View.GONE);

        //Recycler View for 7-8 peers (because 9-10 without video)
        bigRecyclerViewLayout = (RelativeLayout) findViewById(R.id.rl_big_recycler_view);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        bigRecyclerView = (RecyclerView) findViewById(R.id.big_recycler_view_cameras);
        bigRecyclerView.setLayoutManager(layoutManager);
        RelativeLayout.LayoutParams bigRecyclerViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.parent_layout_big_camera_group_call);
        bigRecyclerViewParams.addRule(RelativeLayout.BELOW, 0);
        bigRecyclerViewLayout.setLayoutParams(bigRecyclerViewParams);
        bigRecyclerViewLayout.requestLayout();
        bigRecyclerView.setVisibility(GONE);
        bigRecyclerViewLayout.setVisibility(GONE);

        //Local camera small
        parentLocal = (ViewGroup) findViewById(R.id.parent_layout_local_camera);
        fragmentContainerLocalCamera = (FrameLayout) findViewById(R.id.fragment_container_local_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fragmentContainerLocalCamera.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerLocalCamera.setLayoutParams(params);
        fragmentContainerLocalCamera.setOnTouchListener(new OnDragTouchListener(fragmentContainerLocalCamera, parentLocal));
        parentLocal.setVisibility(View.GONE);
        fragmentContainerLocalCamera.setVisibility(View.GONE);

        //Local camera Full Screen
        parentLocalFS = (ViewGroup) findViewById(R.id.parent_layout_local_camera_FS);
        fragmentContainerLocalCameraFS = (FrameLayout) findViewById(R.id.fragment_container_local_cameraFS);
        RelativeLayout.LayoutParams paramsFS = (RelativeLayout.LayoutParams) fragmentContainerLocalCameraFS.getLayoutParams();
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerLocalCameraFS.setLayoutParams(paramsFS);
        parentLocalFS.setVisibility(View.GONE);
        fragmentContainerLocalCameraFS.setVisibility(View.GONE);

        //Remote camera Full Screen
        parentRemoteFS = (ViewGroup) findViewById(R.id.parent_layout_remote_camera_FS);
        fragmentContainerRemoteCameraFS = (FrameLayout) findViewById(R.id.fragment_container_remote_cameraFS);
        RelativeLayout.LayoutParams paramsRemoteFS = (RelativeLayout.LayoutParams) fragmentContainerRemoteCameraFS.getLayoutParams();
        paramsRemoteFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        paramsRemoteFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerRemoteCameraFS.setLayoutParams(paramsRemoteFS);
        fragmentContainerRemoteCameraFS.setOnTouchListener(new OnDragTouchListener(fragmentContainerRemoteCameraFS, parentRemoteFS));
        parentRemoteFS.setVisibility(View.GONE);
        fragmentContainerRemoteCameraFS.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            myAvatarLayout = (RelativeLayout) findViewById(R.id.call_chat_my_image_rl);
            myAvatarLayout.setVisibility(View.GONE);
            myImage = (RoundedImageView) findViewById(R.id.call_chat_my_image);
            myInitialLetter = (TextView) findViewById(R.id.call_chat_my_image_initial_letter);

            contactAvatarLayout = (RelativeLayout) findViewById(R.id.call_chat_contact_image_rl);
            contactAvatarLayout.setVisibility(View.GONE);
            contactImage = (RoundedImageView) findViewById(R.id.call_chat_contact_image);
            contactInitialLetter = (TextView) findViewById(R.id.call_chat_contact_image_initial_letter);

            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_video_off));

            //Contact's avatar
            chatId = extras.getLong("chatHandle", -1);
            if ((chatId != -1) && (megaChatApi != null)) {
                chat = megaChatApi.getChatRoom(chatId);
                callChat = megaChatApi.getChatCall(chatId);
                if (callChat == null) {
                    megaChatApi.removeChatCallListener(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        super.finishAndRemoveTask();
                    } else {
                        super.finish();
                    }
                    return;
                }

                log("Start call Service");
                Intent intentService = new Intent(this, CallService.class);
                intentService.putExtra("chatHandle", callChat.getChatid());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(intentService);
                } else {
                    this.startService(intentService);
                }

                audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                int callStatus = callChat.getStatus();
                log("The status of the callChat is: " + callStatus);
                titleToolbar.setText(chat.getTitle());
                updateSubTitle();
                updateLocalSpeakerStatus();
                if (chat.isGroup()) {
                    smallElementsIndividualCallLayout.setVisibility(View.GONE);
                    bigElementsIndividualCallLayout.setVisibility(View.GONE);
                    bigElementsGroupCallLayout.setVisibility(View.VISIBLE);
                } else {
                    smallElementsIndividualCallLayout.setVisibility(View.VISIBLE);
                    bigElementsIndividualCallLayout.setVisibility(View.VISIBLE);
                    bigElementsGroupCallLayout.setVisibility(View.GONE);
                    bigRecyclerView.setVisibility(GONE);
                    bigRecyclerViewLayout.setVisibility(GONE);
                }

                if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                    log("onCreate:RING_IN");
                    ringtone = RingtoneManager.getRingtone(this, DEFAULT_RINGTONE_URI);
                    ringerTimer = new Timer();
                    MyRingerTask myRingerTask = new MyRingerTask();
                    ringerTimer.schedule(myRingerTask, 0, 500);

                    if(audioManager.getRingerMode()!=AudioManager.RINGER_MODE_SILENT){
                        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0){
                            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            long[] pattern = {0, 1000, 500, 500, 1000};

                            if (vibrator != null) {
                                if (vibrator.hasVibrator()) {
                                    vibrator.vibrate(pattern, 0);
                                }
                            }
                        }
                    }

                    if (chat.isGroup()) {
                        log("onCreate:RING_IN:group");
                        if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
                            peersBeforeCall.clear();
                        }
                        if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                            peersOnCall.clear();
                        }
                        if ((callChat!=null)&&(callChat.getPeeridParticipants().size() > 0)) {
                            for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                                long userPeerid = callChat.getPeeridParticipants().get(i);
                                long userClientid = callChat.getClientidParticipants().get(i);
                                InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);
                                log("onCreate:RING_IN -> "+userPeer.getPeerId()+" added in peersBeforeCall");
                                if(peersBeforeCall!=null){
                                    peersBeforeCall.add((peersBeforeCall.size() == 0 ? 0 : (peersBeforeCall.size() - 1)), userPeer);
                                }
                            }
                            updatePeers();

                        }
                    }else{
                        log("onCreate:RING_IN:individual");
                        myAvatarLayout.setVisibility(View.VISIBLE);
                        contactAvatarLayout.setVisibility(View.VISIBLE);
                        setProfileAvatar(megaChatApi.getMyUserHandle());
                        setProfileAvatar(chat.getPeerHandle(0));
                    }

                } else if ((callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callStatus == MegaChatCall.CALL_STATUS_JOINING)) {
                    log("onCreate:IN_PROGRESS||JOINING");
                    if(rtcAudioManager==null){
                        rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());
                    }
                    if(rtcAudioManager!=null){
                        rtcAudioManager.stop();
                        rtcAudioManager.start(null);
                    }
                    updateScreenStatus();

                } else if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    log("onCreate:REQUEST_SENT");

                    if(rtcAudioManager==null){
                        rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());
                    }
                    if(rtcAudioManager!=null){
                        rtcAudioManager.stop();
                        rtcAudioManager.start(null);
                    }

                    if(audioManager.getRingerMode()!=AudioManager.RINGER_MODE_SILENT){
                        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0){
                            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
                            thePlayer = MediaPlayer.create(getApplicationContext(), R.raw.outgoing_voice_video_call);
                            if (volume == 0) {
                                toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 60000);
                            } else {
                                thePlayer.setLooping(true);
                                thePlayer.start();
                            }
                        }
                    }

                    if (chat.isGroup()) {
                        log("onCreate:REQUEST_SENT:group");
                        if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
                            peersBeforeCall.clear();
                        }
                        if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                            peersOnCall.clear();
                        }
                        if((callChat!=null)&&(peersOnCall!=null)){
                            InfoPeerGroupCall myPeer = new InfoPeerGroupCall(megaChatApi.getMyUserHandle(), megaChatApi.getMyClientidHandle(chatId), megaChatApi.getMyFullname(), callChat.hasLocalVideo(), callChat.hasLocalAudio(), false, true, null);
                            log("onCreate:REQUEST_SENT -> I added in peersOnCall");
                            peersOnCall.add(myPeer);
                            updatePeers();

                        }
                    } else {
                        log("onCreate:REQUEST_SENT:individual");
                        setProfileAvatar(megaChatApi.getMyUserHandle());
                        setProfileAvatar(chat.getPeerHandle(0));
                        myAvatarLayout.setVisibility(View.VISIBLE);
                    }
                    updateLocalVideoStatus();
                    updateLocalAudioStatus();

                } else {
                    log("onCreate:other status: "+callStatus);
                }
            }
        }
        if (checkPermissions()) {
//            checkPermissionsWriteLog();
            showInitialFABConfiguration();
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getType());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate");
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish: type: "+request.getType());

        if(request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {

            if (e.getErrorCode() != MegaError.API_OK) {
                log("onRequestFinish:TYPE_GET_ATTR_USER: OK");

                File avatar = buildAvatarFile(this, request.getEmail() + ".jpg");
                Bitmap bitmap = null;

                if (isFileAvailable(avatar)){
                    if (avatar.length() > 0){
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                        }else{
                            log("onRequestFinish:Avatar found it");
                            if(chat.isGroup()){
                                log("onRequestFinish:Avatar found it: Group -> big avatar");
                                avatarBigCameraGroupCallInitialLetter.setVisibility(GONE);
                                avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
                                avatarBigCameraGroupCallImage.setImageBitmap(bitmap);

                            }else{
                                if (callChat == null) {
                                    callChat = megaChatApi.getChatCall(chatId);
                                }
                                if (callChat != null) {
                                    if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                                        log("onRequestFinish:Avatar found it: Individual -> myImage");
                                        myImage.setImageBitmap(bitmap);
                                        myInitialLetter.setVisibility(GONE);
                                    } else {
                                        log("onRequestFinish:Avatar found it: Individual -> contactImage");
                                        contactImage.setImageBitmap(bitmap);
                                        contactInitialLetter.setVisibility(GONE);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) { }

    //Individual Call:  Profile
    public void setProfileAvatar(long peerId) {
        log("setProfileAvatar  peerId = "+peerId);
        Bitmap bitmap = null;
        File avatar = null;
        String email;
        String name;
        if(peerId == megaChatApi.getMyUserHandle()){
            email = megaChatApi.getMyEmail();
            name = megaChatApi.getMyFullname();
        }else{
            email = chat.getPeerEmail(0);
            name = chat.getPeerFullname(0);
        }
        if (!TextUtils.isEmpty(email)) {
            avatar = buildAvatarFile(this, email + ".jpg");
        }

        if ((isFileAvailable(avatar)) && (avatar.length() > 0)) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
//                bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(this, bitmap, 3);
                if (bitmap != null) {
                    if((megaChatApi != null) && (callChat == null)){
                        callChat = megaChatApi.getChatCall(chatId);
                    }
                    if(callChat!=null){
                        if(callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT){
                            if(peerId == megaChatApi.getMyUserHandle()){
                                contactImage.setImageBitmap(bitmap);
                                contactInitialLetter.setVisibility(GONE);
                            }else{
                                myImage.setImageBitmap(bitmap);
                                myInitialLetter.setVisibility(GONE);
                            }
                        }else{
                            if(peerId == megaChatApi.getMyUserHandle()){
                                myImage.setImageBitmap(bitmap);
                                myInitialLetter.setVisibility(GONE);
                            }else{
                                contactImage.setImageBitmap(bitmap);
                                contactInitialLetter.setVisibility(GONE);
                            }
                        }
                    }
                }else{
                    if (peerId != megaChatApi.getMyUserHandle()) {
                        avatar.delete();
                        if (!avatarRequested) {
                            avatarRequested = true;
                            megaApi.getUserAvatar(email,buildAvatarFile(this,email + ".jpg").getAbsolutePath(),this);
                        }
                    }
                    createDefaultAvatar(peerId, name);
                }
        }else{
            if (peerId != megaChatApi.getMyUserHandle()) {
                if (!avatarRequested) {
                    avatarRequested = true;
                    megaApi.getUserAvatar(email,buildAvatarFile(this,email + ".jpg").getAbsolutePath(),this);
                }
            }
            createDefaultAvatar(peerId, name);
        }
    }

    //Individual Call: Default Avatar
    public void createDefaultAvatar(long peerId, String peerName) {
        log("createDefaultAvatar  peerId = "+peerId);
        String firstLetter = peerName.charAt(0) + "";
        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);

        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(peerId));
        if (color != null) {
            p.setColor(Color.parseColor(color));
        } else {
            p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        } else {
            radius = defaultAvatar.getHeight() / 2;
        }
        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);
        if(callChat == null){
            callChat = megaChatApi.getChatCall(chatId);
        }
        if(callChat!=null){
            if(callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT){
                if(peerId == megaChatApi.getMyUserHandle()){
                    contactImage.setImageBitmap(defaultAvatar);
                    contactInitialLetter.setText(firstLetter);
                    contactInitialLetter.setTextSize(60);
                    contactInitialLetter.setTextColor(Color.WHITE);
                    contactInitialLetter.setVisibility(View.VISIBLE);
                }else{
                    myImage.setImageBitmap(defaultAvatar);
                    myInitialLetter.setText(firstLetter);
                    myInitialLetter.setTextSize(40);
                    myInitialLetter.setTextColor(Color.WHITE);
                    myInitialLetter.setVisibility(View.VISIBLE);
                }
            }else{
                if(peerId == megaChatApi.getMyUserHandle()){
                    myImage.setImageBitmap(defaultAvatar);
                    myInitialLetter.setText(firstLetter);
                    myInitialLetter.setTextSize(40);
                    myInitialLetter.setTextColor(Color.WHITE);
                    myInitialLetter.setVisibility(View.VISIBLE);
                }else{
                    contactImage.setImageBitmap(defaultAvatar);
                    contactInitialLetter.setText(firstLetter);
                    contactInitialLetter.setTextSize(60);
                    contactInitialLetter.setTextColor(Color.WHITE);
                    contactInitialLetter.setVisibility(View.VISIBLE);
                }
            }
        }
    }


    //Group call: avatar
    public void setProfilePeerSelected(long peerId, String fullName, String peerEmail) {
        log("setProfilePeerSelected");

        if (peerId == megaChatApi.getMyUserHandle()) {
            //My peer, other client
            peerEmail = megaChatApi.getMyEmail();
        } else {
            //Contact
            if((peerEmail == null) || (peerId != peerSelected.getPeerId())){
                peerEmail = megaChatApi.getContactEmail(peerId);
                if (peerEmail == null) {
                    CallNonContactNameListener listener = new CallNonContactNameListener(this, peerId, true, fullName);
                    megaChatApi.getUserEmail(peerId, listener);
                }
            }
        }


        if(peerEmail!=null){
            File avatar = null;
            Bitmap bitmap = null;

            if (!TextUtils.isEmpty(peerEmail)) {
                avatar = buildAvatarFile(this, peerEmail + ".jpg");
            }

            if (isFileAvailable(avatar) && (avatar.length() > 0)) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
//                bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(this, bitmap, 3);
                if (bitmap != null) {
                    avatarBigCameraGroupCallInitialLetter.setVisibility(GONE);
                    avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
                    avatarBigCameraGroupCallImage.setImageBitmap(bitmap);

                } else {
                    if (peerId != megaChatApi.getMyUserHandle()) {
                        avatar.delete();
                        if (megaApi == null) {
                            return;
                        }
                        megaApi.getUserAvatar(peerEmail,buildAvatarFile(this,peerEmail + ".jpg").getAbsolutePath(),this);
                    }
                    createDefaultAvatarPeerSelected(peerId,fullName,peerEmail);
                }
            } else {
                if(peerId != megaChatApi.getMyUserHandle()){
                    if (megaApi == null) {
                        return;
                    }
                    megaApi.getUserAvatar(peerEmail,buildAvatarFile(this,peerEmail + ".jpg").getAbsolutePath(),this);
                }
                createDefaultAvatarPeerSelected(peerId, fullName, peerEmail);
            }
        }
    }

    //Group call: default avatar of peer selected
    public void createDefaultAvatarPeerSelected(long peerId, String peerName, String peerEmail) {
        log("createDefaultAvatarPeerSelected");
        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);
        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(peerId));
        if (color != null) {
            p.setColor(Color.parseColor(color));
        } else {
            p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        } else {
            radius = defaultAvatar.getHeight() / 2;
        }
        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);

        avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallImage.setImageBitmap(defaultAvatar);

        if((peerName != null) && (peerName.trim().length() > 0)) {
            String firstLetter = peerName.charAt(0) + "";
            firstLetter = firstLetter.toUpperCase(Locale.getDefault());
            avatarBigCameraGroupCallInitialLetter.setText(firstLetter);
            avatarBigCameraGroupCallInitialLetter.setTextColor(Color.WHITE);
            avatarBigCameraGroupCallInitialLetter.setVisibility(View.VISIBLE);
        } else {
            if((peerEmail != null) && (peerEmail.length() > 0)){
                String firstLetter = peerEmail.charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                avatarBigCameraGroupCallInitialLetter.setText(firstLetter);
                avatarBigCameraGroupCallInitialLetter.setTextColor(Color.WHITE);
                avatarBigCameraGroupCallInitialLetter.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void hideActionBar() {
        if (aB != null && aB.isShowing()) {
            if (tB != null) {
                tB.animate().translationY(-220).setDuration(800L).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        aB.hide();
                    }
                }).start();
            } else {
                aB.hide();
            }
        }
    }

    protected void showActionBar() {
        if (aB != null && !aB.isShowing()) {
            aB.show();
            if (tB != null) {
                tB.animate().translationY(0).setDuration(800L).start();
            }
        }
    }

    protected void hideFABs() {

        if (videoFAB.getVisibility() == View.VISIBLE) {
            videoFAB.hide();
            videoFAB.setVisibility(GONE);
            linearArrowVideo.setVisibility(GONE);
            relativeVideo.setVisibility(GONE);
        }
        if (microFAB.getVisibility() == View.VISIBLE) {
            microFAB.hide();
            microFAB.setVisibility(GONE);
        }
        if (speakerFAB.getVisibility() == View.VISIBLE) {
            speakerFAB.hide();
            speakerFAB.setVisibility(GONE);
        }

        if (rejectFAB.getVisibility() == View.VISIBLE) {
            rejectFAB.hide();
            rejectFAB.setVisibility(GONE);
        }
        if (hangFAB.getVisibility() == View.VISIBLE) {
            hangFAB.hide();
            hangFAB.setVisibility(GONE);
        }
        if (answerCallFAB.getVisibility() == View.VISIBLE) {
            answerCallFAB.hide();
            answerCallFAB.setVisibility(GONE);
            linearArrowCall.setVisibility(GONE);
            relativeCall.setVisibility(GONE);
        }
        if ((bigRecyclerViewLayout != null) && (bigRecyclerView != null) && (parentBigCameraGroupCall != null)) {
            RelativeLayout.LayoutParams bigRecyclerViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            bigRecyclerViewParams.addRule(RelativeLayout.BELOW, R.id.parent_layout_big_camera_group_call);
            bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, 0);
            bigRecyclerViewLayout.setLayoutParams(bigRecyclerViewParams);
            bigRecyclerViewLayout.requestLayout();
        }
    }

    @Override
    public void onPause() {
        log("onPause");
        MegaApplication.activityPaused();
        super.onPause();
        if(mSensorManager!=null){
            log("onPause:unregisterListener");
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        log("onResume");
        super.onResume();
        if ((peersOnCall != null) && (peersOnCall.size()>0)) {
            for (InfoPeerGroupCall peer : peersOnCall) {
                if (peer.getListener() != null) {
                    if (peer.getListener().getHeight() != 0) {
                        peer.getListener().setHeight(0);
                    }
                    if (peer.getListener().getWidth() != 0) {
                        peer.getListener().setWidth(0);
                    }
                }
            }

        }
        if(mSensorManager!=null){
            log("onResume:unregisterListener&registerListener");
            mSensorManager.unregisterListener(this);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        MegaApplication.activityResumed();
        if ((callChat == null) && (megaChatApi != null)) {
            callChat = megaChatApi.getChatCall(chatId);
        }
        if (callChat != null) {
            if ((callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)) {
                ((MegaApplication) getApplication()).sendSignalPresenceActivity();
            }
        }
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        if(mSensorManager!=null){
            log("onDestroy:unregisterListener");
            mSensorManager.unregisterListener(this);
        }
        clearHandlers();

        if ((peersOnCall != null) && (peersOnCall.size() > 0)) {
            for (InfoPeerGroupCall peer : peersOnCall) {
                if (peer.getListener() != null) {
                    if (peer.getListener().getHeight() != 0) {
                        peer.getListener().setHeight(0);
                    }
                    if (peer.getListener().getWidth() != 0) {
                        peer.getListener().setWidth(0);
                    }
                }
            }

        }

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
        }

        clearSurfacesViews();
        peerSelected = null;
        if (adapterList != null) {
            adapterList.updateMode(false);
        }
        isManualMode = false;

        if (adapterGrid != null) {
            adapterGrid.onDestroy();
        }
        if (adapterList != null) {
            adapterList.onDestroy();
        }
        if(peersOnCall!=null){
            peersOnCall.clear();

        }
        if(peersBeforeCall!=null){
            peersBeforeCall.clear();

        }
        recyclerView.setAdapter(null);
        bigRecyclerView.setAdapter(null);

        stopAudioSignals();
        if(rtcAudioManager!=null){
            rtcAudioManager.stop();
        }        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed");
        retryConnectionsAndSignalPresence();

        if(mSensorManager!=null){
            log("onBackPressed:unregisterListener");
            mSensorManager.unregisterListener(this);
        }

        clearHandlers();

        if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
            for (InfoPeerGroupCall peer : peersOnCall) {
                if (peer.getListener() != null) {
                    if (peer.getListener().getHeight() != 0) {
                        peer.getListener().setHeight(0);
                    }
                    if (peer.getListener().getWidth() != 0) {
                        peer.getListener().setWidth(0);
                    }
                }
            }

        }

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
        }

        clearSurfacesViews();

        peerSelected = null;
        if (adapterList != null) {
            adapterList.updateMode(false);
        }
        isManualMode = false;

        if (adapterGrid != null) {
            adapterGrid.onDestroy();
        }
        if (adapterList != null) {
            adapterList.onDestroy();
        }

        peersOnCall.clear();
        peersBeforeCall.clear();

        recyclerView.setAdapter(null);
        bigRecyclerView.setAdapter(null);

        stopAudioSignals();
        if(rtcAudioManager!=null){
            rtcAudioManager.stop();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        } else {
            super.finish();
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        log("onRequestStart: " + request.getType());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
        log("onRequestUpdate: " + request.getType());
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish: " + request.getType());

        if (request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            log("onRequestFinish: TYPE_HANG_CHAT_CALL");
            if(mSensorManager!=null){
                log("onRequestFinish:unregisterListener");
                mSensorManager.unregisterListener(this);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                super.finishAndRemoveTask();
            } else {
                super.finish();
            }
        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                showInitialFABConfiguration();

                if (request.getFlag() == true) {
                    log("Ok answer with video");
                } else {
                    log("Ok answer with NO video - ");
                }
                updateLocalVideoStatus();
                updateLocalAudioStatus();


            } else {
                log("Error call: " + e.getErrorString());

                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {

                    showErrorAlertDialogGroupCall(getString(R.string.call_error_too_many_participants), true, this);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        super.finishAndRemoveTask();
                    } else {
                        super.finish();
                    }
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) {

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
//                if(request.getParamType()==MegaChatRequest.AUDIO){
//                    if(request.getFlag()==true){
//                        log("Enable audio");
//                        microFAB.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
//                    }
//                    else{
//                        log("Disable audio");
//                        microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
//                    }
//                }
//                else if(request.getParamType()==MegaChatRequest.VIDEO){
//                    if(request.getFlag()==true){
//                        log("Enable video");
//                        myAvatarLayout.setVisibility(View.VISIBLE);
//                        myImageBorder.setVisibility(GONE);
//                        videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
//                    }
//                    else{
//                        log("Disable video");
//                        myAvatarLayout.setVisibility(GONE);
//                        myImageBorder.setVisibility(View.VISIBLE);
//                        videoFAB.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
//                    }
//                }
            } else {
                log("Error changing audio or video: " + e.getErrorString());
                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {
                    showSnackbar(getString(R.string.call_error_too_many_video));
                }

            }
        }

    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {

        if (call.getChatid() == chatId) {
            this.callChat = call;
            log("onChatCallUpdate:chatId: " + chatId);

            if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
                int callStatus = call.getStatus();
                log("CHANGE_TYPE_STATUS -> status: " + callStatus);

                switch (callStatus) {

                    case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                    case MegaChatCall.CALL_STATUS_JOINING:{
                        log("CHANGE_TYPE_STATUS:IN_PROGRESS||JOINING");

                        if (chat.isGroup()) {
                            checkParticipants(call);

                        }else{
                            setProfileAvatar(megaChatApi.getMyUserHandle());
                            setProfileAvatar(chat.getPeerHandle(0));
                            if (localCameraFragmentFS != null) {
                                localCameraFragmentFS.removeSurfaceView();
                                FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                                ftFS.remove(localCameraFragmentFS);
                                localCameraFragmentFS = null;
                                contactAvatarLayout.setVisibility(View.VISIBLE);
                                parentLocalFS.setVisibility(View.GONE);
                                fragmentContainerLocalCameraFS.setVisibility(View.GONE);
                            }
                            updateLocalVideoStatus();
                            updateLocalAudioStatus();
                            updateRemoteVideoStatus(-1, -1);
                            updateRemoteAudioStatus(-1, -1);

                        }

                        videoFAB.setOnClickListener(null);
                        answerCallFAB.setOnTouchListener(null);
                        videoFAB.setOnTouchListener(null);
                        videoFAB.setOnClickListener(this);

                        stopAudioSignals();
                        if(rtcAudioManager==null){
                            rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());
                        }
                        if(rtcAudioManager!=null){
                            rtcAudioManager.stop();
                            rtcAudioManager.start(null);
                        }
                        showInitialFABConfiguration();
                        updateSubTitle();
                        break;

                    }
                    case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION: {
                        log("CHANGE_TYPE_STATUS:CALL_STATUS_TERMINATING_USER_PARTICIPATION, chatId = "+chatId);

                        //I have finished the group call but I can join again
                        clearHandlers();
                        stopAudioSignals();
                        if(rtcAudioManager!=null){
                            rtcAudioManager.stop();
                        }
                        ((MegaApplication) getApplication()).setSpeakerStatus(false);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            super.finishAndRemoveTask();
                        } else {
                            super.finish();
                        }

                        break;
                    }
                    case MegaChatCall.CALL_STATUS_USER_NO_PRESENT: {
                        log("CHANGE_TYPE_STATUS:CALL_STATUS_USER_NO_PRESENT, chatId = "+chatId);

                        clearHandlers();
                        break;
                    }
                    case MegaChatCall.CALL_STATUS_DESTROYED: {
                        log("CHANGE_TYPE_STATUS:CALL_STATUS_DESTROYED, TERM code of the call: " + call.getTermCode());

                        //The group call has finished but I can not join again
                        clearHandlers();
                        stopAudioSignals();
                        if(rtcAudioManager!=null){
                            rtcAudioManager.stop();
                        }

                        ((MegaApplication) getApplication()).setSpeakerStatus(false);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            super.finishAndRemoveTask();
                        } else {
                            super.finish();
                        }
                        break;
                    }
                    default: {
                        log("CHANGE_TYPE_STATUS:other status = "+callStatus);
                        break;
                    }
                }

            } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_STATUS)) {
                log("onChatCallUpdate:CHANGE_TYPE_SESSION_STATUS");

                if (chat.isGroup()) {
                    if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
                        peersBeforeCall.clear();
                    }
                    long userPeerId = call.getPeerSessionStatusChange();
                    long userClientId = call.getClientidSessionStatusChange();

                    MegaChatSession userSession = call.getMegaChatSession(userPeerId, userClientId);

                    if (userSession != null) {

                        if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_INITIAL) {
                            log("CHANGE_TYPE_SESSION_STATUS:INITIAL");


                        }else if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                            log("CHANGE_TYPE_SESSION_STATUS:IN_PROGRESS");

                            //contact joined the group call
                            boolean peerContain = false;
                            if ((peersOnCall != null) && (peersOnCall.size() > 0)) {
                                for (InfoPeerGroupCall peerOnCall : peersOnCall) {
                                    if ((peerOnCall.getPeerId() == userPeerId) && (peerOnCall.getClientId() == userClientId)) {
                                        peerContain = true;
                                        break;
                                    }
                                }
                                if (!peerContain) {
                                    InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerId, userClientId, getName(userPeerId), false, false, false, true, null);
                                    log("CHANGE_TYPE_SESSION_STATUS:IN_PROGRESS "+userPeer.getPeerId()+" added in peersOnCall");

                                    peersOnCall.add((peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1)), userPeer);
                                    infoUsersBar.setText(getString(R.string.contact_joined_the_call, userPeer.getName()));
                                    infoUsersBar.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
                                    infoUsersBar.setAlpha(1);
                                    infoUsersBar.setVisibility(View.VISIBLE);
                                    infoUsersBar.animate().alpha(0).setDuration(4000);

                                    if (peersOnCall.size() < MAX_PEERS_GRID) {
                                        if (adapterGrid != null) {
                                            if (peersOnCall.size() < 4) {
                                                recyclerViewLayout.setPadding(0, 0, 0, 0);
                                                recyclerView.setColumnWidth((int) widthScreenPX);
                                                int posInserted = (peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                                adapterGrid.notifyItemInserted(posInserted);
                                                adapterGrid.notifyDataSetChanged();
                                            } else {
                                                if (peersOnCall.size() == 4) {
                                                    recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
                                                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                                    adapterGrid.notifyItemInserted(peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                                    adapterGrid.notifyDataSetChanged();
                                                } else {
                                                    recyclerViewLayout.setPadding(0, 0, 0, 0);
                                                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                                    int posInserted = (peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                                    adapterGrid.notifyItemInserted(posInserted);
                                                    adapterGrid.notifyItemRangeChanged((posInserted - 1), peersOnCall.size());

                                                }
                                            }
                                        } else {
                                            updatePeers();

                                        }
                                    } else {
                                        if (adapterList != null) {
                                            if (peersOnCall.size() == MAX_PEERS_GRID) {
                                                updatePeers();

                                            } else {
                                                int posInserted = (peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                                adapterList.notifyItemInserted(posInserted);
                                                adapterList.notifyItemRangeChanged((posInserted - 1), peersOnCall.size());
                                                updateUserSelected();
                                            }
                                        } else {
                                            updatePeers();

                                        }
                                    }

                                }
                                updateRemoteVideoStatus(userPeerId, userClientId);
                                updateRemoteAudioStatus(userPeerId, userClientId);
                                updateLocalVideoStatus();
                                updateLocalAudioStatus();
                            }

                        } else if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_DESTROYED) {
                            log("CHANGE_TYPE_SESSION_STATUS:DESTROYED");

                            //contact left the group call
                            if ((peersOnCall != null) && (peersOnCall.size() > 0)) {
                                for (int i = 0; i < peersOnCall.size(); i++) {
                                    if ((peersOnCall.get(i).getPeerId() == userPeerId) && (peersOnCall.get(i).getClientId() == userClientId)) {

                                        log("CHANGE_TYPE_SESSION_STATUS:DESTROYED "+peersOnCall.get(i).getPeerId()+" removed from peersOnCall");
                                        infoUsersBar.setText(getString(R.string.contact_left_the_call, peersOnCall.get(i).getName()));
                                        infoUsersBar.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
                                        infoUsersBar.setAlpha(1);
                                        infoUsersBar.setVisibility(View.VISIBLE);
                                        infoUsersBar.animate().alpha(0).setDuration(4000);
                                        peersOnCall.remove(i);

                                        if((peersOnCall.size()!=0)&& (peersOnCall.size() < MAX_PEERS_GRID)){
                                            if (adapterGrid != null) {
                                                if (peersOnCall.size() < 4) {
                                                    recyclerViewLayout.setPadding(0, 0, 0, 0);
                                                    recyclerView.setColumnWidth((int) widthScreenPX);
                                                    adapterGrid.notifyItemRemoved(i);
                                                    adapterGrid.notifyDataSetChanged();
                                                } else {
                                                    if (peersOnCall.size() == 6) {
                                                        recyclerViewLayout.setPadding(0, 0, 0, 0);
                                                        recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                                        adapterGrid.notifyItemRemoved(i);
                                                        adapterGrid.notifyDataSetChanged();
                                                    } else {
                                                        if (peersOnCall.size() == 4) {
                                                            recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
                                                            recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                                        } else {
                                                            recyclerViewLayout.setPadding(0, 0, 0, 0);
                                                            recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                                        }
                                                        adapterGrid.notifyItemRemoved(i);
                                                        adapterGrid.notifyItemRangeChanged(i, peersOnCall.size());
                                                    }
                                                }
                                            } else {
                                                updatePeers();

                                            }
                                        } else if(peersOnCall.size()!=0){
                                            if ((adapterList != null) && (peersOnCall.size() >= MAX_PEERS_GRID)) {
                                                adapterList.notifyItemRemoved(i);
                                                adapterList.notifyItemRangeChanged(i, peersOnCall.size());
                                                updateUserSelected();
                                            } else {
                                                updatePeers();

                                            }
                                        }
                                        break;
                                    }
                                }
                                updateLocalVideoStatus();
                                updateLocalAudioStatus();

                            }
                        } else {
                            log("CHANGE_TYPE_SESSION_STATUS:OTHER = "+userSession.getStatus());
                        }
                    }
                } else {

                    if ((call.getPeerSessionStatusChange() == call.getSessionsPeerid().get(0)) && (call.getClientidSessionStatusChange() == call.getSessionsClientid().get(0))) {
                        updateSubTitle();
                    }
                    updateRemoteVideoStatus(-1, -1);
                    updateRemoteAudioStatus(-1, -1);
                    updateLocalVideoStatus();
                    updateLocalAudioStatus();
                }


            } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_REMOTE_AVFLAGS)) {
                log("CHANGE_TYPE_SESSION_STATUS:REMOTE_AVFLAGS");

                if (chat.isGroup()) {
                    updateRemoteVideoStatus(call.getPeerSessionStatusChange(), call.getClientidSessionStatusChange());
                    updateRemoteAudioStatus(call.getPeerSessionStatusChange(), call.getClientidSessionStatusChange());

                } else {
                    if ((call.getPeerSessionStatusChange() == call.getSessionsPeerid().get(0)) && (call.getClientidSessionStatusChange() == call.getSessionsClientid().get(0))) {
                        updateRemoteVideoStatus(-1, -1);
                        updateRemoteAudioStatus(-1, -1);
                    }
                }
                updateSubtitleNumberOfVideos();

            } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
                log("CHANGE_TYPE_SESSION_STATUS:LOCAL_AVFLAGS");
                updateLocalVideoStatus();
                updateLocalAudioStatus();

                updateSubtitleNumberOfVideos();

            } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_RINGING_STATUS)) {
                log("CHANGE_TYPE_SESSION_STATUS:RINGING");

            } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)) {
                log("CHANGE_TYPE_SESSION_STATUS:COMPOSITION: status -> " + call.getStatus());

                if ((call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) || (call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
                    log("CHANGE_TYPE_SESSION_STATUS:COMPOSITION RING_IN || USER_NO_PRESENT -> TotalParticipants: " + call.getPeeridParticipants().size());

                    checkParticipants(call);

                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    log("CHANGE_TYPE_SESSION_STATUS:COMPOSITION:IN_PROGRESS ");

                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_JOINING) {
                    log("CHANGE_TYPE_SESSION_STATUS:COMPOSITION:JOINING");

                }else{
                    log("CHANGE_TYPE_SESSION_STATUS:COMPOSITION:OTHER = "+call.getStatus());

                }
            } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_AUDIO_LEVEL)) {
                log("CHANGE_TYPE_SESSION_STATUS:CHANGE_TYPE_SESSION_AUDIO_LEVEL");
                if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                    if (peersOnCall.size() >= MAX_PEERS_GRID) {
                        if (!isManualMode) {
                            long userPeerid = call.getPeerSessionStatusChange();
                            long userClientid = call.getClientidSessionStatusChange();
                            MegaChatSession userSession = call.getMegaChatSession(userPeerid, userClientid);
                            if (userSession != null) {
                                boolean userHasAudio = userSession.getAudioDetected();
                                if (userHasAudio) {
                                    //The user is talking
                                    int position = -1;
                                    for (int i = 0; i < peersOnCall.size(); i++) {
                                        if ((peersOnCall.get(i).getPeerId() == userPeerid) && (peersOnCall.get(i).getClientId() == userClientid)) {
                                            position = i;
                                        }
                                    }
                                    if (position != -1) {
                                        peerSelected = adapterList.getNodeAt(position);
                                        log("audio detected: "+peerSelected.getPeerId());
                                        updateUserSelected();
                                    }
                                }
                            }
                        }

                    }
                }
            } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_NETWORK_QUALITY)) {
                log("CHANGE_TYPE_SESSION_STATUS:NETWORK_QUALITY");

                if (chat.isGroup()) {
                    if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
                        peersBeforeCall.clear();
                    }
                    long userPeerid = call.getPeerSessionStatusChange();
                    long userClientid = call.getClientidSessionStatusChange();

                    MegaChatSession userSession = call.getMegaChatSession(userPeerid, userClientid);

                    if (userSession != null) {
                        if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                            log("CHANGE_TYPE_SESSION_STATUS:NETWORK_QUALITY:IN_PROGRESS");

                            int qualityLevel = userSession.getNetworkQuality();
                            if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                                if (qualityLevel < 2) {
                                    //bad quality
                                    for (int i = 0; i < peersOnCall.size(); i++) {
                                        if ((peersOnCall.get(i).getPeerId() == userPeerid) && (peersOnCall.get(i).getClientId() == userClientid)) {
                                            log("(peerId = "+peersOnCall.get(i).getPeerId()+", clientId = "+peersOnCall.get(i).getClientId()+") has bad quality: "+qualityLevel);
                                            if (peersOnCall.get(i).isGoodQuality()) {
                                                peersOnCall.get(i).setGoodQuality(false);
                                                if (peersOnCall.size() < MAX_PEERS_GRID) {
                                                    if (adapterGrid != null) {
                                                        adapterGrid.changesInQuality(i, null);
                                                    } else {
                                                        updatePeers();

                                                    }
                                                } else {
                                                    if (adapterList != null) {
                                                        adapterList.changesInQuality(i, null);

                                                    } else {
                                                        updatePeers();

                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    //good quality
                                    for (int i = 0; i < peersOnCall.size(); i++) {
                                        if ((peersOnCall.get(i).getPeerId() == userPeerid) && (peersOnCall.get(i).getClientId() == userClientid)) {
                                            if (!peersOnCall.get(i).isGoodQuality()) {
                                                log("(peerId = "+peersOnCall.get(i).getPeerId()+", clientId = "+peersOnCall.get(i).getClientId()+") has good quality: "+qualityLevel);
                                                peersOnCall.get(i).setGoodQuality(true);
                                                if (peersOnCall.size() < MAX_PEERS_GRID) {
                                                    if (adapterGrid != null) {
                                                        adapterGrid.changesInQuality(i, null);
                                                    } else {
                                                        updatePeers();

                                                    }
                                                } else {
                                                    if (adapterList != null) {
                                                        adapterList.changesInQuality(i, null);
                                                    } else {
                                                        updatePeers();

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            log("other userSession.getStatus(): "+userSession.getStatus());
                        }
                    }
                } else {
                }
            } else {
                log("other call.getChanges(): "+call.getChanges());
            }
        }
    }

    public void stopAudioSignals() {
        try {
            if (thePlayer != null) {
                thePlayer.stop();
                thePlayer.release();
            }
        } catch (Exception e) {
            log("Exception stopping player");
        }
        try {
            if (toneGenerator != null) {
                toneGenerator.stopTone();
                toneGenerator.release();
            }
        } catch (Exception e) {
            log("Exception stopping tone generator");

        }
        try {
            if (ringtone != null) {
                ringtone.stop();
            }
        } catch (Exception e) {
            log("Exception stopping ringtone");

        }
        try {
            if (timer != null) {
                timer.cancel();
            }
            if (ringerTimer != null) {
                ringerTimer.cancel();
            }
        } catch (Exception e) {
            log("Exception stopping ringing time");

        }
        try {
            if (vibrator != null) {
                if (vibrator.hasVibrator()) {
                    vibrator.cancel();
                }
            }
        } catch (Exception e) {
            log("Exception stopping vibrator");

        }
        thePlayer = null;
        toneGenerator = null;
        timer = null;
        ringerTimer = null;
    }


    @Override
    public void onClick(View v) {
        log("onClick");

        switch (v.getId()) {
            case R.id.call_chat_contact_image_rl:
            case R.id.parent_layout_big_camera_group_call: {
                remoteCameraClick();
                break;
            }
            case R.id.video_fab: {
                log("onClick video FAB");
                if (callChat == null) {
                    if (megaChatApi != null) {
                        callChat = megaChatApi.getChatCall(chatId);
                    }
                }
                if (callChat != null) {
                    if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                        linearFAB = (LinearLayout) findViewById(R.id.linear_buttons);
                        RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutCompress);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                        megaChatApi.answerChatCall(chatId, true, this);
                        clearHandlers();

                        answerCallFAB.clearAnimation();
                        videoFAB.clearAnimation();
                    } else {
                        if (callChat.hasLocalVideo()) {
                            log(" disableVideo");
                            megaChatApi.disableVideo(chatId, this);
                        } else {
                            log(" enableVideo");
                            megaChatApi.enableVideo(chatId, this);
                        }
                    }

                    if ((callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)) {
                        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                    }
                }
                break;
            }
            case R.id.micro_fab: {
                log("Click on micro fab");
                if (callChat == null) {
                    if (megaChatApi != null) {
                        callChat = megaChatApi.getChatCall(chatId);
                    }
                }
                if (callChat != null) {
                    if (callChat.hasLocalAudio()) {
                        megaChatApi.disableAudio(chatId, this);
                    } else {
                        megaChatApi.enableAudio(chatId, this);
                    }
                    if ((callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)) {
                        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                    }
                }
                break;
            }
            case R.id.speaker_fab: {
                log("Click on speaker fab");

                if(((MegaApplication) getApplication()).isSpeakerOn()){
                    ((MegaApplication) getApplication()).setSpeakerStatus(false);
                }else{
                    ((MegaApplication) getApplication()).setSpeakerStatus(true);
                }

                updateLocalSpeakerStatus();
                if ((callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)) {
                    ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                }
                break;
            }
            case R.id.reject_fab:
            case R.id.hang_fab:{
                log("Click on reject fab or hang fab");
                megaChatApi.hangChatCall(chatId, this);
                if (callChat == null) {
                    if (megaChatApi != null) {
                        callChat = megaChatApi.getChatCall(chatId);
                    }
                }
                if (callChat != null) {
                    if ((callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)) {
                        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                    }
                }
                break;
            }
            case R.id.answer_call_fab: {
                log("Click on answer fab");
                if (callChat == null) {
                    if (megaChatApi != null) {
                        callChat = megaChatApi.getChatCall(chatId);
                    }
                }
                if (callChat != null) {
                    if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                        linearFAB = (LinearLayout) findViewById(R.id.linear_buttons);
                        RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                        linearFAB.setLayoutParams(layoutCompress);
                        linearFAB.requestLayout();
                        linearFAB.setOrientation(LinearLayout.HORIZONTAL);
                        megaChatApi.answerChatCall(chatId, false, this);
                        clearHandlers();
                        answerCallFAB.clearAnimation();
                        videoFAB.clearAnimation();
                    }

                    if ((callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)) {
                        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                    }
                }
                break;
            }
        }
    }

//    public void checkPermissionsWriteLog(){
//        log("checkPermissionsWriteLog()");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            boolean hasWriteLogPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED);
//            if (!hasWriteLogPermission) {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALL_LOG}, Constants.WRITE_LOG);
//            }
//        }
//    }

    public boolean checkPermissions() {
        log("checkPermissions:Camera && Audio");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
            if (!hasCameraPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA);
                return false;
            }
            boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
            if (!hasRecordAudioPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, Constants.RECORD_AUDIO);
                return false;
            }
            return true;
        }
        return true;
    }

    public void showInitialFABConfiguration() {
        log("showInitialFABConfiguration");
        if (callChat == null) {
            if (megaChatApi != null) {
                callChat = megaChatApi.getChatCall(chatId);
            }
        }

        if (callChat != null) {
            if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                log("showInitialFABConfiguration:RING_IN");

                if (answerCallFAB.getVisibility() != View.VISIBLE) {
                    relativeCall.setVisibility(View.VISIBLE);
                    answerCallFAB.show();
                    answerCallFAB.setVisibility(View.VISIBLE);
                }
                linearArrowCall.setVisibility(GONE);

                if (videoFAB.getVisibility() != View.VISIBLE) {
                    relativeVideo.setVisibility(View.VISIBLE);
                    videoFAB.show();
                    videoFAB.setVisibility(View.VISIBLE);
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                    videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_videocam_white));
                }
                linearArrowVideo.setVisibility(GONE);


                if (rejectFAB.getVisibility() != View.VISIBLE) {
                    rejectFAB.show();
                    rejectFAB.setVisibility(View.VISIBLE);
                }

                if (speakerFAB.getVisibility() != View.INVISIBLE) {
                    speakerFAB.setVisibility(View.INVISIBLE);
                }
                if (microFAB.getVisibility() != View.INVISIBLE) {
                    microFAB.setVisibility(View.INVISIBLE);
                }
                if (hangFAB.getVisibility() != View.GONE) {
                    hangFAB.setVisibility(View.GONE);
                }

                if (callChat.hasVideoInitialCall()) {
                    RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                    layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                    linearFAB.setLayoutParams(layoutExtend);
                    linearFAB.requestLayout();
                    linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                    answerCallFAB.setOnClickListener(this);
                    videoFAB.setOnClickListener(null);

                    linearArrowVideo.setVisibility(View.VISIBLE);

                    videoFAB.startAnimation(shake);
                    animationAlphaArrows(fourArrowVideo);
                    handlerArrow1 = new Handler();
                    handlerArrow1.postDelayed(new Runnable() {
                        public void run() {
                            animationAlphaArrows(thirdArrowVideo);
                            handlerArrow2 = new Handler();
                            handlerArrow2.postDelayed(new Runnable() {
                                public void run() {
                                    animationAlphaArrows(secondArrowVideo);
                                    handlerArrow3 = new Handler();
                                    handlerArrow3.postDelayed(new Runnable() {
                                        public void run() {
                                            animationAlphaArrows(firstArrowVideo);
                                        }
                                    }, 250);
                                }
                            }, 250);
                        }
                    }, 250);


                    videoFAB.setOnTouchListener(new OnSwipeTouchListener(this) {
                        public void onSwipeTop() {
                            videoFAB.clearAnimation();

                            TranslateAnimation translateAnim = new TranslateAnimation(0, 0, 0, -380);
                            translateAnim.setDuration(translationAnimationDuration);
                            translateAnim.setFillAfter(true);
                            translateAnim.setFillBefore(true);
                            translateAnim.setRepeatCount(0);

                            AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
                            alphaAnim.setDuration(alphaAnimationDuration);
                            alphaAnim.setFillAfter(true);
                            alphaAnim.setFillBefore(true);
                            alphaAnim.setRepeatCount(0);

                            AnimationSet s = new AnimationSet(false);//false means don't share interpolators
                            s.addAnimation(translateAnim);
                            s.addAnimation(alphaAnim);

                            videoFAB.startAnimation(s);
                            firstArrowVideo.clearAnimation();
                            secondArrowVideo.clearAnimation();
                            thirdArrowVideo.clearAnimation();
                            fourArrowVideo.clearAnimation();
                            linearArrowVideo.setVisibility(GONE);

                            translateAnim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {

                                    linearFAB.setOrientation(LinearLayout.HORIZONTAL);
                                    RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                                    layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                    linearFAB.setLayoutParams(layoutCompress);
                                    linearFAB.requestLayout();
                                    answerVideoCall();
                                }
                            });
                        }

                        public void onSwipeRight() {
                        }

                        public void onSwipeLeft() {
                        }

                        public void onSwipeBottom() {
                        }

                    });

                } else {

                    RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                    layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                    layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                    linearFAB.setLayoutParams(layoutExtend);
                    linearFAB.requestLayout();
                    linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                    answerCallFAB.startAnimation(shake);
                    linearArrowCall.setVisibility(View.VISIBLE);

                    animationAlphaArrows(fourArrowCall);
                    handlerArrow4 = new Handler();
                    handlerArrow4.postDelayed(new Runnable() {
                        public void run() {
                            animationAlphaArrows(thirdArrowCall);
                            handlerArrow5 = new Handler();
                            handlerArrow5.postDelayed(new Runnable() {
                                public void run() {
                                    animationAlphaArrows(secondArrowCall);
                                    handlerArrow6 = new Handler();
                                    handlerArrow6.postDelayed(new Runnable() {
                                        public void run() {
                                            animationAlphaArrows(firstArrowCall);
                                        }
                                    }, 250);
                                }
                            }, 250);
                        }
                    }, 250);

                    answerCallFAB.setOnTouchListener(new OnSwipeTouchListener(this) {
                        public void onSwipeTop() {
                            answerCallFAB.clearAnimation();
                            TranslateAnimation translateAnim = new TranslateAnimation(0, 0, 0, -380);
                            translateAnim.setDuration(translationAnimationDuration);
                            translateAnim.setFillAfter(true);
                            translateAnim.setFillBefore(true);
                            translateAnim.setRepeatCount(0);

                            AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
                            alphaAnim.setDuration(alphaAnimationDuration);
                            alphaAnim.setFillAfter(true);
                            alphaAnim.setFillBefore(true);
                            alphaAnim.setRepeatCount(0);

                            AnimationSet s = new AnimationSet(false);//false means don't share interpolators
                            s.addAnimation(translateAnim);
                            s.addAnimation(alphaAnim);

                            answerCallFAB.startAnimation(s);
                            firstArrowCall.clearAnimation();
                            secondArrowCall.clearAnimation();
                            thirdArrowCall.clearAnimation();
                            fourArrowCall.clearAnimation();
                            linearArrowCall.setVisibility(GONE);

                            translateAnim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                                    layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                    linearFAB.setLayoutParams(layoutCompress);
                                    linearFAB.requestLayout();
                                    linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                                    answerAudioCall();
                                }
                            });
                        }

                        public void onSwipeRight() {
                        }

                        public void onSwipeLeft() {
                        }

                        public void onSwipeBottom() {
                        }

                    });

                }

            }else if((callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT)||(callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS)){

                log("showInitialFABConfiguration: other case");

                relativeVideo.setVisibility(View.VISIBLE);
                if(speakerFAB.getVisibility()!=View.VISIBLE){
                    speakerFAB.show();
                    speakerFAB.setVisibility(View.VISIBLE);
                }
                if(videoFAB.getVisibility()!=View.VISIBLE){
                    videoFAB.show();
                    videoFAB.setVisibility(View.VISIBLE);
                }
                if(microFAB.getVisibility()!=View.VISIBLE){
                    microFAB.show();
                    microFAB.setVisibility(View.VISIBLE);
                }
                if(hangFAB.getVisibility()!=View.VISIBLE){
                    hangFAB.show();
                    hangFAB.setVisibility(View.VISIBLE);
                }

                linearArrowVideo.setVisibility(GONE);
                if(answerCallFAB.getVisibility()!= View.INVISIBLE){
                    answerCallFAB.setVisibility(View.INVISIBLE);
                    relativeCall.setVisibility(View.INVISIBLE);
                }

                linearArrowCall.setVisibility(GONE);
                if(rejectFAB.getVisibility()!= View.INVISIBLE){
                    rejectFAB.setVisibility(View.INVISIBLE);
                }

            }

            if ((bigRecyclerViewLayout != null) && (bigRecyclerView != null) && (parentBigCameraGroupCall != null)) {
                RelativeLayout.LayoutParams bigRecyclerViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
                bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.parent_layout_big_camera_group_call);
                bigRecyclerViewParams.addRule(RelativeLayout.BELOW, 0);
                bigRecyclerViewLayout.setLayoutParams(bigRecyclerViewParams);
                bigRecyclerViewLayout.requestLayout();
            }
        }
    }

    public void updateLocalVideoStatus() {
        log("updateLocalVideoStatus");
        if ((callChat == null) && (megaChatApi != null)) {
            callChat = megaChatApi.getChatCall(chatId);
        }
        if (callChat != null) {
            int callStatus = callChat.getStatus();
            if (chat.isGroup()) {
                if (callChat != null) {
                    if (callChat.hasLocalVideo()) {
                        log("updateLocalVideoStatus:group:Video local connected");
                        videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                        videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));
                        if ((peersOnCall != null) && (peersOnCall.size() > 0)) {
                            for (int i = 0; i < peersOnCall.size(); i++) {
                                if ((peersOnCall.get(i).getPeerId() == megaChatApi.getMyUserHandle()) && (peersOnCall.get(i).getClientId() == megaChatApi.getMyClientidHandle(chatId))) {
                                    if (!peersOnCall.get(i).isVideoOn()) {
                                        log("updateLocalVideoStatus: activate Local Video");
                                        peersOnCall.get(i).setVideoOn(true);
                                        if (peersOnCall.size() < MAX_PEERS_GRID) {
                                            if (adapterGrid != null) {
                                                adapterGrid.notifyItemChanged(i);
                                            } else {
                                                updatePeers();

                                            }
                                        } else {
                                            if (adapterList != null) {
                                                adapterList.notifyItemChanged(i);
                                            } else {
                                                updatePeers();

                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        log("onUpdateLocalVideoStatus:group:Video local NOT connected");
                        videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                        videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
                        if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                            for (int i = 0; i < peersOnCall.size(); i++) {
                                 if ((peersOnCall.get(i).getPeerId() == megaChatApi.getMyUserHandle()) && (peersOnCall.get(i).getClientId() == megaChatApi.getMyClientidHandle(chatId))) {
                                     if (peersOnCall.get(i).isVideoOn()) {
                                         log("updateLocalVideoStatus: desactivate Local Video");
                                         peersOnCall.get(i).setVideoOn(false);
                                         if (peersOnCall.size() < MAX_PEERS_GRID) {
                                             if (adapterGrid != null) {
                                                 adapterGrid.notifyItemChanged(i);
                                             } else {
                                                 updatePeers();
                                             }
                                         } else {
                                             if (adapterList != null) {
                                                 adapterList.notifyItemChanged(i);
                                             } else {
                                                 updatePeers();
                                             }
                                         }
                                     }
                                     break;
                                 }
                            }
                        }
                    }
                }
            } else {
                log("updateLocalVideoStatus:individual");
                if (callChat != null) {
                    if (callChat.hasLocalVideo()) {
                        log("updateLocalVideoStatus:Video local connected");
                        videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                        videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));

                        if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                            log("callStatus: CALL_STATUS_REQUEST_SENT");

                            if (localCameraFragmentFS == null) {
                                localCameraFragmentFS = LocalCameraCallFullScreenFragment.newInstance(chatId);
                                FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                                ftFS.replace(R.id.fragment_container_local_cameraFS, localCameraFragmentFS, "localCameraFragmentFS");
                                ftFS.commitNowAllowingStateLoss();
                            }
                            contactAvatarLayout.setVisibility(GONE);
                            parentLocalFS.setVisibility(View.VISIBLE);
                            fragmentContainerLocalCameraFS.setVisibility(View.VISIBLE);

                        } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                            log("callStatus: CALL_STATUS_IN_PROGRESS");

                            if (localCameraFragment == null) {
                                localCameraFragment = LocalCameraCallFragment.newInstance(chatId);
                                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                                ft.replace(R.id.fragment_container_local_camera, localCameraFragment, "localCameraFragment");
                                ft.commitNowAllowingStateLoss();
                            }

                            myAvatarLayout.setVisibility(GONE);
                            parentLocal.setVisibility(View.VISIBLE);
                            fragmentContainerLocalCamera.setVisibility(View.VISIBLE);
                        }

                    } else {
                        log("updateLocalVideoStatus:Video local NOT connected");
                        videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                        videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
                        if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                            log("callStatus: CALL_STATUS_REQUEST_SENT");

                            if (localCameraFragmentFS != null) {
                                localCameraFragmentFS.removeSurfaceView();
                                FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                                ftFS.remove(localCameraFragmentFS);
                                localCameraFragmentFS = null;
                            }
                            parentLocalFS.setVisibility(View.GONE);
                            fragmentContainerLocalCameraFS.setVisibility(View.GONE);
                            contactAvatarLayout.setVisibility(View.VISIBLE);

                        } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                            log("callStatus: CALL_STATUS_IN_PROGRESS ");
                            if (localCameraFragment != null) {
                                localCameraFragment.removeSurfaceView();
                                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                                ft.remove(localCameraFragment);
                                localCameraFragment = null;
                            }
                            parentLocal.setVisibility(View.GONE);
                            fragmentContainerLocalCamera.setVisibility(View.GONE);
                            myAvatarLayout.setVisibility(View.VISIBLE);
                        }
                    }
                    refreshOwnMicro();
                }
            }
        }
    }

    public void updateLocalAudioStatus() {
        log("updateLocalAudioStatus");

        if ((callChat == null) && (megaChatApi != null)) {
            callChat = megaChatApi.getChatCall(chatId);
        }
        if (callChat != null) {
            if (chat.isGroup()) {
                if (callChat.hasLocalAudio()) {
                    log("updateLocalAudioStatus:group:Audio local connected");
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        int position = peersOnCall.size() - 1;
                        if (!peersOnCall.get(position).isAudioOn()) {
                            peersOnCall.get(position).setAudioOn(true);
                            if (peersOnCall.size() < MAX_PEERS_GRID) {
                                if (adapterGrid != null) {
                                    adapterGrid.changesInAudio(position, null);
                                } else {
                                    updatePeers();

                                }
                            } else {
                                if (adapterList != null) {
                                    adapterList.changesInAudio(position, null);
                                } else {
                                    updatePeers();

                                }
                            }
                        }
                    }
                } else {
                    log("updateLocalAudioStatus:group:Audio local NOT connected");
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        int position = peersOnCall.size() - 1;
                        if (peersOnCall.get(position).isAudioOn()) {
                            peersOnCall.get(position).setAudioOn(false);
                            if (peersOnCall.size() < MAX_PEERS_GRID) {
                                if (adapterGrid != null) {
                                    adapterGrid.changesInAudio(position, null);
                                } else {
                                    updatePeers();

                                }
                            } else {
                                if (adapterList != null) {
                                    adapterList.changesInAudio(position, null);
                                } else {
                                    updatePeers();

                                }
                            }
                        }
                    }
                }
            } else {
                if (callChat.hasLocalAudio()) {
                    log("updateLocalAudioStatus:individual:Audio local connected");
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
                } else {
                    log("updateLocalAudioStatus:individual:Audio local NOT connected");
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
                }
                refreshOwnMicro();
            }
        }

    }

    private void updateLocalSpeakerStatus(){
        log("updateLocalSpeakerStatus()");
        if(((MegaApplication) getApplication()).isSpeakerOn()){
            //enable speaker
            log("enable speaker");
            if(rtcAudioManager==null){
                rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());
            }
            if(rtcAudioManager!=null){
                rtcAudioManager.activateSpeaker(true);
                speakerFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                speakerFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_speaker_on));
            }
        }else{
            //disable speaker
            log("disable speaker");
            if(rtcAudioManager==null){
                rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());
            }
            if(rtcAudioManager!=null){
                rtcAudioManager.activateSpeaker(false);
                speakerFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                speakerFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_speaker_off));
            }
        }
    }

    public void updateRemoteVideoStatus(long userPeerId, long userClientId) {
        log("updateRemoteVideoStatus (peerid = "+userPeerId+", clientid = "+userClientId+")");
        if ((callChat == null) && (megaChatApi != null)) {
            callChat = megaChatApi.getChatCall(chatId);
        }
        if (callChat != null) {
            if (chat.isGroup()) {
                MegaChatSession userSession = callChat.getMegaChatSession(userPeerId, userClientId);
                if (userSession != null && userSession.hasVideo()) {
                    log("updateRemoteVideoStatus: Contact -> Video remote connected");

                    if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                        for (int i = 0; i < peersOnCall.size(); i++) {
                            if ((peersOnCall.get(i).getPeerId() == userPeerId) && (peersOnCall.get(i).getClientId() == userClientId)) {
                                if (!peersOnCall.get(i).isVideoOn()) {
                                    log("updateRemoteVideo: Contact Connected video");
                                    peersOnCall.get(i).setVideoOn(true);
                                    if (peersOnCall.size() < MAX_PEERS_GRID) {
                                        if (adapterGrid != null) {
                                            adapterGrid.notifyItemChanged(i);
                                        } else {
                                            updatePeers();

                                        }
                                    } else {
                                        if (adapterList != null) {
                                            adapterList.notifyItemChanged(i);
                                            if (peerSelected != null) {
                                                if ((peerSelected.getPeerId() == userPeerId) && (peerSelected.getClientId() == userClientId)) {
                                                    createBigFragment(peerSelected.getPeerId(), peerSelected.getClientId());
                                                    avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                                    if (peerSelected.isAudioOn()) {
                                                        //Disable audio icon GONE
                                                        microFragmentBigCameraGroupCall.setVisibility(GONE);
                                                    } else {
                                                        //Disable audio icon VISIBLE
                                                        microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            }
                                        } else {
                                            updatePeers();

                                            if (peerSelected != null) {
                                                if ((peerSelected.getPeerId() == userPeerId) && (peerSelected.getClientId() == userClientId)) {
                                                    createBigFragment(peerSelected.getPeerId(), peerSelected.getClientId());
                                                    avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                                    if (peerSelected.isAudioOn()) {
                                                        //Disable audio icon GONE
                                                        microFragmentBigCameraGroupCall.setVisibility(GONE);
                                                    } else {
                                                        //Disable audio icon VISIBLE
                                                        microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                } else {
                    log("updateRemoteVideoStatus: Contact -> Video remote NO connected");
                    if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                        for (int i = 0; i < peersOnCall.size(); i++) {
                            if ((peersOnCall.get(i).getPeerId() == userPeerId) && (peersOnCall.get(i).getClientId() == userClientId)) {
                                if (peersOnCall.get(i).isVideoOn()) {
                                    peersOnCall.get(i).setVideoOn(false);
                                    log("updateRemoteVideo: Contact Disconnected video");

                                    if (peersOnCall.size() < MAX_PEERS_GRID) {
                                        if (adapterGrid != null) {
                                            adapterGrid.notifyItemChanged(i);
                                        } else {
                                            updatePeers();

                                        }
                                    } else {
                                        if (adapterList != null) {
                                            adapterList.notifyItemChanged(i);
                                            if (peerSelected != null) {
                                                if ((peerSelected.getPeerId() == userPeerId) && (peerSelected.getClientId() == userClientId)) {
                                                    createBigAvatar();
                                                    microFragmentBigCameraGroupCall.setVisibility(GONE);
                                                    if (peerSelected.isAudioOn()) {
                                                        //Disable audio icon GONE
                                                        avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                                    } else {
                                                        //Disable audio icon VISIBLE
                                                        avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            }
                                        } else {
                                            updatePeers();

                                            if (peerSelected != null) {
                                                if ((peerSelected.getPeerId() == userPeerId) && (peerSelected.getClientId() == userClientId)) {
                                                    createBigAvatar();
                                                    microFragmentBigCameraGroupCall.setVisibility(GONE);
                                                    if (peerSelected.isAudioOn()) {
                                                        //Disable audio icon GONE
                                                        avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                                    } else {
                                                        //Disable audio icon VISIBLE
                                                        avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }

            } else {
                log("updateRemoteVideoStatus:individual");
                MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
                if (isRemoteVideo == REMOTE_VIDEO_NOT_INIT) {
                    if (userSession != null && userSession.hasVideo()) {
                        log("updateRemoteVideoStatus:REMOTE_VIDEO_NOT_INIT Contact Video remote connected");
                        isRemoteVideo = REMOTE_VIDEO_ENABLED;

                        if (remoteCameraFragmentFS == null) {
                            remoteCameraFragmentFS = RemoteCameraCallFullScreenFragment.newInstance(chatId, callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.replace(R.id.fragment_container_remote_cameraFS, remoteCameraFragmentFS, "remoteCameraFragmentFS");
                            ft.commitNowAllowingStateLoss();
                        }
                        contactAvatarLayout.setOnClickListener(null);
                        contactAvatarLayout.setVisibility(GONE);
                        parentRemoteFS.setVisibility(View.VISIBLE);
                        fragmentContainerRemoteCameraFS.setVisibility(View.VISIBLE);

                    } else {
                        log("updateRemoteVideoStatus:REMOTE_VIDEO_NOT_INIT Contact Video remote NOT connected");
                        isRemoteVideo = REMOTE_VIDEO_DISABLED;
                        if (remoteCameraFragmentFS != null) {
                            remoteCameraFragmentFS.removeSurfaceView();
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.remove(remoteCameraFragmentFS);
                            remoteCameraFragmentFS = null;
                        }
                        contactAvatarLayout.setVisibility(View.VISIBLE);
                        contactAvatarLayout.setOnClickListener(this);
                        parentRemoteFS.setVisibility(View.GONE);
                        fragmentContainerRemoteCameraFS.setVisibility(View.GONE);
                    }
                } else {
                    if ((isRemoteVideo == REMOTE_VIDEO_ENABLED) && (userSession != null) && (!userSession.hasVideo())) {
                        log("updateRemoteVideoStatus:REMOTE_VIDEO_ENABLED Contact Video remote connected");

                        isRemoteVideo = REMOTE_VIDEO_DISABLED;

                        if (remoteCameraFragmentFS != null) {
                            remoteCameraFragmentFS.removeSurfaceView();
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.remove(remoteCameraFragmentFS);
                            remoteCameraFragmentFS = null;
                        }
                        contactAvatarLayout.setVisibility(View.VISIBLE);
                        contactAvatarLayout.setOnClickListener(this);
                        parentRemoteFS.setVisibility(View.GONE);
                        fragmentContainerRemoteCameraFS.setVisibility(View.GONE);

                    } else if ((isRemoteVideo == REMOTE_VIDEO_DISABLED) && (userSession != null) && (userSession.hasVideo())) {
                        log("updateRemoteVideoStatus:REMOTE_VIDEO_DISABLED Contact Video remote connected");

                        isRemoteVideo = REMOTE_VIDEO_ENABLED;

                        if (remoteCameraFragmentFS == null) {
                            remoteCameraFragmentFS = RemoteCameraCallFullScreenFragment.newInstance(chatId, callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.replace(R.id.fragment_container_remote_cameraFS, remoteCameraFragmentFS, "remoteCameraFragmentFS");
                            ft.commitNowAllowingStateLoss();
                        }
                        contactAvatarLayout.setOnClickListener(null);
                        contactAvatarLayout.setVisibility(GONE);
                        parentRemoteFS.setVisibility(View.VISIBLE);
                        fragmentContainerRemoteCameraFS.setVisibility(View.VISIBLE);

                    }
                }
            }
        }
    }

    public void updateRemoteAudioStatus(long userPeerId, long userClientId) {
        log("updateRemoteAudioStatus (peerid = "+userPeerId+", clientid = "+userClientId+")");

        if ((callChat == null) && (megaChatApi != null)) {
            callChat = megaChatApi.getChatCall(chatId);
        }
        if (callChat != null) {
            if (chat.isGroup()) {
                MegaChatSession userSession = callChat.getMegaChatSession(userPeerId, userClientId);
                if (userSession != null && userSession.hasAudio()) {
                    log("updateRemoteAudioStatus:group Contact -> Audio remote connected");
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        for (int i = 0; i < peersOnCall.size(); i++) {

                            if ((peersOnCall.get(i).getPeerId() == userPeerId) && (peersOnCall.get(i).getClientId() == userClientId)) {
                                if (!peersOnCall.get(i).isAudioOn()) {
                                    log("updateRemoteAudioStatus: Contact Connected audio");
                                    peersOnCall.get(i).setAudioOn(true);
                                    if (peersOnCall.size() < MAX_PEERS_GRID) {
                                        if (adapterGrid != null) {
                                            adapterGrid.changesInAudio(i, null);
                                        } else {
                                            updatePeers();

                                        }
                                    } else {
                                        if (adapterList != null) {
                                            adapterList.changesInAudio(i, null);
                                        } else {
                                            updatePeers();
                                        }
                                        if (peerSelected != null) {
                                            if ((peerSelected.getPeerId() == userPeerId) && (peerSelected.getClientId() == userClientId)) {
                                                avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                                microFragmentBigCameraGroupCall.setVisibility(GONE);
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    log("updateRemoteAudioStatus: Contact -> Audio remote NO connected");

                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        for (int i = 0; i < peersOnCall.size(); i++) {
                            if ((peersOnCall.get(i).getPeerId() == userPeerId) && (peersOnCall.get(i).getClientId() == userClientId)) {
                                if (peersOnCall.get(i).isAudioOn()) {
                                    log("updateRemoteAudioStatus: Contact Disconnected audio");
                                    peersOnCall.get(i).setAudioOn(false);

                                    if (peersOnCall.size() < MAX_PEERS_GRID) {
                                        if (adapterGrid != null) {
                                            adapterGrid.changesInAudio(i, null);
                                        } else {
                                            updatePeers();

                                        }
                                    } else {
                                        if (adapterList != null) {
                                            adapterList.changesInAudio(i, null);
                                        } else {
                                            updatePeers();

                                        }

                                        if (peerSelected != null) {
                                            if ((peerSelected.getPeerId() == userPeerId) && (peerSelected.getClientId() == userClientId)) {
                                                if (peerSelected.isVideoOn()) {
                                                    avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                                    microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                                                } else {
                                                    avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                                                    microFragmentBigCameraGroupCall.setVisibility(GONE);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            } else {
                log("updateRemoteAudioStatus:individual");
                refreshLayoutContactMicro();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult");

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_CAMERA: {
                log("REQUEST_CAMERA");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissions()) {
//                        checkPermissionsWriteLog();
                        showInitialFABConfiguration();
                    }
                } else {
                    rejectFAB.setVisibility(View.VISIBLE);
                }
                break;
            }
            case Constants.RECORD_AUDIO: {
                log("RECORD_AUDIO");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissions()) {
//                        checkPermissionsWriteLog();
                        showInitialFABConfiguration();
                    }
                } else {
                    rejectFAB.setVisibility(View.VISIBLE);
                }
                break;
            }
            case Constants.WRITE_LOG: {
                log("WRITE_LOG");
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    checkPermissionsWriteLog();
//                }
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("ChatCallActivity", message);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.values[0] == 0) {
            //Turn off Screen
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } else {
            //Turn on Screen
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    public void remoteCameraClick() {
        log("remoteCameraClick");
        if ((megaChatApi != null)&&(callChat == null)) {
            callChat = megaChatApi.getChatCall(chatId);
        }
        if (callChat != null) {
            if (callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                if (aB.isShowing()) {
                    hideActionBar();
                    hideFABs();
                } else {
                    showActionBar();
                    showInitialFABConfiguration();
                }
            }
        }
    }

    public void itemClicked(InfoPeerGroupCall peer) {

        log("itemClicked:userSelected: -> (peerId = "+peer.getPeerId()+", clientId = "+peer.getClientId()+")");
        if ((peerSelected.getPeerId() == peer.getPeerId()) && (peerSelected.getClientId() == peer.getClientId())) {
            //I touched the same user that is now in big fragment:
            if (isManualMode) {
                isManualMode = false;
                log("manual mode - False");
            } else {
                isManualMode = true;
                log("manual mode - True");
            }
            if (adapterList != null) {
                adapterList.updateMode(isManualMode);
                if((peersOnCall!=null)&&(peersOnCall.size()>0)){
                    for (int i = 0; i < peersOnCall.size(); i++) {
                        if ((peersOnCall.get(i).getPeerId() == peer.getPeerId()) && (peersOnCall.get(i).getClientId() == peer.getClientId())) {
                            peersOnCall.get(i).setGreenLayer(true);
                            adapterList.changesInGreenLayer(i, null);
                        } else {
                            if (peersOnCall.get(i).hasGreenLayer()) {
                                peersOnCall.get(i).setGreenLayer(false);
                                adapterList.changesInGreenLayer(i, null);
                            }
                        }
                    }
                }
            }

        } else {
            if ((peer.getPeerId() == megaChatApi.getMyUserHandle()) && (peer.getClientId() == megaChatApi.getMyClientidHandle(chatId))) {
                //Me
                log("itemClicked:Click myself - do nothing");
            } else {
                //contact
                if (!isManualMode) {
                    isManualMode = true;
                    if (adapterList != null) {
                        adapterList.updateMode(true);
                    }
                    log("manual mode - True");
                }
                peerSelected = peer;
                updateUserSelected();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private class MyRingerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (ringtone != null && !ringtone.isPlaying()) {
                        ringtone.play();
                    }
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        log("onKeyDown");
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                if(audioManager==null){
                    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                }
                if(audioManager != null) {
                    try {
                        if ((callChat == null) && (megaChatApi != null)) {
                            callChat = megaChatApi.getChatCall(chatId);
                        }
                        if (callChat != null) {
                            if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                                log("onKeyDown:UP:RING_IN");
                                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);

                            } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                                log("nKeyDown:UP:REQUEST_SENT");
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);

                            } else {
                                log("onKeyDown:UP:OTHER");
                                audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                            }
                        }
                    } catch (SecurityException e) {
                        return super.onKeyDown(keyCode, event);
                    }
                }
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                if(audioManager==null){
                    audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                }
                if(audioManager != null) {
                    try {
                        if ((callChat == null) && (megaChatApi != null)) {
                            callChat = megaChatApi.getChatCall(chatId);
                        }
                        if (callChat != null) {
                            if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                                log("onKeyDown:DOWN:RING_IN");
                                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);

                            } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                                log("onKeyDown:DOWN:REQUEST_SENT");
                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                            } else {

                                log("onKeyDown:DOWN:OTHER");
                                audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                            }
                        }
                    } catch (SecurityException e) {
                        return super.onKeyDown(keyCode, event);
                    }
                }
                return true;
            }
            default: {
                return super.onKeyDown(keyCode, event);
            }
        }
    }

    public void answerAudioCall() {
        log("answerAudioCall");
        clearHandlers();
        if (megaChatApi != null) {
            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }
            megaChatApi.answerChatCall(chatId, false, this);
        }
    }

    public void answerVideoCall() {
        log("answerVideoCall");
        clearHandlers();
        if (megaChatApi != null) {
            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }
            megaChatApi.answerChatCall(chatId, true, this);
        }
    }

    public void animationAlphaArrows(final ImageView arrow) {
        AlphaAnimation alphaAnimArrows = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimArrows.setDuration(alphaAnimationDurationArrow);
        alphaAnimArrows.setFillAfter(true);
        alphaAnimArrows.setFillBefore(true);
        alphaAnimArrows.setRepeatCount(Animation.INFINITE);
        arrow.startAnimation(alphaAnimArrows);
    }

    public void updateSubTitle() {
        log("updateSubTitle");
        int sessionStatus = -1;
        if ((callChat == null) && (megaChatApi != null)) {
            callChat = megaChatApi.getChatCall(chatId);
        }
        if (callChat != null) {
            if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                log("updateSubTitle:REQUEST_SENT");
                if ((chat == null) && (megaChatApi != null)) {
                    chat = megaChatApi.getChatRoom(chatId);
                }
                subtitleToobar.setVisibility(View.VISIBLE);
                if(callInProgressChrono!=null){
                    callInProgressChrono.stop();
                    callInProgressChrono.setVisibility(View.GONE);
                }
                if (chat != null) {
                    if (chat.isGroup()) {
                        subtitleToobar.setText(getString(R.string.outgoing_call_starting));
                    }else{
                        if(callChat.hasVideoInitialCall())   {
                            subtitleToobar.setText(getString(R.string.outgoing_video_call_starting).toLowerCase());
                        }else{
                            subtitleToobar.setText(getString(R.string.outgoing_audio_call_starting).toLowerCase());
                        }
                    }
                }else{
                    subtitleToobar.setText(getString(R.string.outgoing_call_starting));
                }

            } else if (callChat.getStatus() <= MegaChatCall.CALL_STATUS_RING_IN) {
                log("updateSubTitle:RING_IN");
                subtitleToobar.setVisibility(View.VISIBLE);
                if(callInProgressChrono!=null){
                    callInProgressChrono.stop();
                    callInProgressChrono.setVisibility(View.GONE);
                }
                subtitleToobar.setText(getString(R.string.incoming_call_starting));

            } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                log("updateSubTitle:IN_PROGRESS");
                if ((chat == null) && (megaChatApi != null)) {
                    chat = megaChatApi.getChatRoom(chatId);
                }
                if (chat != null) {
                    if (chat.isGroup()) {
                        subtitleToobar.setVisibility(GONE);

                        if(callInProgressChrono!=null){
                            callInProgressChrono.setVisibility(View.VISIBLE);
                            callInProgressChrono.setBase(SystemClock.elapsedRealtime() - (callChat.getDuration()*1000));
                            callInProgressChrono.start();
                            callInProgressChrono.setFormat(" %s");
                        }
                    } else {
                        linearParticipants.setVisibility(GONE);
                        MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
                        if (userSession != null) {
                            sessionStatus = userSession.getStatus();
                            log("sessionStatus: " + sessionStatus);
                            if (sessionStatus == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                                subtitleToobar.setVisibility(View.GONE);

                                if(callInProgressChrono!=null){
                                    callInProgressChrono.setVisibility(View.VISIBLE);
                                    callInProgressChrono.setBase(SystemClock.elapsedRealtime() - (callChat.getDuration()*1000));
                                    callInProgressChrono.start();
                                    callInProgressChrono.setFormat("%s");
                                }
                            } else {
                                subtitleToobar.setVisibility(View.VISIBLE);
                                if(callInProgressChrono!=null){
                                    callInProgressChrono.stop();
                                    callInProgressChrono.setVisibility(View.GONE);
                                }
                                subtitleToobar.setText(getString(R.string.chat_connecting));
                            }
                        } else {
                            log("Error getting the session of the user");
                            subtitleToobar.setVisibility(GONE);
                            if(callInProgressChrono!=null){
                                callInProgressChrono.stop();
                                callInProgressChrono.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            } else {
                subtitleToobar.setVisibility(GONE);
                if(callInProgressChrono!=null){
                    callInProgressChrono.stop();
                    callInProgressChrono.setVisibility(View.GONE);
                }
            }
        }
    }

    private void updateSubtitleNumberOfVideos() {
        log("updateSubtitleNumberOfVideos");
        if((megaChatApi!=null)&&(callChat==null)){
            callChat = megaChatApi.getChatCall(chatId);
        }
        if(callChat!=null){

            int usersWithVideo = callChat.getNumParticipants(MegaChatCall.VIDEO);
            log("updateSubtitleNumberOfVideos: usersWithVideo = "+usersWithVideo);

            if(usersWithVideo > 0){
                if((totalVideosAllowed == 0)&&(megaChatApi != null)){
                    totalVideosAllowed = megaChatApi.getMaxVideoCallParticipants();
                }
                if(totalVideosAllowed != 0){
                    participantText.setText(usersWithVideo + "/" + totalVideosAllowed);
                    linearParticipants.setVisibility(View.VISIBLE);
                }else{
                    linearParticipants.setVisibility(View.GONE);
                }
            }else{
                linearParticipants.setVisibility(View.GONE);
            }
        }
    }

    public void updatePeers() {
        log("updatePeer ");

        if(callChat==null){
            callChat = megaChatApi.getChatCall(chatId);
        }

        if(callChat!=null){
            log("updatePeer STATUS = "+callChat.getStatus());

            if((callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) || ((callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION) && (callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT))){
                //I'M NOT IN THE CALL. peersBeforeCall
                //Call INCOMING
                log("updatePeers:incoming call: peersBeforeCall");

                if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                    peersOnCall.clear();
                }

                isNecessaryCreateAdapter = true;
                linearParticipants.setVisibility(View.GONE);

                if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
                    log("updatePeers:incoming call: peersBeforeCall not empty");
                    if (peersBeforeCall.size() < MAX_PEERS_GRID) {
                        log("updatePeers:incoming call: peersBeforeCall 1-6 ");
                        //1-6
                        if (adapterList != null) {
                            adapterList.onDestroy();
                            adapterList = null;
                        }
                        avatarBigCameraGroupCallLayout.setVisibility(View.GONE);

                        bigRecyclerView.setAdapter(null);
                        bigRecyclerView.setVisibility(GONE);
                        bigRecyclerViewLayout.setVisibility(GONE);

                        parentBigCameraGroupCall.setOnClickListener(null);
                        parentBigCameraGroupCall.setVisibility(View.GONE);

                        recyclerViewLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.VISIBLE);
                        if (peersBeforeCall.size() < 4) {
                            recyclerViewLayout.setPadding(0, 0, 0, 0);
                            recyclerView.setColumnWidth((int) widthScreenPX);
                        } else {
                            if (peersBeforeCall.size() == 4) {
                                recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
                            } else {
                                recyclerViewLayout.setPadding(0, 0, 0, 0);
                            }
                            recyclerView.setColumnWidth((int) widthScreenPX / 2);
                        }
                        if (adapterGrid == null) {
                            log("updatePeer:(1-6) incoming call - create adapter");
                            recyclerView.setAdapter(null);
                            adapterGrid = new GroupCallAdapter(this, recyclerView, peersBeforeCall, chatId, true);
                            recyclerView.setAdapter(adapterGrid);
                        } else {
                            log("updatePeer:(1-6) incoming call - notifyDataSetChanged");
                            adapterGrid.notifyDataSetChanged();
                        }
                    } else {
                        log("updatePeers:incoming call: peersBeforeCall 7+ ");

                        //7 +
                        if (adapterGrid != null) {
                            adapterGrid.onDestroy();
                            adapterGrid = null;
                        }
                        recyclerView.setAdapter(null);
                        recyclerView.setVisibility(GONE);
                        recyclerViewLayout.setVisibility(View.GONE);

                        parentBigCameraGroupCall.setOnClickListener(this);
                        parentBigCameraGroupCall.setVisibility(View.VISIBLE);

                        bigRecyclerViewLayout.setVisibility(View.VISIBLE);
                        bigRecyclerView.setVisibility(View.VISIBLE);
                        if (adapterList == null) {
                            log("updatePeer:(7 +) incoming call - create adapter");
                            bigRecyclerView.setAdapter(null);
                            adapterList = new GroupCallAdapter(this, bigRecyclerView, peersBeforeCall, chatId, false);
                            bigRecyclerView.setAdapter(adapterList);
                        } else {
                            log("updatePeer:(7 +) incoming call - notifyDataSetChanged");
                            adapterList.notifyDataSetChanged();
                        }
                        updateUserSelected();
                    }

                } else {
                    log("updatePeers:incoming call: peersBeforeCall empty");
                    if (adapterGrid != null) {
                        adapterGrid.onDestroy();
                        adapterGrid = null;
                    }
                    if (adapterList != null) {
                        adapterList.onDestroy();
                        adapterList = null;
                    }
                    recyclerView.setAdapter(null);
                    recyclerView.setVisibility(GONE);
                    recyclerViewLayout.setVisibility(View.GONE);

                    bigRecyclerView.setAdapter(null);
                    bigRecyclerView.setVisibility(GONE);
                    bigRecyclerViewLayout.setVisibility(GONE);

                    parentBigCameraGroupCall.setOnClickListener(null);
                    parentBigCameraGroupCall.setVisibility(View.GONE);
                }

            }else{
                //I'M IN THE CALL. peersOnCall
                log("updatePeers:in progress call: peersOnCall");

                if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
                    peersBeforeCall.clear();
                }

                if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                    log("updatePeers:in progress call: peersOnCall not empty");
                    if (peersOnCall.size() < MAX_PEERS_GRID) {
                        log("updatePeers:in progress call: peersOnCall 1-6");
                        //1-6
                        if (adapterList != null) {
                            adapterList.onDestroy();
                            adapterList = null;
                        }

                        if (bigCameraGroupCallFragment != null) {
                            bigCameraGroupCallFragment.removeSurfaceView();
                            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                            ftFS.remove(bigCameraGroupCallFragment);
                            bigCameraGroupCallFragment = null;
                        }
                        avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
                        bigRecyclerView.setAdapter(null);
                        bigRecyclerView.setVisibility(GONE);
                        bigRecyclerViewLayout.setVisibility(GONE);

                        parentBigCameraGroupCall.setOnClickListener(null);
                        parentBigCameraGroupCall.setVisibility(View.GONE);
                        recyclerViewLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.VISIBLE);

                        if (peersOnCall.size() < 4) {
                            recyclerViewLayout.setPadding(0, 0, 0, 0);
                            recyclerView.setColumnWidth((int) widthScreenPX);
                        } else {
                            if (peersOnCall.size() == 4) {
                                recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
                            } else {
                                recyclerViewLayout.setPadding(0, 0, 0, 0);
                            }
                            recyclerView.setColumnWidth((int) widthScreenPX / 2);
                        }
                        if (adapterGrid == null) {
                            log("updatePeer:(1-6) in progress call - create adapter(1º)");
                            recyclerView.setAdapter(null);
                            adapterGrid = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, true);
                            recyclerView.setAdapter(adapterGrid);

                        } else {

                            if (isNecessaryCreateAdapter) {
                                log("updatePeer:(1-6) in progress call - create adapter(2º)");
                                adapterGrid.onDestroy();
                                recyclerView.setAdapter(null);
                                adapterGrid = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, true);
                                recyclerView.setAdapter(adapterGrid);
                            } else {

                                log("updatePeer:(1-6) in progress call - notifyDataSetChanged");
                                adapterGrid.notifyDataSetChanged();
                            }
                        }

                    } else {
                        log("updatePeers:in progress call: peersOnCall 7+");
                        //7 +
                        if (adapterGrid != null) {
                            adapterGrid.onDestroy();
                            adapterGrid = null;
                        }
                        recyclerView.setAdapter(null);
                        recyclerView.setVisibility(GONE);
                        recyclerViewLayout.setVisibility(View.GONE);
                        parentBigCameraGroupCall.setOnClickListener(this);
                        parentBigCameraGroupCall.setVisibility(View.VISIBLE);

                        bigRecyclerViewLayout.setVisibility(View.VISIBLE);
                        bigRecyclerView.setVisibility(View.VISIBLE);

                        if (adapterList == null) {
                            log("updatePeer:(7 +) in progress call - create adapter");
                            bigRecyclerView.setAdapter(null);
                            adapterList = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, false);
                            bigRecyclerView.setAdapter(adapterList);
                        } else {
                            if (isNecessaryCreateAdapter) {
                                log("updatePeer:(7 +) in progress call - create adapter");
                                adapterList.onDestroy();
                                bigRecyclerView.setAdapter(null);
                                adapterList = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, false);
                                bigRecyclerView.setAdapter(adapterList);
                            } else {
                                log("updatePeer:(7 +) in progress call - notifyDataSetChanged");
                                adapterList.notifyDataSetChanged();
                            }
                        }
                        updateUserSelected();
                    }
                    isNecessaryCreateAdapter = false;

                } else {
                    log("updatePeers:in progress call: peersOnCall empty");
                    if (adapterGrid != null) {
                        adapterGrid.onDestroy();
                        adapterGrid = null;
                    }
                    if (adapterList != null) {
                        adapterList.onDestroy();
                        adapterList = null;
                    }
                    recyclerView.setAdapter(null);
                    recyclerView.setVisibility(View.GONE);
                    recyclerViewLayout.setVisibility(View.GONE);
                    bigRecyclerView.setAdapter(null);
                    bigRecyclerView.setVisibility(GONE);
                    bigRecyclerViewLayout.setVisibility(GONE);
                    parentBigCameraGroupCall.setOnClickListener(null);
                    parentBigCameraGroupCall.setVisibility(View.GONE);
                }
            }
        }
    }

    public void updateUserSelected() {
        log("updateUserSelected");
        if((callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) || ((callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION)&&(callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT))) {
            //I'M NOT IN THE CALL. peersBeforeCall
            //Call INCOMING
            log("updateUserSelected: INCOMING");
            //Call INCOMING
            parentBigCameraGroupCall.setVisibility(View.VISIBLE);
            if (peerSelected == null) {
                //First time:
                //Remove Camera element, because with incoming, avatar is the only showed
                if (bigCameraGroupCallFragment != null) {
                    bigCameraGroupCallFragment.removeSurfaceView();
                    FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                    ftFS.remove(bigCameraGroupCallFragment);
                    bigCameraGroupCallFragment = null;
                }
                fragmentBigCameraGroupCall.setVisibility(View.GONE);

                //Create Avatar, get the last peer of peersBeforeCall
                if ((peersBeforeCall != null) && (peersBeforeCall.size() > 0)) {
                    avatarBigCameraGroupCallLayout.setVisibility(View.VISIBLE);
                    InfoPeerGroupCall peerTemp = peersBeforeCall.get((peersBeforeCall.size()) - 1);
                    setProfilePeerSelected(peerTemp.getPeerId(), peerTemp.getName(), null);
                } else {
                    avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
                }
            }
        }else{
            //I'M IN THE CALL. peersOnCall
            //Call IN PROGRESS
            log("updateUserSelected: IN PROGRESS");

            if (peerSelected == null) {
                log("updateUserSelected:peerSelected == null");

                //First case:
                if (!isManualMode) {
                    if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                        int position = 0;

                        peerSelected = peersOnCall.get(position);
                        log("updateUserSelected:InProgress - new peerSelected (peerId = "+peerSelected.getPeerId()+", clientId = "+peerSelected.getClientId()+")");

                        for (int i = 0; i < peersOnCall.size(); i++) {
                            if (i == position) {
                                if (!peersOnCall.get(position).hasGreenLayer()) {
                                    peersOnCall.get(position).setGreenLayer(true);
                                    if (adapterList != null) {
                                        adapterList.changesInGreenLayer(position, null);
                                    }
                                }
                            } else {
                                if (peersOnCall.get(i).hasGreenLayer()) {
                                    peersOnCall.get(i).setGreenLayer(false);
                                    if (adapterList != null) {
                                        adapterList.changesInGreenLayer(i, null);
                                    }
                                }
                            }
                        }
                        if (peerSelected.isVideoOn()) {
                            //Video ON
                            createBigFragment(peerSelected.getPeerId(), peerSelected.getClientId());
                            avatarBigCameraGroupCallMicro.setVisibility(GONE);
                            if (peerSelected.isAudioOn()) {
                                //Disable audio icon GONE
                                microFragmentBigCameraGroupCall.setVisibility(GONE);
                            } else {
                                //Disable audio icon VISIBLE
                                microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                            }
                        } else {
                            //Video OFF
                            createBigAvatar();
                            microFragmentBigCameraGroupCall.setVisibility(GONE);
                            if (peerSelected.isAudioOn()) {
                                //Disable audio icon GONE
                                avatarBigCameraGroupCallMicro.setVisibility(GONE);
                            } else {
                                //Disable audio icon VISIBLE
                                avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }else{
                log("updateUserSelected:peerSelected != null");

                //find if peerSelected is removed:
                if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                    boolean peerContained = false;
                    for (int i = 0; i < peersOnCall.size(); i++) {
                        if ((peersOnCall.get(i).getPeerId() == peerSelected.getPeerId()) && (peersOnCall.get(i).getClientId() == peerSelected.getClientId())) {
                            peerContained = true;
                            break;
                        }
                    }
                    if (!peerContained) {
                        //it was removed
                        if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
                            int position = 0;
                            peerSelected = peersOnCall.get(position);
                            log("updateUserSelected:InProgress - new peerSelected (peerId = "+peerSelected.getPeerId()+", clientId = "+peerSelected.getClientId()+")");

                            for (int i = 0; i < peersOnCall.size(); i++) {
                                if (i == position) {
                                    isManualMode = false;
                                    if (adapterList != null) {
                                        adapterList.updateMode(false);
                                    }
                                    if (!peersOnCall.get(position).hasGreenLayer()) {
                                        peersOnCall.get(position).setGreenLayer(true);
                                        if (adapterList != null) {
                                            adapterList.changesInGreenLayer(position, null);
                                        }
                                    }
                                } else {
                                    if (peersOnCall.get(i).hasGreenLayer()) {
                                        peersOnCall.get(i).setGreenLayer(false);
                                        if (adapterList != null) {
                                            adapterList.changesInGreenLayer(i, null);
                                        }
                                    }
                                }
                            }

                            if (peerSelected.isVideoOn()) {
                                //Video ON
                                createBigFragment(peerSelected.getPeerId(), peerSelected.getClientId());
                                avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                if (peerSelected.isAudioOn()) {
                                    //Disable audio icon GONE
                                    microFragmentBigCameraGroupCall.setVisibility(GONE);
                                } else {
                                    //Disable audio icon VISIBLE
                                    microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                                }
                            } else {
                                //Video OFF
                                createBigAvatar();
                                microFragmentBigCameraGroupCall.setVisibility(GONE);
                                if (peerSelected.isAudioOn()) {
                                    //Disable audio icon GONE
                                    avatarBigCameraGroupCallMicro.setVisibility(GONE);
                                } else {
                                    //Disable audio icon VISIBLE
                                    avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                                }
                            }
                        }

                    } else {
                        log("updateUserSelected:InProgress - peerSelected (peerId = "+peerSelected.getPeerId()+", clientId = "+peerSelected.getClientId()+")");
                        if((peersOnCall!=null)&&(peersOnCall.size()>0)){
                            for (int i = 0; i < peersOnCall.size(); i++) {
                                if ((peersOnCall.get(i).getPeerId() == peerSelected.getPeerId()) && (peersOnCall.get(i).getClientId() == peerSelected.getClientId())) {
                                    peersOnCall.get(i).setGreenLayer(true);
                                    if (adapterList != null) {
                                        adapterList.changesInGreenLayer(i, null);
                                    }
                                } else {
                                    if (peersOnCall.get(i).hasGreenLayer()) {
                                        peersOnCall.get(i).setGreenLayer(false);
                                        if (adapterList != null) {
                                            adapterList.changesInGreenLayer(i, null);
                                        }
                                    }
                                }
                            }
                        }


                        if (peerSelected.isVideoOn()) {
                            //Video ON
                            createBigFragment(peerSelected.getPeerId(), peerSelected.getClientId());
                            avatarBigCameraGroupCallMicro.setVisibility(GONE);
                            if (peerSelected.isAudioOn()) {
                                //Audio on, icon GONE
                                microFragmentBigCameraGroupCall.setVisibility(GONE);
                            } else {
                                //Audio off, icon VISIBLE
                                microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                            }
                        } else {
                            //Video OFF
                            createBigAvatar();
                            microFragmentBigCameraGroupCall.setVisibility(GONE);
                            if (peerSelected.isAudioOn()) {
                                //Audio on, icon GONE
                                avatarBigCameraGroupCallMicro.setVisibility(GONE);
                            } else {
                                //Audio off, icon VISIBLE
                                avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }
        }

    }

    public void createBigFragment(long peerId, long clientId) {
        log("createBigFragment()");
        //Remove big Camera
        if (bigCameraGroupCallFragment != null) {
            bigCameraGroupCallFragment.removeSurfaceView();
            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
            ftFS.remove(bigCameraGroupCallFragment);
            bigCameraGroupCallFragment = null;
        }

        //Create big Camera
        if (bigCameraGroupCallFragment == null) {
            bigCameraGroupCallFragment = BigCameraGroupCallFragment.newInstance(chatId, peerId, clientId);
            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
            ftFS.replace(R.id.fragment_big_camera_group_call, bigCameraGroupCallFragment, "bigCameraGroupCallFragment");
            ftFS.commitNowAllowingStateLoss();
        }

        fragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
        parentBigCameraGroupCall.setVisibility(View.VISIBLE);
        //Remove Avatar
        avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
    }

    public void createBigAvatar() {
        log("createBigAvatar()");
        //Remove big Camera
        if (bigCameraGroupCallFragment != null) {
            bigCameraGroupCallFragment.removeSurfaceView();
            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
            ftFS.remove(bigCameraGroupCallFragment);
            bigCameraGroupCallFragment = null;
        }

        fragmentBigCameraGroupCall.setVisibility(View.GONE);

        //Create Avatar
        avatarBigCameraGroupCallImage.setImageBitmap(null);
        setProfilePeerSelected(peerSelected.getPeerId(), peerSelected.getName(), null);
        parentBigCameraGroupCall.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallLayout.setVisibility(View.VISIBLE);
    }

    private void clearSurfacesViews() {
        log("clearSurfacesViews");
        if (localCameraFragment != null) {
            localCameraFragment.removeSurfaceView();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(localCameraFragment);
            localCameraFragment = null;
        }

        if (localCameraFragmentFS != null) {
            localCameraFragmentFS.removeSurfaceView();
            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
            ftFS.remove(localCameraFragmentFS);
            localCameraFragmentFS = null;
        }
        if (remoteCameraFragmentFS != null) {
            remoteCameraFragmentFS.removeSurfaceView();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(remoteCameraFragmentFS);
            remoteCameraFragmentFS = null;
        }

        if (bigCameraGroupCallFragment != null) {
            bigCameraGroupCallFragment.removeSurfaceView();
            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
            ftFS.remove(bigCameraGroupCallFragment);
            bigCameraGroupCallFragment = null;
        }
    }

    private void clearHandlers() {
        log("clearHandlers");
        if (handlerArrow1 != null) {
            handlerArrow1.removeCallbacksAndMessages(null);
        }
        if (handlerArrow2 != null) {
            handlerArrow2.removeCallbacksAndMessages(null);
        }
        if (handlerArrow3 != null) {
            handlerArrow3.removeCallbacksAndMessages(null);
        }
        if (handlerArrow4 != null) {
            handlerArrow4.removeCallbacksAndMessages(null);
        }
        if (handlerArrow5 != null) {
            handlerArrow5.removeCallbacksAndMessages(null);
        }
        if (handlerArrow6 != null) {
            handlerArrow6.removeCallbacksAndMessages(null);
        }

        if(callInProgressChrono!=null){
            callInProgressChrono.stop();
            callInProgressChrono.setVisibility(View.GONE);
        }
    }

    public void showSnackbar(String s){
        log("showSnackbar: "+s);
        showSnackbar(fragmentContainer, s);
    }

    private String getName(long peerid) {
        String name = " ";
        if((megaChatApi != null)&&(chat!=null)) {
            if (peerid == megaChatApi.getMyUserHandle()) {
                name = megaChatApi.getMyFullname();
                if (name == null) {
                    name = megaChatApi.getMyEmail();
                }
            } else {
                name = chat.getPeerFullnameByHandle(peerid);
                if (name == null) {
                    name = megaChatApi.getContactEmail(peerid);
                    if (name == null) {
                        CallNonContactNameListener listener = new CallNonContactNameListener(this, peerid, false, name);
                            megaChatApi.getUserEmail(peerid, listener);
                    }
                }
            }
        }
        return name;
    }

    public void updateNonContactName(long peerid, String peerEmail) {
        log("updateNonContactName: Email found it");
        if ((peersBeforeCall != null) && (peersBeforeCall.size() != 0)) {
            for (InfoPeerGroupCall peer : peersBeforeCall) {
                if (peerid == peer.getPeerId()) {
                    peer.setName(peerEmail);
                }
            }
        }
        if ((peersOnCall != null) && (peersOnCall.size() != 0)) {
            for (InfoPeerGroupCall peer : peersOnCall) {
                if (peerid == peer.getPeerId()) {
                    peer.setName(peerEmail);
                }
            }
        }
    }

    private void checkParticipants(MegaChatCall call) {
        log("checkParticipants");
        if((call==null)&&(megaChatApi!=null)){
            call = megaChatApi.getChatCall(chatId);
        }
        if(call!=null){
            if((call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) || ((call.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION)&&(call.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT))) {
                log("checkParticipants: INCOMING");
                //peersBeforeCall
                if ((megaChatApi != null) && (call.getPeeridParticipants().size() > 0)) {
                    boolean isMe = false;
                    for (int i = 0; i < call.getPeeridParticipants().size(); i++) {
                        long userPeerid = call.getPeeridParticipants().get(i);
                        long userClientid = call.getClientidParticipants().get(i);
                        if ((userPeerid == megaChatApi.getMyUserHandle()) && (userClientid == megaChatApi.getMyClientidHandle(chatId))) {
                            isMe = true;
                            break;
                        }
                    }
                    if (!isMe) {
                        if ((peersBeforeCall != null) && (peersBeforeCall.size() > 0)) {
                            boolean changes = false;
                            for (int i = 0; i < call.getPeeridParticipants().size(); i++) {
                                boolean peerContain = false;
                                long userPeerid = call.getPeeridParticipants().get(i);
                                long userClientid = call.getClientidParticipants().get(i);

                                for (InfoPeerGroupCall peerBeforeCall : peersBeforeCall) {
                                    if ((peerBeforeCall.getPeerId() == userPeerid) && (peerBeforeCall.getClientId() == userClientid)) {
                                        peerContain = true;
                                        break;
                                    }
                                }
                                if (!peerContain) {
                                    InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);
                                    log("checkParticipants "+userPeer.getPeerId()+" added in peersBeforeCall");
                                    if(peersBeforeCall!=null){
                                        peersBeforeCall.add((peersBeforeCall.size() == 0 ? 0 : (peersBeforeCall.size() - 1)), userPeer);
                                        changes = true;
                                    }
                                }
                            }

                            if((peersBeforeCall!=null)&&(peersBeforeCall.size() > 0)){
                                for (int i = 0; i < peersBeforeCall.size(); i++) {
                                    boolean peerContained = false;
                                    for (int j = 0; j < call.getPeeridParticipants().size(); j++) {
                                        long userPeerid = call.getPeeridParticipants().get(j);
                                        long userClientid = call.getClientidParticipants().get(j);
                                        if ((peersBeforeCall.get(i).getPeerId() == userPeerid) && (peersBeforeCall.get(i).getClientId() == userClientid)) {
                                            peerContained = true;
                                        }
                                    }
                                    if (!peerContained) {
                                        log("checkParticipants "+peersBeforeCall.get(i).getPeerId()+" removed from peersBeforeCall");
                                        peersBeforeCall.remove(i);
                                        changes = true;
                                    }
                                }
                            }

                            if (changes) {
                                updatePeers();

                            }
                        } else {
                            log("peersBeforeCall is empty -> add all");
                            for (int i = 0; i < call.getPeeridParticipants().size(); i++) {
                                long userPeerid = call.getPeeridParticipants().get(i);
                                long userClientid = call.getClientidParticipants().get(i);
                                InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);
                                log("checkParticipants "+userPeer.getPeerId()+" added in peersBeforeCall");
                                if(peersBeforeCall!=null){
                                    peersBeforeCall.add((peersBeforeCall.size() == 0 ? 0 : (peersBeforeCall.size() - 1)), userPeer);
                                }
                            }
                            updatePeers();
                        }
                    }
                }

            }else{
                log("checkParticipants: IN PROGRESS");
                //peersOnCall

                if((megaChatApi != null) && (peersOnCall != null)){
                    if ((peersOnCall.size() != 0)&& (call.getPeeridParticipants().size() > 0)) {
                        boolean changes = false;
                        //Get all participant and check it (some will be added and others will be removed)
                        for (int i = 0; i < call.getPeeridParticipants().size(); i++) {
                            boolean peerContain = false;
                            long userPeerid = call.getPeeridParticipants().get(i);
                            long userClientid = call.getClientidParticipants().get(i);

                            for (InfoPeerGroupCall peerOnCall : peersOnCall) {
                                if ((peerOnCall.getPeerId() == userPeerid) && (peerOnCall.getClientId() == userClientid)) {
                                    peerContain = true;
                                    break;
                                }
                            }
                            if (!peerContain) {
                                if ((userPeerid == megaChatApi.getMyUserHandle()) && (userClientid == megaChatApi.getMyClientidHandle(chatId))) {
                                    //me
                                    InfoPeerGroupCall myPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), call.hasLocalVideo(), call.hasLocalAudio(), false, true, null);
                                    log("checkParticipants I added in peersOnCall");
                                    peersOnCall.add(myPeer);
                                    changes = true;

                                } else {
                                    //contact
                                    MegaChatSession sessionPeer = call.getMegaChatSession(userPeerid,userClientid);
                                    if((sessionPeer!=null)&&(sessionPeer.getStatus() <= MegaChatSession.SESSION_STATUS_IN_PROGRESS)) {
                                        InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);
                                        peersOnCall.add((peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1)), userPeer);
                                        changes = true;
                                        log("checkParticipants "+userPeer.getPeerId()+" added in peersOnCall");
                                    }
                                }
                            }
                        }

                        for (int i = 0; i < peersOnCall.size(); i++) {
                            boolean peerContained = false;
                            for (int j = 0; j < call.getPeeridParticipants().size(); j++) {
                                long userPeerid = call.getPeeridParticipants().get(j);
                                long userClientid = call.getClientidParticipants().get(j);
                                if ((peersOnCall.get(i).getPeerId() == userPeerid) && (peersOnCall.get(i).getClientId() == userClientid)) {
                                    peerContained = true;
                                    break;
                                }
                            }
                            if (!peerContained) {
                                log("checkParticipants "+peersOnCall.get(i).getPeerId()+" removed of peersOnCall");
                                peersOnCall.remove(i);
                                changes = true;
                            }
                        }
                        if (changes) {
                            updatePeers();
                        }

                    } else {
                        log("checkParticipants:peersOnCall is empty, add all");
                        //peersOnCall empty
                        boolean changes = false;

                        for (int i = 0; i < call.getPeeridParticipants().size(); i++) {
                            long userPeerid = call.getPeeridParticipants().get(i);
                            long userClientid = call.getClientidParticipants().get(i);

                            if ((userPeerid == megaChatApi.getMyUserHandle()) && (userClientid == megaChatApi.getMyClientidHandle(chatId))) {
                                InfoPeerGroupCall myPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), call.hasLocalVideo(), call.hasLocalAudio(), false, true, null);
                                log("checkParticipants I added in peersOnCall");
                                peersOnCall.add(myPeer);
                                changes = true;
                            } else {
                                MegaChatSession sessionPeer = call.getMegaChatSession(userPeerid,userClientid);
                                if((sessionPeer!=null)&&(sessionPeer.getStatus() <= MegaChatSession.SESSION_STATUS_IN_PROGRESS)) {
                                    InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);
                                    log("checkParticipants " + userPeer.getPeerId() + " added in peersOnCall ");
                                    peersOnCall.add((peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1)), userPeer);
                                    changes = true;
                                }
                            }
                        }
                        if (changes) {
                            updatePeers();

                        }
                    }
                    if ((megaChatApi!=null)&&(peersOnCall != null) && (peersOnCall.size() > 0)) {
                        log("checkParticipants update Video&&Audio local&&remoto");
                        for (int i = 0; i < peersOnCall.size(); i++) {
                            if ((peersOnCall.get(i).getPeerId() == megaChatApi.getMyUserHandle()) && (peersOnCall.get(i).getClientId() == megaChatApi.getMyClientidHandle(chatId))) {
                                //Me
                                updateLocalVideoStatus();
                                updateLocalAudioStatus();
                            } else {
                                //Contact
                                updateRemoteVideoStatus(peersOnCall.get(i).getPeerId(), peersOnCall.get(i).getClientId());
                                updateRemoteAudioStatus(peersOnCall.get(i).getPeerId(), peersOnCall.get(i).getClientId());
                            }
                        }
                    }
                }
            }
        }
    }

    private void refreshLayoutContactMicro(){
        if(!chat.isGroup()) {
            if (callChat == null) {
                if (megaChatApi != null) {
                    callChat = megaChatApi.getChatCall(chatId);
                }
            }
            if (callChat != null) {
                MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
                if (userSession != null) {
                    if (userSession.hasAudio()) {
                        log("onPrepareOptionsMenu:Audio remote connected");
                        mutateCallLayout.setVisibility(GONE);
                        refreshOwnMicro();

                    } else {
                        log("onPrepareOptionsMenu:Audio remote NOT connected");
                        String name = chat.getPeerFirstname(0);
                        if ((name == null) || (name == " ")) {
                            if(megaChatApi!=null){
                                name = megaChatApi.getContactEmail(callChat.getSessionsPeerid().get(0));
                                if (name == null) {
                                    name = " ";
                                }
                            }
                        }
                        mutateCallText.setText(getString(R.string.muted_contact_micro, name));
                        mutateCallLayout.setVisibility(View.VISIBLE);

                        if(mutateOwnCallLayout.getVisibility() == View.VISIBLE){
                            mutateOwnCallLayout.setVisibility(GONE);
                        }
                    }
                }
            }
        }
    }

    public void refreshOwnMicro(){
        log("refreshOwnMicro");
        if(!chat.isGroup()){
            if (callChat == null) {
                if(megaChatApi != null){
                    callChat = megaChatApi.getChatCall(chatId);
                }
            }
            if(callChat != null){
                if(callChat.hasLocalAudio()){
                    if (localCameraFragment != null){
                        localCameraFragment.showMicro(false);
                    }
                    mutateOwnCallLayout.setVisibility(View.GONE);

                }else{
                    if(callChat.hasLocalVideo()){
                        if (localCameraFragment != null){
                            localCameraFragment.showMicro(true);
                        }
                        mutateOwnCallLayout.setVisibility(View.GONE);
                    }else{
                        if (localCameraFragment != null){
                            localCameraFragment.showMicro(false);
                        }

                        if(mutateCallLayout.getVisibility() == View.VISIBLE){
                            mutateOwnCallLayout.setVisibility(GONE);
                        }else{
                            mutateOwnCallLayout.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }
    }

}
