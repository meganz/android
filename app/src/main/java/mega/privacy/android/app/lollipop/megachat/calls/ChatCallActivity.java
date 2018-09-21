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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.OnSwipeTouchListener;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.CustomItemClickListener;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.GroupCallAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
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
import static mega.privacy.android.app.utils.Util.brandAlertDialog;
import static mega.privacy.android.app.utils.Util.context;

public class ChatCallActivity extends AppCompatActivity implements MegaChatRequestListenerInterface, MegaChatCallListenerInterface, MegaRequestListenerInterface, View.OnClickListener, SensorEventListener, KeyEvent.Callback {

    DatabaseHandler dbH = null;
    ChatItemPreferences chatPrefs = null;
    MegaUser myUser;
    Chronometer myChrono;

    public static int REMOTE_VIDEO_NOT_INIT = -1;
    public static int REMOTE_VIDEO_ENABLED = 1;
    public static int REMOTE_VIDEO_DISABLED = 0;

    float widthScreenPX, heightScreenPX;

    // flagMyAvatar if true - small avatar circle is contact's avatar
    // flagMyAvatar if false - small avatar circle is my avatar
    boolean flagMyAvatar;

    // flagContactAvatar if true - big avatar circle is contact's avatar
    // flagContactAvatar if false - big avatar circle is my avatar
    boolean flagContactAvatar;

    long chatId;
    MegaChatRoom chat;
    MegaChatCall callChat;
    private MegaApiAndroid megaApi = null;
    MegaChatApiAndroid megaChatApi = null;
    private Handler handler;
    Display display;
    DisplayMetrics outMetrics;
    float density;
    float scaleW;
    float scaleH;
    AppBarLayout appBarLayout;
    Toolbar tB;
    ActionBar aB;
    boolean avatarRequested = false;
//    InfoPeerGroupCall peerSelected = null;

    ArrayList<InfoPeerGroupCall> peersOnCall = new ArrayList<>();
    ArrayList<InfoPeerGroupCall> peersBeforeCall = new ArrayList<>();

    Timer timer = null;
    Timer ringerTimer = null;
    long milliseconds = 0;

    RelativeLayout smallElementsIndividualCallLayout;
    RelativeLayout bigElementsIndividualCallLayout;
    RelativeLayout bigElementsGroupCallLayout;

    CustomizedGridCallRecyclerView recyclerView;

    LinearLayoutManager layoutManager;
    RecyclerView bigRecyclerView;
    GroupCallAdapter adapter;

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

    static ChatCallActivity chatCallActivityActivity = null;

    private MenuItem remoteAudioIcon;

    FloatingActionButton videoFAB;
    FloatingActionButton microFAB;
    FloatingActionButton hangFAB;
    FloatingActionButton answerCallFAB;

    AudioManager audioManager;
    MediaPlayer thePlayer;

    FrameLayout fragmentContainerLocalCamera;
    FrameLayout fragmentContainerLocalCameraFS;
    FrameLayout fragmentContainerRemoteCameraFS;

    ViewGroup parentLocal;
    ViewGroup parentLocalFS;
    ViewGroup parentRemoteFS;

    private LocalCameraCallFragment localCameraFragment;
    private LocalCameraCallFullScreenFragment localCameraFragmentFS = null;
    private RemoteCameraCallFullScreenFragment remoteCameraFragmentFS = null;

//    //Big elements for group call (more than 6 users)
//    FrameLayout fragmentContainerBigCameraGroupCall;
//    ViewGroup parentBigCameraGroupCall;
//    private BigCameraGroupCallFragment bigCameraGroupCallFragment = null;
//    RelativeLayout bigAvatarGroupCallLayout;
//    RoundedImageView bigAvatarGroupCallImage;
//    TextView bigAvatarGroupCallInitialLetter;

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

    long startTime, timeInMilliseconds = 0;
    Handler customHandler = new Handler();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_calls_chat, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        log("onPrepareOptionsMenu");

        remoteAudioIcon = menu.findItem(R.id.info_remote_audio);
        if(chat.isGroup()){
            remoteAudioIcon.setVisible(false);
        }
        else{
            MegaChatSession userSession = callChat.getMegaChatSession(chat.getPeerHandle(0));

            if(callChat!=null && userSession!=null){
                if(userSession.hasAudio()){
                    log("Audio remote connected");
                    remoteAudioIcon.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_volume_up_white));
                }
                else{
                    log("Audio remote NOT connected");
                    remoteAudioIcon.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_volume_off_white));
                }
            }
            else{
                log("callChat is Null");
            }
            remoteAudioIcon.setVisible(true);
            remoteAudioIcon.setEnabled(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                log("Hang call");
                onBackPressed();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateScreenStatusInProgress(){
        log("updateScreenStatusInProgress()");
        relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
        relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
        flagMyAvatar = true;
        setProfileMyAvatar();
        flagContactAvatar = false;
        setProfileContactAvatar();

        if(chat.isGroup()){

            relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
            relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
            relativeVideo.requestLayout();

            if((peersBeforeCall != null)&&(peersBeforeCall.size() != 0)){
                peersBeforeCall.clear();
            }

            if((peersOnCall != null)&&(peersOnCall.size() != 0)){
                boolean changes = false;
                for(int i = 0; i < callChat.getParticipants().size(); i++){
                    long userHandle = callChat.getParticipants().get(i);
                    if((peersOnCall != null)&&(peersOnCall.size() != 0)) {
                        boolean peerContain = false;
                        for (InfoPeerGroupCall peer : peersOnCall) {
                            if (peer.getHandle().equals(userHandle)) {
                                peerContain = true;
                                break;
                            }
                        }
                        if (!peerContain) {
                            if(userHandle == megaChatApi.getMyUserHandle()){
                                log("updateScreenStatusInProgress()-peersOnCall.add("+megaChatApi.getMyFullname()+")-> ME");

                                InfoPeerGroupCall myPeer = new InfoPeerGroupCall(megaChatApi.getMyUserHandle(),  megaChatApi.getMyFullname(), callChat.hasLocalVideo(), callChat.hasLocalAudio(), null, null);
                                peersOnCall.add(myPeer);
                                changes = true;
                            }else{
                                MegaChatSession userSession = callChat.getMegaChatSession(userHandle);
                                if(userSession!=null){
                                    if(userSession.getStatus()==MegaChatSession.SESSION_STATUS_IN_PROGRESS){
                                        log("updateScreenStatusInProgress()-peersOnCall.add("+chat.getPeerFullnameByHandle(userHandle)+")");
                                        InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userHandle,  chat.getPeerFullnameByHandle(userHandle), userSession.hasVideo(), userSession.hasAudio(), null, null);
                                        peersOnCall.add(0, userPeer);
                                        changes = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if(changes){
                    createNewAdapter(true);
                }

            }else{
                boolean changes = false;
                for(int i = 0; i < callChat.getParticipants().size(); i++){
                    long userHandle = callChat.getParticipants().get(i);
                    if(userHandle == megaChatApi.getMyUserHandle()){
                        InfoPeerGroupCall myPeer = new InfoPeerGroupCall(megaChatApi.getMyUserHandle(),  megaChatApi.getMyFullname(), callChat.hasLocalVideo(), callChat.hasLocalAudio(), null, null);
                        peersOnCall.add(myPeer);
                        changes = true;
                    }else{
                        MegaChatSession userSession = callChat.getMegaChatSession(userHandle);
                        if(userSession!=null){
                            if(userSession.getStatus()==MegaChatSession.SESSION_STATUS_IN_PROGRESS){
                                InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userHandle,  chat.getPeerFullnameByHandle(userHandle), userSession.hasVideo(), userSession.hasAudio(), null,null);
                                peersOnCall.add(0, userPeer);
                                changes = true;
                            }else{
                            }
                        }

                    }
                }
                if(changes){
                    createNewAdapter(true);
                }
            }

        }else{
            relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
            relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
            flagMyAvatar = false;
            setProfileMyAvatar();
            flagContactAvatar = true;
            setProfileContactAvatar();
        }

        updateLocalVideoStatus();
        updateLocalAudioStatus();
        updateRemoteAudioStatus(-1);
        updateRemoteVideoStatus(-1);

        stopAudioSignals();
        startClock();
    }


    public void updateSubTitle(){
        log("updateSubTitle");
        int sessionStatus = -1;

        if(callChat.getStatus()<=MegaChatCall.CALL_STATUS_RING_IN){
            aB.setSubtitle(getString(R.string.call_starting));
        }else if(callChat.getStatus()==MegaChatCall.CALL_STATUS_IN_PROGRESS){
            if(chat.isGroup()){
                startClock();
                int totalParticipants = callChat.getNumParticipants() + 1;
                log("update subtitle: "+totalParticipants +" of "+chat.getPeerCount());
            }
            else{
                MegaChatSession userSession = callChat.getMegaChatSession(chat.getPeerHandle(0));
                if(userSession!=null){
                    sessionStatus = userSession.getStatus();
                    log("sessionStatus: "+sessionStatus);
                    if(sessionStatus==MegaChatSession.SESSION_STATUS_IN_PROGRESS){
                        startClock();
                    }
                    else{
                        aB.setSubtitle(getString(R.string.chat_connecting));
                    }
                }
                else{
                    log("Error getting the session of the user");
                    aB.setSubtitle(null);
                }
            }
        }
        else{
            aB.setSubtitle(null);
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log("onNewIntent()");

        Bundle extras = intent.getExtras();
        log(getIntent().getAction());
        if (extras != null) {
            long newChatId = extras.getLong("chatHandle", -1);
            log("New chat id: "+newChatId);
            if(chatId==newChatId){
                log("Its the same call");
            }else{
                log("New intent to the activity with a new chatId");
                //Check the new call if in progress
                chatId = newChatId;
                chat = megaChatApi.getChatRoom(chatId);
                callChat = megaChatApi.getChatCall(chatId);

                aB.setTitle(chat.getTitle());
                updateSubTitle();

                updateScreenStatusInProgress();

                log("Start call Service");
                Intent intentService = new Intent(this, CallService.class);
                intentService.putExtra("chatHandle", callChat.getChatid());
                this.startService(intentService);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calls_chat);

        chatCallActivityActivity = this;

        MegaApplication.setShowPinScreen(true);

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        widthScreenPX = outMetrics.widthPixels;
        heightScreenPX = outMetrics.heightPixels;
        density = getResources().getDisplayMetrics().density;
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        MegaApplication app = (MegaApplication) getApplication();
        if (megaApi == null) {
            megaApi = app.getMegaApi();
        }

        log("retryPendingConnections()");
        if (megaApi != null) {
            log("---------retryPendingConnections");
            megaApi.retryPendingConnections();
        }

        if (megaChatApi != null){
            megaChatApi.retryPendingConnections(false, null);
        }

        if (megaChatApi == null) {
            megaChatApi = app.getMegaChatApi();
        }

        if(megaApi==null||megaApi.getRootNode()==null){
            log("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
            log("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        megaChatApi.addChatCallListener(this);
        myUser = megaApi.getMyUser();

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        handler = new Handler();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        try {
            field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable ignored) {}

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

// Wake screen for Xiaomi devices
//        if (!powerManager.isInteractive()){ // if screen is not already on, turn it on (get wake_lock for 10 seconds)
//            PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.ON_AFTER_RELEASE,"MH24_SCREENLOCK");
//            wl.acquire(10000);
//            PowerManager.WakeLock wl_cpu = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MH24_SCREENLOCK");
//            wl_cpu.acquire(10000);
//        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tB = (Toolbar) findViewById(R.id.call_toolbar);
        if (tB == null) {
            log("Tb is Null");
            return;
        }
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setTitle(" ");

        myChrono = new Chronometer(context);

        smallElementsIndividualCallLayout = (RelativeLayout) findViewById(R.id.small_elements_individual_call);
        smallElementsIndividualCallLayout.setVisibility(GONE);

        bigElementsIndividualCallLayout = (RelativeLayout) findViewById(R.id.big_elements_individual_call);
        bigElementsIndividualCallLayout.setVisibility(GONE);

        linearFAB = (LinearLayout) findViewById(R.id.linear_buttons);
        RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        linearFAB.setLayoutParams(layoutCompress);
        linearFAB.setOrientation(LinearLayout.HORIZONTAL);

        relativeCall = (RelativeLayout) findViewById(R.id.relative_answer_call_fab);
        relativeCall.setVisibility(GONE);

        linearArrowCall = (LinearLayout) findViewById(R.id.linear_arrow_call);
        firstArrowCall = (ImageView) findViewById(R.id.first_arrow_call);
        secondArrowCall = (ImageView) findViewById(R.id.second_arrow_call);
        thirdArrowCall = (ImageView) findViewById(R.id.third_arrow_call);
        fourArrowCall = (ImageView) findViewById(R.id.four_arrow_call);

        relativeVideo = (RelativeLayout) findViewById(R.id.relative_video_fab);
        relativeVideo.setVisibility(GONE);

        linearArrowVideo = (LinearLayout) findViewById(R.id.linear_arrow_video);
        firstArrowVideo = (ImageView) findViewById(R.id.first_arrow_video);
        secondArrowVideo = (ImageView) findViewById(R.id.second_arrow_video);
        thirdArrowVideo = (ImageView) findViewById(R.id.third_arrow_video);
        fourArrowVideo = (ImageView) findViewById(R.id.four_arrow_video);

        answerCallFAB = (FloatingActionButton) findViewById(R.id.answer_call_fab);
        answerCallFAB.setVisibility(GONE);

        videoFAB = (FloatingActionButton) findViewById(R.id.video_fab);
        videoFAB.setOnClickListener(this);
        videoFAB.setVisibility(GONE);

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
        recyclerView = (CustomizedGridCallRecyclerView) findViewById(R.id.recycler_view_cameras);
        recyclerView.setPadding(0, 0, 0, 0);
        recyclerView.setClipToPadding(false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setColumnWidth((int) widthScreenPX);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setVisibility(GONE);

        //Recycler View for 7-8 peers (because 9-10 without video)
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        bigRecyclerView = (RecyclerView) findViewById(R.id.big_recycler_view_cameras);
        bigRecyclerView.setLayoutManager(layoutManager);
        bigRecyclerView.setClipToPadding(false);
        bigRecyclerView.setHasFixedSize(true);
        bigRecyclerView.setVisibility(GONE);

//        //Big elements group calls
//        parentBigCameraGroupCall = (ViewGroup) findViewById(R.id.parent_layout_big_camera_group_call);
//        ViewGroup.LayoutParams paramsBigCameraGroupCall = (ViewGroup.LayoutParams) parentBigCameraGroupCall.getLayoutParams();
//        if(widthScreenPX<heightScreenPX){
//            paramsBigCameraGroupCall.width = (int)widthScreenPX;
//            paramsBigCameraGroupCall.height = (int)widthScreenPX;
//        }else{
//            paramsBigCameraGroupCall.width = (int)heightScreenPX;
//            paramsBigCameraGroupCall.height = (int)heightScreenPX;
//        }
//
//        parentBigCameraGroupCall.setLayoutParams(paramsBigCameraGroupCall);
//
//        fragmentContainerBigCameraGroupCall = (FrameLayout) findViewById(R.id.fragment_container_big_camera_group_call);
//        fragmentContainerBigCameraGroupCall.setVisibility(View.GONE);
//
//        bigAvatarGroupCallLayout = (RelativeLayout) findViewById(R.id.big_camera_group_call_avatar_rl);
//        bigAvatarGroupCallImage = (RoundedImageView) findViewById(R.id.big_camera_group_call_image);
//        bigAvatarGroupCallInitialLetter = (TextView) findViewById(R.id.big_camera_group_call_initial_letter);
//        bigAvatarGroupCallLayout.setVisibility(View.GONE);
//
//        parentBigCameraGroupCall.setVisibility(View.GONE);

        //Local camera small
        parentLocal = (ViewGroup) findViewById(R.id.parent_layout_local_camera);
        fragmentContainerLocalCamera = (FrameLayout) findViewById(R.id.fragment_container_local_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)fragmentContainerLocalCamera.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        fragmentContainerLocalCamera.setLayoutParams(params);
        fragmentContainerLocalCamera.setOnTouchListener(new OnDragTouchListener(fragmentContainerLocalCamera,parentLocal));
        parentLocal.setVisibility(View.GONE);
        fragmentContainerLocalCamera.setVisibility(View.GONE);

        //Local camera Full Screen
        parentLocalFS = (ViewGroup) findViewById(R.id.parent_layout_local_camera_FS);
        fragmentContainerLocalCameraFS = (FrameLayout) findViewById(R.id.fragment_container_local_cameraFS);
        RelativeLayout.LayoutParams paramsFS = (RelativeLayout.LayoutParams)fragmentContainerLocalCameraFS.getLayoutParams();
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerLocalCameraFS.setLayoutParams(paramsFS);
        parentLocalFS.setVisibility(View.GONE);
        fragmentContainerLocalCameraFS.setVisibility(View.GONE);

        //Remote camera Full Screen
        parentRemoteFS = (ViewGroup) findViewById(R.id.parent_layout_remote_camera_FS);
        fragmentContainerRemoteCameraFS = (FrameLayout) findViewById(R.id.fragment_container_remote_cameraFS);
        RelativeLayout.LayoutParams paramsRemoteFS = (RelativeLayout.LayoutParams)fragmentContainerRemoteCameraFS.getLayoutParams();
        paramsRemoteFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        paramsRemoteFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
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
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
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

            videoFAB.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_video_off));

            //Contact's avatar
            chatId = extras.getLong("chatHandle", -1);
            log("Chat handle to call: " + chatId);
            if (chatId != -1) {
                chat = megaChatApi.getChatRoom(chatId);
                callChat = megaChatApi.getChatCall(chatId);

                if (callChat == null){
                    megaChatApi.removeChatCallListener(this);
                    MegaApplication.activityPaused();
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        super.finishAndRemoveTask();
                    }
                    else {
                        super.finish();
                    }
                    return;
                }

                log("Start call Service");
                Intent intentService = new Intent(this, CallService.class);
                intentService.putExtra("chatHandle", callChat.getChatid());
                this.startService(intentService);

                audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                int callStatus = callChat.getStatus();
                log("The status of the callChat is: " + callStatus);

                aB.setTitle(chat.getTitle());
                updateSubTitle();

                if(chat.isGroup()){
                    smallElementsIndividualCallLayout.setVisibility(View.GONE);
                    bigElementsIndividualCallLayout.setVisibility(View.GONE);
                    bigElementsGroupCallLayout.setVisibility(View.VISIBLE);

                }else{
                    smallElementsIndividualCallLayout.setVisibility(View.VISIBLE);
                    bigElementsIndividualCallLayout.setVisibility(View.VISIBLE);
                    bigElementsGroupCallLayout.setVisibility(View.GONE);
                }

                if(callStatus==MegaChatCall.CALL_STATUS_RING_IN){

                    ringtone = RingtoneManager.getRingtone(this, DEFAULT_RINGTONE_URI);
                    ringerTimer = new Timer();
                    MyRingerTask myRingerTask = new MyRingerTask();
                    ringerTimer.schedule(myRingerTask, 0, 500);

                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 1000, 500, 500, 1000};
                    if (vibrator != null){
                        if (vibrator.hasVibrator()){
                            //FOR API>=26
                            //vibrator.vibrate(createWaveform(pattern, 0), USAGE_NOTIFICATION_RINGTONE); ??
                            vibrator.vibrate(pattern, 0);
                        }
                    }

                    if(chat.isGroup()){
                        log("onCreate()-> Incoming group call");

                        relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                        relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.MATCH_PARENT;
                        relativeVideo.requestLayout();

                        //Get all the participants, add them to peersBeforeCall array and show only the avatars
                        if((peersBeforeCall != null)&&(peersBeforeCall.size() != 0)){
                            peersBeforeCall.clear();
                        }

                        if((peersOnCall != null)&&(peersOnCall.size() != 0)){
                            peersOnCall.clear();
                        }

                        if(callChat.getParticipants().size()!=0){
                            boolean changes = false;
                            for(int i = 0; i < callChat.getParticipants().size(); i++){
                                long userHandle = callChat.getParticipants().get(i);
                                InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userHandle,  chat.getPeerFullnameByHandle(userHandle), false, false, null, null);
                                peersBeforeCall.add(0, userPeer);
                                changes = true;
                            }
                            if(changes){
                                createNewAdapter(false);
                            }
                        }

                    }else{
                        log("Incoming individual call");

                        relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                        relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.MATCH_PARENT;
                        relativeVideo.requestLayout();

                        myAvatarLayout.setVisibility(View.VISIBLE);
                        contactAvatarLayout.setVisibility(View.VISIBLE);
                        flagMyAvatar = true;
                        setProfileMyAvatar();
                        flagContactAvatar = false;
                        setProfileContactAvatar();
                    }

                }else if(callStatus==MegaChatCall.CALL_STATUS_IN_PROGRESS){
                    log("onCreate()-> InProgress");
                    updateScreenStatusInProgress();

                }else{


                    int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (volume == 0) {
                        toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
                        toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 60000);
                    }else {
                        thePlayer = MediaPlayer.create(getApplicationContext(), R.raw.outgoing_voice_video_call);
                        thePlayer.setLooping(true);
                        thePlayer.start();
                    }

                    if(chat.isGroup()){
                        log("onCreate()-> Outgoing group call");

                        relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                        relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
                        relativeVideo.requestLayout();

                        if((peersOnCall != null)&&(peersOnCall.size() != 0)){
                            peersOnCall.clear();
                        }

                        if((peersBeforeCall != null)&&(peersBeforeCall.size() != 0)){
                            peersBeforeCall.clear();
                        }

                        InfoPeerGroupCall myPeer = new InfoPeerGroupCall(megaChatApi.getMyUserHandle(),  megaChatApi.getMyFullname(), callChat.hasLocalVideo(), callChat.hasLocalAudio(), null, null);
                        peersOnCall.add(myPeer);
                        createNewAdapter(true);

                    }else{
                        log("Outgoing individual call");

                        relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
                        relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                        relativeVideo.requestLayout();

                        flagMyAvatar = false;
                        setProfileMyAvatar();
                        flagContactAvatar = true;
                        setProfileContactAvatar();
                        myAvatarLayout.setVisibility(View.VISIBLE);
                    }
                    updateLocalVideoStatus();
                    updateLocalAudioStatus();
                }
            }
        }
        if(checkPermissions()){
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
        log("onRequestFinish(): "+request.getEmail());
    }


    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {}

    public void createDefaultAvatar(long userHandle,  String fullName) {
        log("createDefaultAvatar");

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);

        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(userHandle));
        if (color != null) {
            log("The color to set the avatar is " + color);
            p.setColor(Color.parseColor(color));
        } else {
            log("Default color to the avatar");
            p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        }else {
            radius = defaultAvatar.getHeight() / 2;
        }
        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        if(flagContactAvatar){
            myImage.setImageBitmap(defaultAvatar);
            String contactFirstLetter = fullName.charAt(0) + "";
            contactFirstLetter = contactFirstLetter.toUpperCase(Locale.getDefault());
            myInitialLetter.setText(contactFirstLetter);
            myInitialLetter.setTextSize(40);
            myInitialLetter.setTextColor(Color.WHITE);
            myInitialLetter.setVisibility(View.VISIBLE);
        }else {
            contactImage.setImageBitmap(defaultAvatar);
            String contactFirstLetter = fullName.charAt(0) + "";
            contactFirstLetter = contactFirstLetter.toUpperCase(Locale.getDefault());
            contactInitialLetter.setText(contactFirstLetter);
            contactInitialLetter.setTextSize(60);
            contactInitialLetter.setTextColor(Color.WHITE);
            contactInitialLetter.setVisibility(View.VISIBLE);
        }
    }

    public void setProfileContactAvatar(){
        log("setProfileContactAvatar");
        Bitmap bitmap = null;
        File avatar = null;
        if (context.getExternalCacheDir() != null) {
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), chat.getPeerEmail(0) + ".jpg");
        } else {
            avatar = new File(context.getCacheDir().getAbsolutePath(), chat.getPeerEmail(0) + ".jpg");
        }

        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, bitmap, 3);
                if (bitmap != null) {
                    if(flagContactAvatar){
                        myImage.setImageBitmap(bitmap);
                        myInitialLetter.setVisibility(GONE);
                    }else{
                        contactImage.setImageBitmap(bitmap);
                        contactInitialLetter.setVisibility(GONE);
                    }
                }
                else{
                    UserAvatarListener listener = new UserAvatarListener(this);
                    avatar.delete();
                    if(!avatarRequested){
                        avatarRequested = true;
                        if (context.getExternalCacheDir() != null){
                            megaApi.getUserAvatar(chat.getPeerEmail(0), context.getExternalCacheDir().getAbsolutePath() + "/" + chat.getPeerEmail(0) + ".jpg", listener);
                        }
                        else{
                            megaApi.getUserAvatar(chat.getPeerEmail(0), context.getCacheDir().getAbsolutePath() + "/" + chat.getPeerEmail(0) + ".jpg", listener);
                        }
                    }

                    createDefaultAvatar(chat.getPeerHandle(0), chat.getPeerFullname(0));
                }
            }
            else{
                UserAvatarListener listener = new UserAvatarListener(this);

                if(!avatarRequested){
                    avatarRequested = true;
                    if (context.getExternalCacheDir() != null){
                        megaApi.getUserAvatar(chat.getPeerEmail(0), context.getExternalCacheDir().getAbsolutePath() + "/" + chat.getPeerEmail(0) + ".jpg", listener);
                    }
                    else{
                        megaApi.getUserAvatar(chat.getPeerEmail(0), context.getCacheDir().getAbsolutePath() + "/" + chat.getPeerEmail(0) + ".jpg", listener);
                    }
                }

                createDefaultAvatar(chat.getPeerHandle(0), chat.getPeerFullname(0));
            }
        }
        else{
            UserAvatarListener listener = new UserAvatarListener(this);

            if(!avatarRequested){
                avatarRequested = true;
                if (context.getExternalCacheDir() != null){
                    megaApi.getUserAvatar(chat.getPeerEmail(0), context.getExternalCacheDir().getAbsolutePath() + "/" + chat.getPeerEmail(0) + ".jpg", listener);
                }
                else{
                    megaApi.getUserAvatar(chat.getPeerEmail(0), context.getCacheDir().getAbsolutePath() + "/" + chat.getPeerEmail(0) + ".jpg", listener);
                }
            }

            createDefaultAvatar(chat.getPeerHandle(0), chat.getPeerFullname(0));
        }
    }

    public void createMyDefaultAvatar() {
        log("createMyDefaultAvatar");

        String myFullName = megaChatApi.getMyFullname();
        String myFirstLetter=myFullName.charAt(0) + "";
        myFirstLetter = myFirstLetter.toUpperCase(Locale.getDefault());
        long userHandle = megaChatApi.getMyUserHandle();

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels,outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);

        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(userHandle));

        if(color!=null){
            log("The color to set the avatar is "+color);
            p.setColor(Color.parseColor(color));
        }
        else{
            log("Default color to the avatar");

            p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        if(flagMyAvatar){
            myImage.setImageBitmap(defaultAvatar);
            myInitialLetter.setText(myFirstLetter);
            myInitialLetter.setTextSize(40);
            myInitialLetter.setTextColor(Color.WHITE);
            myInitialLetter.setVisibility(View.VISIBLE);
        }else{
            contactImage.setImageBitmap(defaultAvatar);
            contactInitialLetter.setText(myFirstLetter);
            contactInitialLetter.setTextSize(60);
            contactInitialLetter.setTextColor(Color.WHITE);
            contactInitialLetter.setVisibility(View.VISIBLE);
        }

    }

    public void setProfileMyAvatar() {
        log("setProfileMyAvatar: "+ flagMyAvatar);
        Bitmap myBitmap = null;
        File avatar = null;
        if (context != null) {
            log("context is not null");
            if (context.getExternalCacheDir() != null) {
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), megaChatApi.getMyEmail() + ".jpg");
            } else {
                avatar = new File(context.getCacheDir().getAbsolutePath(), megaChatApi.getMyEmail() + ".jpg");
            }
        }
        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                myBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                myBitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, myBitmap, 3);
                if (myBitmap != null) {
                    if(flagMyAvatar){
                        myImage.setImageBitmap(myBitmap);
                        myInitialLetter.setVisibility(GONE);
                    }else{
                        contactImage.setImageBitmap(myBitmap);
                        contactInitialLetter.setVisibility(GONE);
                    }
                }
                else{
                    createMyDefaultAvatar();
                }
            }
            else {
                createMyDefaultAvatar();
            }
        } else {
            createMyDefaultAvatar();
        }
    }

    protected void hideActionBar(){
        if (aB != null && aB.isShowing()) {
            if(tB != null) {
               tB.animate().translationY(-220).setDuration(800L)
                        .withEndAction(new Runnable() {
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

    protected void showActionBar(){
        if (aB != null && !aB.isShowing()) {
            aB.show();
            if(tB != null) {
                tB.animate().translationY(0).setDuration(800L).start();

            }
        }
    }

    protected void hideFABs(){
        if(videoFAB.getVisibility() == View.VISIBLE){
            videoFAB.hide();
        }
        if(microFAB.getVisibility() == View.VISIBLE){
            microFAB.hide();
        }
        if(hangFAB.getVisibility() == View.VISIBLE){
            hangFAB.hide();
        }
        if(answerCallFAB.getVisibility() == View.VISIBLE){
            answerCallFAB.hide();
            relativeCall.setVisibility(GONE);

        }
    }

    @Override
    public void onPause(){
        log("onPause");

        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        log("onResume()");
        if(peersOnCall!=null){
            if(peersOnCall.size()!=0){
                if(peersOnCall.size()<7){
                    for(InfoPeerGroupCall peer :peersOnCall){
                        if(peer.getListener()!=null){
                            if(peer.getListener().getHeight() != 0){
                                peer.getListener().setHeight(0);
                            }
                            if(peer.getListener().getWidth() != 0){
                                peer.getListener().setWidth(0);
                            }
                        }
                    }
                }else{
                    for(InfoPeerGroupCall peer :peersOnCall){
                        if(peer.getListener()!=null){
                            peer.getListener().setHeight(0);
                            peer.getListener().setWidth(0);
                        }
                    }
                }

            }
        }


        super.onResume();
//        adapter.notifyDataSetChanged();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        MegaApplication.activityResumed();
        if((callChat.getStatus()==MegaChatCall.CALL_STATUS_IN_PROGRESS)||(callChat.getStatus()==MegaChatCall.CALL_STATUS_REQUEST_SENT)){
            ((MegaApplication) getApplication()).sendSignalPresenceActivity();
        }
    }

    @Override
    public void onDestroy(){
        log("onDestroy()");

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
//            megaChatApi.removeChatVideoListener(chatId, userHandle, this);
        }

        if (customHandler != null){
            customHandler.removeCallbacksAndMessages(null);
        }

        peersOnCall.clear();
        peersBeforeCall.clear();
        stopAudioSignals();
        mSensorManager.unregisterListener(this);
        MegaApplication.activityPaused();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed");
//		if (overflowMenuLayout != null){
//			if (overflowMenuLayout.getVisibility() == View.VISIBLE){
//				overflowMenuLayout.setVisibility(View.GONE);
//				return;
//			}
//		}
//        super.onBackPressed();
        super.onBackPressed();

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
//            megaChatApi.removeChatVideoListener(chatId, userHandle, this);
        }

        if (customHandler != null){
            customHandler.removeCallbacksAndMessages(null);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        }
        else {
            super.finish();
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        log("onRequestStart: "+request.getType());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
        log("onRequestUpdate: "+request.getType());


    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish: "+request.getType());

        if(request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL){
            MegaApplication.activityPaused();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                super.finishAndRemoveTask();
            }
            else {
                super.finish();
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                videoFAB.setVisibility(View.VISIBLE);
                relativeVideo.setVisibility(View.VISIBLE);

                microFAB.setVisibility(View.VISIBLE);
                answerCallFAB.setVisibility(GONE);
                relativeCall.setVisibility(GONE);
                linearArrowVideo.setVisibility(GONE);

                if(request.getFlag()==true){
                    log("Ok answer with video");
//                    updateLocalVideoStatus();

                }
                else{
                    log("Ok answer with NO video - ");
//                    updateLocalVideoStatus();
                }

                updateLocalVideoStatus();
            }
            else{
                log("Error call: "+e.getErrorString());
                MegaApplication.activityPaused();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    super.finishAndRemoveTask();
                }
                else {
                    super.finish();
                }
//                showSnackbar(getString(R.string.clear_history_error));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL){

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
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
            }
            else{
                log("Error changing audio or video: "+e.getErrorString());
//                showSnackbar(getString(R.string.clear_history_error));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {
        log("onChatCallUpdate() ");

        if(call.getChatid()==chatId){
            this.callChat = call;

            if(callChat.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)){
                log("CHANGE_TYPE_STATUS");

                int callStatus = callChat.getStatus();
                switch (callStatus){
                    case MegaChatCall.CALL_STATUS_IN_PROGRESS:{

                        if(chat.isGroup()){
                            log("CALL_STATUS_IN_PROGRESS");

                            if((peersBeforeCall != null)&&(peersBeforeCall.size() != 0)){
                                peersBeforeCall.clear();
                            }

                            if((peersOnCall != null)&&(peersOnCall.size() != 0)){
                                boolean peerContain = false;
                                for(InfoPeerGroupCall peer : peersOnCall) {
                                    if (peer.getHandle().equals(megaChatApi.getMyUserHandle())) {
                                        peerContain = true;
                                        break;
                                    }
                                }
                                if(!peerContain){
                                    InfoPeerGroupCall myPeer = new InfoPeerGroupCall(megaChatApi.getMyUserHandle(),  megaChatApi.getMyFullname(), callChat.hasLocalVideo(), callChat.hasLocalAudio(), null,null);
                                    peersOnCall.add(myPeer);
                                    log("createNewAdapter() -> change my status to IN PROGRESS");
                                    createNewAdapter(true);
                                }
                            }else{
                                InfoPeerGroupCall myPeer = new InfoPeerGroupCall(megaChatApi.getMyUserHandle(),  megaChatApi.getMyFullname(), callChat.hasLocalVideo(), callChat.hasLocalAudio(), null,null);
                                peersOnCall.add(myPeer);
//                                createNewAdapter(true);
                            }
                            updateLocalVideoStatus();
                            updateLocalAudioStatus();

                        }else{
                            flagMyAvatar = true;
                            setProfileMyAvatar();
                            flagContactAvatar = false;
                            setProfileContactAvatar();
                            if (localCameraFragmentFS != null) {
                                localCameraFragmentFS.setVideoFrame(false);
                                FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                                ftFS.remove(localCameraFragmentFS);
                                localCameraFragmentFS = null;
                                contactAvatarLayout.setVisibility(View.VISIBLE);
                                parentLocalFS.setVisibility(View.GONE);
                                fragmentContainerLocalCameraFS.setVisibility(View.GONE);
                            }
                            updateLocalVideoStatus();
                            updateRemoteVideoStatus(-1);
                        }

                        videoFAB.setOnClickListener(null);
                        answerCallFAB.setOnTouchListener(null);
                        videoFAB.setOnTouchListener(null);
                        videoFAB.setOnClickListener(this);
                        updateSubTitle();
                        stopAudioSignals();
                        rtcAudioManager.start(null);
                        showInitialFABConfiguration();
                        break;

                    }
                    case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:{
                        log("CALL_STATUS_TERMINATING_USER_PARTICIPATION");
                        //I have finished the group call but I can join again

                        log("Terminating call of chat: "+chatId);
                        if(chat.isGroup()){
                            stopAudioSignals();
                            rtcAudioManager.stop();
                            MegaApplication.activityPaused();

                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                super.finishAndRemoveTask();
                            }
                            else {
                                super.finish();
                            }
                        }

                        break;
                    }
                    case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:{
                        log("CALL_STATUS_USER_NO_PRESENT");

                        break;
                    }
                    case MegaChatCall.CALL_STATUS_DESTROYED:{
                        log("CALL_STATUS_DESTROYED:TERM code of the call: "+call.getTermCode());
                        //The group call has finished but I can not join again

                        stopAudioSignals();
                        rtcAudioManager.stop();
                        MegaApplication.activityPaused();

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            super.finishAndRemoveTask();
                        }
                        else {
                            super.finish();
                        }
                        break;
                    }
                }
            }
            else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_STATUS)){
                log("CHANGE_TYPE_SESSION_STATUS");

                if(chat.isGroup()){

                    if((peersBeforeCall != null)&&(peersBeforeCall.size() != 0)){
                        peersBeforeCall.clear();
                    }
                    long userHandle = call.getPeerSessionStatusChange();

                    MegaChatSession userSession = callChat.getMegaChatSession(userHandle);
                    if(userSession != null){
                        if(userSession.getStatus()==MegaChatSession.SESSION_STATUS_IN_PROGRESS){
                            log("SESSION_STATUS_IN_PROGRESS");

                            updateSubTitle();

                            //contact joined the group call
                            log(chat.getPeerFullnameByHandle(userHandle)+" joined in the group call");
                            boolean changes = false;

                            if((peersOnCall != null)&&(peersOnCall.size() != 0)){

                                boolean peerContain = false;
                                for(InfoPeerGroupCall peer : peersOnCall) {
                                    if (peer.getHandle().equals(userHandle)) {
                                        peerContain = true;
                                        break;
                                    }
                                }
                                if(!peerContain){

                                    InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userHandle,  chat.getPeerFullnameByHandle(userHandle), userSession.hasVideo(), userSession.hasAudio(), null, null);
                                    peersOnCall.add(0, userPeer);
                                    changes = true;
                                }
                                if(changes){
                                    createNewAdapter(true);
                                }
                            }

                        }else if(userSession.getStatus()==MegaChatSession.SESSION_STATUS_DESTROYED){
                            log("SESSION_STATUS_DESTROYED");

                            updateSubTitle();

                            //contact left the group call
                            log(chat.getPeerFullnameByHandle(userHandle)+" left the group call");
                            boolean changes = false;
                            if((peersOnCall != null)&&(peersOnCall.size() != 0)){

                                for(int i=0;i<peersOnCall.size();i++){
                                    if(peersOnCall.get(i).getHandle() == userHandle){
                                        peersOnCall.remove(i);
                                        changes = true;
                                        break;
                                    }
                                }
                            }

                            if(changes){
                                createNewAdapter(true);
                            }

                        }
                        updateRemoteVideoStatus(userHandle);
                        updateRemoteAudioStatus(userHandle);
                        updateLocalVideoStatus();
                        updateLocalAudioStatus();
                    }


                }
                else{
                    if(call.getPeerSessionStatusChange()==chat.getPeerHandle(0)){
                        updateSubTitle();
                    }

                    updateRemoteVideoStatus(-1);
                    updateRemoteAudioStatus(-1);
                    updateLocalVideoStatus();
                    updateLocalAudioStatus();

                }
            }
            else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_REMOTE_AVFLAGS)){
                log("onChatCallUpdate()-CHANGE_TYPE_REMOTE_AVFLAGS");
                if(chat.isGroup()){
                    updateRemoteVideoStatus(call.getPeerSessionStatusChange());
                    updateRemoteAudioStatus(call.getPeerSessionStatusChange());
                }
                else{
                    if(call.getPeerSessionStatusChange()==chat.getPeerHandle(0)){
                        updateRemoteVideoStatus(-1);
                        updateRemoteAudioStatus(-1);
                    }
                }
            }
            else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)){
                log("onChatCallUpdate()-CHANGE_TYPE_LOCAL_AVFLAGS");
                updateLocalAudioStatus();
                updateLocalVideoStatus();

            }else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_RINGING_STATUS)){
                log("onChatCallUpdate()-CHANGE_TYPE_RINGING_STATUS");

            }else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)){
                log("onChatCallUpdate()-CHANGE_TYPE_CALL_COMPOSITION");
                if(call.getStatus() ==  MegaChatCall.CALL_STATUS_RING_IN){

                    if((peersBeforeCall!=null)&&(peersBeforeCall.size()!=0)){
                        peersBeforeCall.clear();
                    }

                    boolean isMe = false;
                    for(int i = 0; i < call.getParticipants().size(); i++){
                        Long userHandle = call.getParticipants().get(i);
                        if (userHandle.equals(megaChatApi.getMyUserHandle())) {
                            isMe = true;
                            break;
                        }
                    }

                    if(!isMe){
                        boolean changes = false;
                        //Get all participant and add them
                        for(int i = 0; i < call.getParticipants().size(); i++){
                            long userHandle = call.getParticipants().get(i);

                            log("onChatCallUpdate()-peersBeforeCall.add("+chat.getPeerFullnameByHandle(userHandle)+")");
                            InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userHandle,  chat.getPeerFullnameByHandle(userHandle), false, false, null,null);
                            peersBeforeCall.add(0, userPeer);
                            changes = true;
                        }
                        if(changes){
                            createNewAdapter(false);
                        }
                    }
                }

            }else{
                log("other: "+call.getChanges());
            }
        }
    }

    public void stopAudioSignals(){
        log("stopAudioSignals");

        try{
            if(thePlayer!=null){
                thePlayer.stop();
                thePlayer.release();
            }
        }
        catch(Exception e){
            log("Exception stopping player");
        }

        try{
            if (toneGenerator != null) {
                toneGenerator.stopTone();
                toneGenerator.release();
            }
        }
        catch(Exception e){
            log("Exception stopping tone generator");
        }

        try{
            if(ringtone != null){
                ringtone.stop();
            }
        }
        catch(Exception e){
            log("Exception stopping ring tone");
        }

        try{
            if (timer != null){
                timer.cancel();
            }

            if (ringerTimer != null) {
                log("Cancel ringer timer");
                ringerTimer.cancel();
            }

        }
        catch(Exception e){
            log("Exception stopping ringing timer");
        }

        try{
            if (vibrator != null){
                if (vibrator.hasVibrator()) {
                    vibrator.cancel();
                }
            }
        }
        catch(Exception e){
            log("Exception stopping vibrator");
        }

        thePlayer=null;
        toneGenerator = null;
        timer = null;
        ringerTimer = null;
    }


    int width = 0;
    int height = 0;
    Bitmap bitmap;

    @Override
    public void onClick(View v) {
        log("onClick");

        switch (v.getId()) {
            case R.id.call_chat_contact_image_rl:{
//            case R.id.parent_layout_big_camera_group_call:{
                remoteCameraClick();
                break;
            }
            case R.id.video_fab:{
                log("onClick video FAB");

                if(callChat.getStatus()==MegaChatCall.CALL_STATUS_RING_IN){
                    megaChatApi.answerChatCall(chatId, true, this);
                    answerCallFAB.clearAnimation();
                }
                else{
                    if(callChat.hasLocalVideo()){
                        log(" disableVideo");

                        megaChatApi.disableVideo(chatId, this);
                    }
                    else{
                        log(" enableVideo");

                        megaChatApi.enableVideo(chatId, this);
                    }
                }

                if((callChat.getStatus()==MegaChatCall.CALL_STATUS_IN_PROGRESS)||(callChat.getStatus()==MegaChatCall.CALL_STATUS_REQUEST_SENT)){
                    ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                }
                break;
            }
            case R.id.micro_fab: {

                if(callChat.hasLocalAudio()){
                    megaChatApi.disableAudio(chatId, this);
                }
                else{
                    megaChatApi.enableAudio(chatId, this);
                }
                if((callChat.getStatus()==MegaChatCall.CALL_STATUS_IN_PROGRESS)||(callChat.getStatus()==MegaChatCall.CALL_STATUS_REQUEST_SENT)){
                    ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                }
                break;
            }
            case R.id.hang_fab: {
                log("Click on hang fab");
                megaChatApi.hangChatCall(chatId, this);

                if((callChat.getStatus()==MegaChatCall.CALL_STATUS_IN_PROGRESS)||(callChat.getStatus()==MegaChatCall.CALL_STATUS_REQUEST_SENT)){
                    ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                }
                break;
            }
            case R.id.answer_call_fab:{
                log("Click on answer fab");
                megaChatApi.answerChatCall(chatId, false, this);
                videoFAB.clearAnimation();

                ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                break;
            }
        }
    }

    public boolean checkPermissions(){
        log("checkPermissions() ");

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

    public void showInitialFABConfiguration(){
        log("showInitialFABConfiguration() ");

        if(callChat.getStatus()==MegaChatCall.CALL_STATUS_RING_IN){
            relativeCall.setVisibility(View.VISIBLE);
            linearArrowCall.setVisibility(GONE);
            answerCallFAB.show();
            answerCallFAB.setVisibility(View.VISIBLE);
            hangFAB.show();
            hangFAB.setVisibility(View.VISIBLE);

            relativeVideo.setVisibility(View.VISIBLE);
            linearArrowVideo.setVisibility(GONE);
            videoFAB.setVisibility(View.VISIBLE);

            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.accentColor)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_videocam_white));

            microFAB.setVisibility(GONE);

            if (callChat.hasVideoInitialCall()) {

                answerCallFAB.setOnClickListener(this);
                videoFAB.setOnClickListener(null);

                linearArrowVideo.setVisibility(View.VISIBLE);
                videoFAB.startAnimation(shake);

                RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                linearFAB.setLayoutParams(layoutExtend);
                linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                animationAlphaArrows(fourArrowVideo);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        animationAlphaArrows(thirdArrowVideo);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                animationAlphaArrows(secondArrowVideo);
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
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
                        log("onSwipeTop");
                        videoFAB.clearAnimation();

                        TranslateAnimation translateAnim = new TranslateAnimation( 0, 0 , 0, -380 );
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

                        translateAnim.setAnimationListener(new Animation.AnimationListener(){
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {

                                RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                                layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                                layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                linearFAB.setLayoutParams(layoutCompress);
                                linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                                answerVideoCall();
                            }
                        });
                    }
                    public void onSwipeRight() {}
                    public void onSwipeLeft() {}
                    public void onSwipeBottom() {}

                });


            }else{

                answerCallFAB.startAnimation(shake);

                RelativeLayout.LayoutParams layoutExtend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                layoutExtend.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                layoutExtend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                linearFAB.setLayoutParams(layoutExtend);
                linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                linearArrowCall.setVisibility(View.VISIBLE);
                animationAlphaArrows(fourArrowCall);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        animationAlphaArrows(thirdArrowCall);
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            public void run() {
                                animationAlphaArrows(secondArrowCall);
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
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
                        log("onSwipeTop");
                        answerCallFAB.clearAnimation();

                        TranslateAnimation translateAnim = new TranslateAnimation( 0, 0 , 0, -380 );
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

                        translateAnim.setAnimationListener(new Animation.AnimationListener(){
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {

                                RelativeLayout.LayoutParams layoutCompress = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                                layoutCompress.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                                layoutCompress.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                linearFAB.setLayoutParams(layoutCompress);
                                linearFAB.setOrientation(LinearLayout.HORIZONTAL);

                                answerAudioCall();
                            }
                        });
                    }
                    public void onSwipeRight() {}
                    public void onSwipeLeft() {}
                    public void onSwipeBottom() {}

                });

            }

        }else{
            relativeVideo.setVisibility(View.VISIBLE);
            videoFAB.show();
            videoFAB.setVisibility(View.VISIBLE);

            microFAB.show();
            microFAB.setVisibility(View.VISIBLE);

            relativeCall.setVisibility(GONE);
            answerCallFAB.setVisibility(GONE);

            hangFAB.show();
            hangFAB.setVisibility(View.VISIBLE);
        }
    }

    public void updateLocalVideoStatus(){
        log("updateLocalVideoStatus");
        int callStatus = callChat.getStatus();

        if(chat.isGroup()){
            log("is group");
            if(callChat !=null){
                if (callChat.hasLocalVideo()) {
                    log("Video local connected");
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                    videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        InfoPeerGroupCall item = peersOnCall.get(peersOnCall.size()-1);
                        if(!item.isVideoOn()){
                            log("activate Local Video for "+peersOnCall.get(peersOnCall.size()-1).getName());
                            item.setVideoOn(true);
                            adapter.notifyItemChanged(peersOnCall.size()-1);
                        }
//                        if(peerSelected!=null){
//                            log("updateLocalVideoStatus()-> peerSelected.getHandle(): "+peerSelected.getHandle()+", peersOnCall.get(peersOnCall.size()-1).getHandle(): "+peersOnCall.get(peersOnCall.size()-1).getHandle());
//                            if(peerSelected.getHandle().equals(peersOnCall.get(peersOnCall.size()-1).getHandle())){
//                                log("updateLocalVideoStatus()-> createBigFragment");
//                                createBigFragment(peerSelected.getHandle());
//                            }
//                        }

                    }
                }else {
                    log("Video local NOT connected");
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        InfoPeerGroupCall item = peersOnCall.get(peersOnCall.size()-1);
                        if(item.isVideoOn()){
                            log("remove Local Video fot "+peersOnCall.get(peersOnCall.size()-1).getName());
                            item.setVideoOn(false);
                            adapter.notifyItemChanged(peersOnCall.size()-1);
                        }
//                        if(peerSelected != null){
//                            log("updateLocalVideoStatus()-> peerSelected.getHandle(): "+peerSelected.getHandle()+", peersOnCall.get(peersOnCall.size()-1).getHandle(): "+peersOnCall.get(peersOnCall.size()-1).getHandle());
//                            if(peerSelected.getHandle().equals(peersOnCall.get(peersOnCall.size()-1).getHandle())){
//                                log("updateLocalVideoStatus()-> removeBigFragment");
//                                removeBigFragment(peerSelected.getHandle(), peerSelected.getName());
//                            }
//                        }

                    }
                }
            }
        }else{
            log("is individual");

            if (callChat.hasLocalVideo()) {
                log("Video local connected");
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));

                if(callStatus==MegaChatCall.CALL_STATUS_REQUEST_SENT){
                    log("callStatus: CALL_STATUS_REQUEST_SENT");

                    if(localCameraFragmentFS == null){
                        log("CREATE localCameraFragmentFS");
                        localCameraFragmentFS = LocalCameraCallFullScreenFragment.newInstance(chatId);
                        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                        ftFS.replace(R.id.fragment_container_local_cameraFS, localCameraFragmentFS, "localCameraFragmentFS");
                        ftFS.commitNowAllowingStateLoss();
                    }
                    contactAvatarLayout.setVisibility(GONE);
                    parentLocalFS.setVisibility(View.VISIBLE);
                    fragmentContainerLocalCameraFS.setVisibility(View.VISIBLE);

                }else if(callStatus==MegaChatCall.CALL_STATUS_IN_PROGRESS){
                    log("callStatus: CALL_STATUS_IN_PROGRESS");

                    if(localCameraFragment == null){
                        log("CREATE localCameraFragment");
                        localCameraFragment = LocalCameraCallFragment.newInstance(chatId);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment_container_local_camera, localCameraFragment, "localCameraFragment");
                        ft.commitNowAllowingStateLoss();
                    }
                    myAvatarLayout.setVisibility(GONE);
                    parentLocal.setVisibility(View.VISIBLE);
                    fragmentContainerLocalCamera.setVisibility(View.VISIBLE);
                }

            }else {
                log("Video local NOT connected");
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));

                if(callStatus==MegaChatCall.CALL_STATUS_REQUEST_SENT){
                    log("callStatus: CALL_STATUS_REQUEST_SENT");

                    if (localCameraFragmentFS != null) {
                        log("REMOVE localCameraFragmentFS");
                        localCameraFragmentFS.setVideoFrame(false);
                        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                        ftFS.remove(localCameraFragmentFS);
                        localCameraFragmentFS = null;
                    }
                    parentLocalFS.setVisibility(View.GONE);
                    fragmentContainerLocalCameraFS.setVisibility(View.GONE);
                    contactAvatarLayout.setVisibility(View.VISIBLE);


                }else if(callStatus==MegaChatCall.CALL_STATUS_IN_PROGRESS){
                    log("callStatus: CALL_STATUS_IN_PROGRESS");
                    if (localCameraFragment != null) {
                        log("REMOVE localCameraFragment");
                        localCameraFragment.setVideoFrame(false);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.remove(localCameraFragment);
                        localCameraFragment = null;
                    }
                    parentLocal.setVisibility(View.GONE);
                    fragmentContainerLocalCamera.setVisibility(View.GONE);
                    myAvatarLayout.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void updateLocalAudioStatus(){
        log("updateLocalAudioStatus");
        if(chat.isGroup()) {
            log("is group");
            if (callChat != null) {
                if(callChat.hasLocalAudio()){
                    log("Audio local connected");
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        InfoPeerGroupCall item = peersOnCall.get(peersOnCall.size()-1);
                        if(!item.isAudioOn()){
                            item.setAudioOn(true);
                            int position = peersOnCall.size() -1;
                            adapter.changesInAudio(position,null);

                        }
                    }
                }else{
                    log("Audio local NOT connected");
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_mic_off));
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        InfoPeerGroupCall item = peersOnCall.get(peersOnCall.size()-1);
                        if(item.isAudioOn()){
                            item.setAudioOn(false);
                            int position = peersOnCall.size() -1;
                            adapter.changesInAudio(position,null);
                        }
                    }

                }
            }

        }else {
            log("is individual");
            if(callChat.hasLocalAudio()){
                log("Audio local connected");
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
            }else{
                log("Audio local NOT connected");
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_mic_off));
            }
        }

    }

    public void updateRemoteVideoStatus(long userHandle){
        log("updateRemoteVideoStatus");
        if(chat.isGroup()){
            MegaChatSession userSession = callChat.getMegaChatSession(userHandle);
            if(userSession!=null && userSession.hasVideo()) {
                log(userHandle+": Video remote connected");
                if (peersOnCall != null && !peersOnCall.isEmpty()) {
                    for(int i=0;i<peersOnCall.size();i++){
                        if(peersOnCall.get(i).getHandle().equals(userHandle)){
                            if(!peersOnCall.get(i).isVideoOn()){
                                log("activate Remote Video for "+peersOnCall.get(i).getName()+" camera TRUE");
                                peersOnCall.get(i).setVideoOn(true);
                                adapter.notifyItemChanged(i);
                                break;
                            }
//                            if(peerSelected!=null){
//                                log("updateRemoteVideoStatus()-> peerSelected.getHandle(): "+peerSelected.getHandle()+", userHandle: "+userHandle);
//                                if(peerSelected.getHandle().equals(userHandle)){
//                                    log("updateRemoteVideoStatus()-> createBigFragment");
//                                    createBigFragment(peerSelected.getHandle());
//                                }
//                            }
//                            break;
                        }
                    }
                }
            }else{
                log(userHandle+": Video remote NOT connected");
                if (peersOnCall != null && !peersOnCall.isEmpty()) {
                    for(int i=0;i<peersOnCall.size();i++){
                        if(peersOnCall.get(i).getHandle().equals(userHandle)){
                            if(peersOnCall.get(i).isVideoOn()){
                                log("remove Remote Video for "+peersOnCall.get(i).getName()+" camera FALSE");
                                peersOnCall.get(i).setVideoOn(false);
                                adapter.notifyItemChanged(i);
                                break;
                            }
//                            if(peerSelected != null){
//                                log("updateRemoteVideoStatus()-> peerSelected.getHandle(): "+peerSelected.getHandle()+", userHandle: "+userHandle);
//                                if(peerSelected.getHandle().equals(userHandle)){
//                                    log("updateRemoteVideoStatus()-> removeBigFragment");
//                                    removeBigFragment(peerSelected.getHandle(), peerSelected.getName());
//                                }
//                            }
//                            break;
                        }
                    }
                }
            }
        }else{
            log("is individual");
            MegaChatSession userSession = callChat.getMegaChatSession(chat.getPeerHandle(0));

            if(isRemoteVideo== REMOTE_VIDEO_NOT_INIT){

                if(userSession!=null && userSession.hasVideo()){
                    log("Video remote connected");
                    isRemoteVideo = REMOTE_VIDEO_ENABLED;

                    if(remoteCameraFragmentFS == null){
                        log("CREATE remoteCameraFragmentFS");
                        remoteCameraFragmentFS = RemoteCameraCallFullScreenFragment.newInstance(chatId, chat.getPeerHandle(0));
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.replace(R.id.fragment_container_remote_cameraFS, remoteCameraFragmentFS, "remoteCameraFragmentFS");
                        ft.commitNowAllowingStateLoss();
                    }
                    contactAvatarLayout.setOnClickListener(null);
                    contactAvatarLayout.setVisibility(GONE);
                    parentRemoteFS.setVisibility(View.VISIBLE);
                    fragmentContainerRemoteCameraFS.setVisibility(View.VISIBLE);

                }else{
                    log("Video remote NOT connected");

                    isRemoteVideo = REMOTE_VIDEO_DISABLED;
                    if (remoteCameraFragmentFS != null) {
                        log("REMOVE remoteCameraFragmentFS");
                        remoteCameraFragmentFS.setVideoFrame(false);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.remove(remoteCameraFragmentFS);
                        remoteCameraFragmentFS = null;
                    }
                    contactAvatarLayout.setVisibility(View.VISIBLE);
                    contactAvatarLayout.setOnClickListener(this);
                    parentRemoteFS.setVisibility(View.GONE);
                    fragmentContainerRemoteCameraFS.setVisibility(View.GONE);
                }
            }else{
                if((isRemoteVideo==REMOTE_VIDEO_ENABLED)&&(!userSession.hasVideo())){
                    isRemoteVideo = REMOTE_VIDEO_DISABLED;

                    if (remoteCameraFragmentFS != null) {
                        log("REMOVE remoteCameraFragmentFS");
                        remoteCameraFragmentFS.setVideoFrame(false);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.remove(remoteCameraFragmentFS);
                        remoteCameraFragmentFS = null;
                    }
                    contactAvatarLayout.setVisibility(View.VISIBLE);
                    contactAvatarLayout.setOnClickListener(this);
                    parentRemoteFS.setVisibility(View.GONE);
                    fragmentContainerRemoteCameraFS.setVisibility(View.GONE);

                }else if((isRemoteVideo==REMOTE_VIDEO_DISABLED)&&(userSession.hasVideo())){
                    isRemoteVideo = REMOTE_VIDEO_ENABLED;

                        if(remoteCameraFragmentFS == null){
                            log("CREATE remoteCameraFragmentFS");
                            remoteCameraFragmentFS = RemoteCameraCallFullScreenFragment.newInstance(chatId, chat.getPeerHandle(0));
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

    public void updateRemoteAudioStatus(long userHandle){
        log("updateRemoteAudioStatus");
        if(chat.isGroup()){
            if(chat.isGroup()){
                MegaChatSession userSession = callChat.getMegaChatSession(userHandle);
                if(userSession!=null && userSession.hasAudio()) {
                    log(userHandle+": Audio remote connected");
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        for(int i=0;i<peersOnCall.size();i++){
                            if(peersOnCall.get(i).getHandle().equals(userHandle)){
                                if(!peersOnCall.get(i).isAudioOn()){
                                    log("remove Remote Audio for "+peersOnCall.get(i).getName()+" micro TRUE");
                                    peersOnCall.get(i).setAudioOn(true);
                                    adapter.changesInAudio(i,null);
                                    break;
                                }
                            }
                        }
                    }
                }else {
                    log(userHandle+": Audio remote NOT connected");
                    if (peersOnCall != null && !peersOnCall.isEmpty()) {
                        for(int i=0;i<peersOnCall.size();i++){
                            if(peersOnCall.get(i).getHandle().equals(userHandle)){
                                if(peersOnCall.get(i).isAudioOn()){
                                    log("remove Remote Audio for "+peersOnCall.get(i).getName()+" micro FALSE");
                                    peersOnCall.get(i).setAudioOn(false);
                                    adapter.changesInAudio(i,null);
                                    break;
                                }
                            }
                        }
                    }

                }
            }
        }else{
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult() ");

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_CAMERA: {
                log("REQUEST_CAMERA");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkPermissions()){
                       showInitialFABConfiguration();
                    }
                }
                else{
                    hangFAB.setVisibility(View.VISIBLE);
                }
                break;
            }
            case Constants.RECORD_AUDIO: {
                log("RECORD_AUDIO");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkPermissions()){
                        showInitialFABConfiguration();
                    }
                }
                else{
                    hangFAB.setVisibility(View.VISIBLE);
                }
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
            if(!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        } else {
            //Turn on Screen
            if(wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    public void remoteCameraClick(){
        log("remoteCameraClick");
        if(callChat.getStatus()==MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            if (aB.isShowing()) {
                hideActionBar();
                hideFABs();
            } else {
                showActionBar();
                showInitialFABConfiguration();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public static String getDateFromMillis(long d) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(d);
    }

    private void startClock(){
        log("startClock");
        long seconds = 0;

        if(callChat!=null){
            seconds = callChat.getDuration();
        }
        long baseTime = SystemClock.uptimeMillis() - (seconds*1000);
        myChrono.setBase(baseTime);
        customHandler.postDelayed(updateTimerThread, 1000);
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            long elapsedTime = SystemClock.uptimeMillis() - myChrono.getBase();
            aB.setSubtitle(getDateFromMillis(elapsedTime));
            customHandler.postDelayed(this, 0);
        }
    };

    private class MyRingerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    if (ringtone != null && !ringtone.isPlaying())
                    {
                        ringtone.play();
                    }
                }});
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {

            case KeyEvent.KEYCODE_VOLUME_UP: {
                try {
                    if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    } else {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    }
                }
                catch(SecurityException e) {
                    return super.onKeyDown(keyCode, event);
                }

                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                try {
                    if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    } else {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    }
                } catch(SecurityException e) {
                    return super.onKeyDown(keyCode, event);
                }

                return true;
            }
            default: {
                return super.onKeyDown(keyCode, event);
            }
        }
    }

    public void answerAudioCall(){
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
        megaChatApi.answerChatCall(chatId, false, this);
    }
    public void answerVideoCall(){
        log("answerVideoCall");
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
        megaChatApi.answerChatCall(chatId, true, this);
    }

    public void animationAlphaArrows(final ImageView arrow){
        AlphaAnimation alphaAnimArrows = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimArrows.setDuration(alphaAnimationDurationArrow);
        alphaAnimArrows.setFillAfter(true);
        alphaAnimArrows.setFillBefore(true);
        alphaAnimArrows.setRepeatCount(Animation.INFINITE);
        arrow.startAnimation(alphaAnimArrows);
    }

    public void createNewAdapter(boolean flag){
        log("createNewAdapter()");

        if(flag){

            //arrayList-> peersOnCall
            if(peersOnCall.size() < 7){

                bigRecyclerView.setAdapter(null);
                bigRecyclerView.setVisibility(GONE);
//                parentBigCameraGroupCall.setOnClickListener(null);
//                parentBigCameraGroupCall.setVisibility(View.GONE);

                if(peersOnCall.size() < 4){
                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    recyclerView.setLayoutParams(params);

                }else if(peersOnCall.size()==4){
                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = Util.scaleWidthPx(360, outMetrics);
                    recyclerView.setLayoutParams(params);

                }else{
                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = Util.scaleWidthPx(540, outMetrics);
                    recyclerView.setLayoutParams(params);
                }

                if(peersOnCall.size() <= 3){
                    recyclerView.setColumnWidth((int) widthScreenPX);
                }else if((peersOnCall.size() > 3)&&(peersOnCall.size() <= 6)){
                    recyclerView.setColumnWidth((int) widthScreenPX/2);
                }

                recyclerView.setAdapter(null);
//                adapter = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, flag,null);
                adapter = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, flag);
                recyclerView.setAdapter(adapter);
                if (adapter.getItemCount() == 0){
                    recyclerView.setVisibility(View.GONE);
                }else{
                    recyclerView.setVisibility(View.VISIBLE);
                }

            }else{
                recyclerView.setAdapter(null);
                recyclerView.setVisibility(GONE);
//                parentBigCameraGroupCall.setOnClickListener(this);
//                parentBigCameraGroupCall.setVisibility(View.VISIBLE);
//                adapter = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, flag,null);
                bigRecyclerView.setAdapter(null);

                adapter = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, flag);

//                bigRecyclerView.setAdapter(null);
//                adapter = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, flag, new CustomItemClickListener(){
//                    @Override-̣
//                    public void onItemClick(int position) {
                        // do what ever you want to do with it
//                        peerSelected = adapter.getNodeAt(position);
//                        if(peerSelected.getHandle().equals(megaChatApi.getMyUserHandle())){
//                            updateLocalVideoStatus();
//                        }else{
//                            updateRemoteVideoStatus(peerSelected.getHandle());
//
//                        }
//
//                    }
//                });
                bigRecyclerView.setAdapter(adapter);

                if (adapter.getItemCount() == 0){
                    bigRecyclerView.setVisibility(View.GONE);
                }else{
                    bigRecyclerView.setVisibility(View.VISIBLE);
                }
            }

        }else{

//            parentBigCameraGroupCall.setVisibility(View.GONE);

            //arrayList-> peersBeforeCall
            if(peersBeforeCall.size() < 7) {

                bigRecyclerView.setAdapter(null);
                bigRecyclerView.setVisibility(GONE);
//                parentBigCameraGroupCall.setOnClickListener(null);
//                parentBigCameraGroupCall.setVisibility(View.GONE);

//                if(peersBeforeCall.size() <= 4){
//                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
//                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
//                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
//                    recyclerView.setLayoutParams(params);
//                }else{
//                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
//                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
//                    params.height = Util.scaleWidthPx(540, outMetrics);
//                    recyclerView.setLayoutParams(params);
//                }

                if(peersBeforeCall.size() < 4){
                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    recyclerView.setLayoutParams(params);

                }else if(peersBeforeCall.size()==4){
                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = Util.scaleWidthPx(360, outMetrics);
                    recyclerView.setLayoutParams(params);

                }else{
                    ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    params.height = Util.scaleWidthPx(540, outMetrics);
                    recyclerView.setLayoutParams(params);
                }

                if(peersBeforeCall.size() <= 3){
                    recyclerView.setColumnWidth((int) widthScreenPX);
                }else if((peersBeforeCall.size() > 3)&&(peersBeforeCall.size() <= 6)){
                    recyclerView.setColumnWidth((int) widthScreenPX/2);

                }

                recyclerView.setAdapter(null);
//                adapter = new GroupCallAdapter(this, recyclerView, peersBeforeCall, chatId, flag, null);
                adapter = new GroupCallAdapter(this, recyclerView, peersBeforeCall, chatId, flag);

                recyclerView.setAdapter(adapter);
                if (adapter.getItemCount() == 0){
                    recyclerView.setVisibility(View.GONE);
                }else{
                    recyclerView.setVisibility(View.VISIBLE);
                }

            }else{
                recyclerView.setAdapter(null);
                recyclerView.setVisibility(GONE);
//                parentBigCameraGroupCall.setOnClickListener(null);
//                parentBigCameraGroupCall.setVisibility(View.GONE);

                bigRecyclerView.setAdapter(null);
//                adapter = new GroupCallAdapter(this, bigRecyclerView, peersBeforeCall, chatId, flag, null);
                adapter = new GroupCallAdapter(this, bigRecyclerView, peersBeforeCall, chatId, flag);

                bigRecyclerView.setAdapter(adapter);

                if (adapter.getItemCount() == 0){
                    bigRecyclerView.setVisibility(View.GONE);
                }else{
                    bigRecyclerView.setVisibility(View.VISIBLE);
                }
            }

        }

    }

//    public void createBigFragment(Long handle){
//        log("createBigFragment()");
//        if(bigCameraGroupCallFragment != null){
//            log("REMOVE bigCameraGroupCallFragment");
//            bigCameraGroupCallFragment.setVideoFrame(false);
//            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
//            ftFS.remove(bigCameraGroupCallFragment);
//            bigCameraGroupCallFragment = null;
//        }
//
//        //Camera
//        if(bigCameraGroupCallFragment == null){
//            log("CREATE bigCameraGroupCallFragment");
//            bigCameraGroupCallFragment = BigCameraGroupCallFragment.newInstance(chatId, handle);
//            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
//            ftFS.replace(R.id.fragment_container_big_camera_group_call, bigCameraGroupCallFragment, "bigCameraGroupCallFragment");
//            ftFS.commitNowAllowingStateLoss();
//        }
//        fragmentContainerBigCameraGroupCall.setVisibility(View.VISIBLE);
//
//        //Avatar
//        bigAvatarGroupCallLayout.setVisibility(View.GONE);
//
//        parentBigCameraGroupCall.setVisibility(View.VISIBLE);
//
//    }

//    public void removeBigFragment(Long handle, String fullName){
//
//        //Camera
//        if (bigCameraGroupCallFragment != null) {
//            log("REMOVE bigCameraGroupCallFragment");
//            bigCameraGroupCallFragment.setVideoFrame(false);
//            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
//            ftFS.remove(bigCameraGroupCallFragment);
//            bigCameraGroupCallFragment = null;
//        }
//        fragmentContainerBigCameraGroupCall.setVisibility(View.GONE);
//
//        //Avatar
//        if(handle.equals(megaChatApi.getMyUserHandle())){
//            setProfileMyAvatarGroupCall();
//        }else{
//            setProfileContactAvatarGroupCall(handle,fullName);
//        }
//        bigAvatarGroupCallLayout.setVisibility(View.VISIBLE);
//
//        parentBigCameraGroupCall.setVisibility(View.VISIBLE);
//    }
//
//    public void setProfileMyAvatarGroupCall() {
//        log("setProfileMyAvatarGroupCall");
//        Bitmap myBitmap = null;
//        File avatar = null;
//        if (context != null) {
//            log("context is not null");
//            if (context.getExternalCacheDir() != null) {
//                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), megaChatApi.getMyEmail() + ".jpg");
//            } else {
//                avatar = new File(context.getCacheDir().getAbsolutePath(), megaChatApi.getMyEmail() + ".jpg");
//            }
//        }
//        if (avatar.exists()) {
//            if (avatar.length() > 0) {
//                BitmapFactory.Options bOpts = new BitmapFactory.Options();
//                bOpts.inPurgeable = true;
//                bOpts.inInputShareable = true;
//                myBitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
//                myBitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, myBitmap, 3);
//                if (myBitmap != null) {
//                    bigAvatarGroupCallImage.setImageBitmap(myBitmap);
//                    bigAvatarGroupCallInitialLetter.setVisibility(GONE);
//                }else{
//                    createMyDefaultAvatarGroupCall();
//                }
//            }else {
//                createMyDefaultAvatarGroupCall();
//            }
//        }else {
//            createMyDefaultAvatarGroupCall();
//        }
//    }

//    public void createMyDefaultAvatarGroupCall() {
//        log("createMyDefaultAvatarGroupCall");
//        String myFullName = megaChatApi.getMyFullname();
//        String myFirstLetter = myFullName.charAt(0) + "";
//        myFirstLetter = myFirstLetter.toUpperCase(Locale.getDefault());
//        long userHandle = megaChatApi.getMyUserHandle();
//
//        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(defaultAvatar);
//        Paint p = new Paint();
//        p.setAntiAlias(true);
//        p.setColor(Color.TRANSPARENT);
//
//        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(userHandle));
//        if (color != null) {
//            p.setColor(Color.parseColor(color));
//        }else {
//            p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
//        }
//
//        int radius;
//        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
//            radius = defaultAvatar.getWidth() / 2;
//        }else {
//            radius = defaultAvatar.getHeight() / 2;
//        }
//        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
//        bigAvatarGroupCallImage.setImageBitmap(defaultAvatar);
//        bigAvatarGroupCallInitialLetter.setText(myFirstLetter);
//        bigAvatarGroupCallInitialLetter.setVisibility(View.VISIBLE);
//    }

//    public void setProfileContactAvatarGroupCall(long userHandle,  String fullName){
//        log("setProfileContactAvatarGroupCall");
//        Bitmap bitmap = null;
//        File avatar = null;
//        String contactMail = megaChatApi.getContactEmail(userHandle);
//        if (context.getExternalCacheDir() != null) {
//            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), contactMail + ".jpg");
//        } else {
//            avatar = new File(context.getCacheDir().getAbsolutePath(), contactMail + ".jpg");
//        }
//
//        if (avatar.exists()) {
//            if (avatar.length() > 0) {
//                BitmapFactory.Options bOpts = new BitmapFactory.Options();
//                bOpts.inPurgeable = true;
//                bOpts.inInputShareable = true;
//                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
//                bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, bitmap, 3);
//                if (bitmap != null) {
//                    bigAvatarGroupCallImage.setVisibility(View.VISIBLE);
//                    bigAvatarGroupCallImage.setImageBitmap(bitmap);
//                    bigAvatarGroupCallInitialLetter.setVisibility(GONE);
//                }else{
//                    UserAvatarListener listener = new UserAvatarListener(context);
//                    avatar.delete();
//                    if(!avatarRequested){
//                        avatarRequested = true;
//                        if (context.getExternalCacheDir() != null){
//                            megaApi.getUserAvatar(contactMail, context.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
//                        }
//                        else{
//                            megaApi.getUserAvatar(contactMail, context.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
//                        }
//                    }
//
//                    createDefaultAvatarGroupCall(userHandle, fullName);
//                }
//            }else{
//                UserAvatarListener listener = new UserAvatarListener(context);
//
//                if(!avatarRequested){
//                    avatarRequested = true;
//                    if (context.getExternalCacheDir() != null){
//                        megaApi.getUserAvatar(contactMail, context.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
//                    }
//                    else{
//                        megaApi.getUserAvatar(contactMail, context.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
//                    }
//                }
//
//                createDefaultAvatarGroupCall(userHandle, fullName);
//            }
//        }else{
//            UserAvatarListener listener = new UserAvatarListener(context);
//
//            if(!avatarRequested){
//                avatarRequested = true;
//                if (context.getExternalCacheDir() != null){
//                    megaApi.getUserAvatar(contactMail, context.getExternalCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
//                }
//                else{
//                    megaApi.getUserAvatar(contactMail, context.getCacheDir().getAbsolutePath() + "/" + contactMail + ".jpg", listener);
//                }
//            }
//            createDefaultAvatarGroupCall(userHandle, fullName);
//        }
//    }

//    public void createDefaultAvatarGroupCall(long userHandle,  String fullName) {
//        log("createDefaultAvatarGroupCall");
//        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(defaultAvatar);
//        Paint p = new Paint();
//        p.setAntiAlias(true);
//        p.setColor(Color.TRANSPARENT);
//
//        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(userHandle));
//        if (color != null) {
//            p.setColor(Color.parseColor(color));
//        }else{
//            p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
//        }
//
//        int radius;
//        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
//            radius = defaultAvatar.getWidth() / 2;
//        }else {
//            radius = defaultAvatar.getHeight() / 2;
//        }
//        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
//        bigAvatarGroupCallImage.setImageBitmap(defaultAvatar);
//        String contactFirstLetter = fullName.charAt(0) + "";
//        contactFirstLetter = contactFirstLetter.toUpperCase(Locale.getDefault());
//        bigAvatarGroupCallInitialLetter.setText(contactFirstLetter);
//        bigAvatarGroupCallInitialLetter.setVisibility(View.VISIBLE);
//    }


}
