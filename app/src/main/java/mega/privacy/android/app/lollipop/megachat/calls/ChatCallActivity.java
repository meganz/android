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
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
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
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.OnSwipeTouchListener;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
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
import nz.mega.sdk.MegaChatVideoListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.provider.Settings.System.DEFAULT_RINGTONE_URI;
import static android.view.View.GONE;
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

    Timer timer = null;
    Timer ringerTimer = null;
    long milliseconds = 0;

    int isRemoteVideo = REMOTE_VIDEO_NOT_INIT;

    Ringtone ringtone = null;
    Vibrator vibrator = null;
    ToneGenerator toneGenerator = null;

    //my avatar
    RelativeLayout myAvatarLayout;
    RelativeLayout myImageBorder;
    RoundedImageView myImage;
    TextView myInitialLetter;

    //contact avatar
    RelativeLayout contactAvatarLayout;
    RoundedImageView contactImage;
    TextView contactInitialLetter;
    RelativeLayout contactImageBorder;

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

    ViewGroup parent;
    ViewGroup parentFS;
    ViewGroup parentRemoteFS;

    private LocalCameraCallFragment localCameraFragment;
    private LocalCameraCallFullScreenFragment localCameraFragmentFS = null;
    private RemoteCameraCallFullScreenFragment remoteCameraFragmentFS = null;

    float heightFAB;

    String fullName = "";
    String email = "";
    long userHandle = -1;
    MegaChatSession userSession = null;

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

        remoteAudioIcon.setEnabled(false);

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
//        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
//
//        int id = item.getItemId();
//        switch (id) {
//            case android.R.id.home: {
//                log("Hang call");
//                megaChatApi.hangChatCall(chatId, null);
//                MegaApplication.activityPaused();
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                    super.finishAndRemoveTask();
//                }
//                else {
//                    super.finish();
//                }
//                break;
//            }
//        }
        return super.onOptionsItemSelected(item);
    }

    public void updateScreenStatusInProgress(){
        log("updateScreenStatusInProgress");
        relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
        relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
        flagMyAvatar = false;
        setProfileMyAvatar();
        flagContactAvatar = true;
        setProfileContactAvatar();

        stopAudioSignals();

        updateLocalVideoStatus();
        updateLocalAudioStatus();
        updateRemoteAudioStatus();
        updateRemoteVideoStatus();
        startClock();
    }

    public void setCallInfo(){
        log("setCallInfo");

        fullName = chat.getTitle();
        email = chat.getPeerEmail(0);
        userHandle = chat.getPeerHandle(0);
        userSession = callChat.getMegaChatSession(userHandle);

        if (fullName.trim() != null) {
            if (fullName.trim().isEmpty()) {
                log("1 - Put email as fullname");
                String[] splitEmail = email.split("[@._]");
                fullName = splitEmail[0];
            }
        } else {
            log("2 - Put email as fullname");
            String[] splitEmail = email.split("[@._]");
            fullName = splitEmail[0];
        }

        aB.setTitle(fullName);
        updateSubTitle();
    }

    public void updateSubTitle(){
        log("updateSubTitle");
        int sessionStatus = -1;

        if(callChat.getStatus()<=MegaChatCall.CALL_STATUS_RING_IN){
            aB.setSubtitle(getString(R.string.call_starting));
        }
        else if(callChat.getStatus()==MegaChatCall.CALL_STATUS_IN_PROGRESS){
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
        else{
            aB.setSubtitle(null);
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log("onNewIntent");

        Bundle extras = intent.getExtras();
        log(getIntent().getAction());
        if (extras != null) {
            long newChatId = extras.getLong("chatHandle", -1);
            log("New chat id: "+newChatId);
            if(chatId==newChatId){
                log("Its the same call");
            }
            else{
                log("New intent to the activity with a new chatId");
                //Check the new call if in progress
                chatId = newChatId;
                chat = megaChatApi.getChatRoom(chatId);
                callChat = megaChatApi.getChatCall(chatId);

                setCallInfo();
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
        log("onCreate");
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
        log("aB.setHomeAsUpIndicator_1");
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(false);
        aB.setDisplayHomeAsUpEnabled(false);
        aB.setTitle(" ");
        myChrono = new Chronometer(context);

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

        //Local camera small
        parent = (ViewGroup) findViewById(R.id.parent_layout);
        fragmentContainerLocalCamera = (FrameLayout) findViewById(R.id.fragment_container_local_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)fragmentContainerLocalCamera.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        fragmentContainerLocalCamera.setLayoutParams(params);
        fragmentContainerLocalCamera.setOnTouchListener(new OnDragTouchListener(fragmentContainerLocalCamera,parent));
        parent.setVisibility(View.GONE);
        fragmentContainerLocalCamera.setVisibility(View.GONE);

        //Local camera Full Screen
        parentFS = (ViewGroup) findViewById(R.id.parent_layout_local_camera_FS);
        fragmentContainerLocalCameraFS = (FrameLayout) findViewById(R.id.fragment_container_local_cameraFS);
        RelativeLayout.LayoutParams paramsFS = (RelativeLayout.LayoutParams)fragmentContainerLocalCameraFS.getLayoutParams();
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerLocalCameraFS.setLayoutParams(paramsFS);
        parentFS.setVisibility(View.GONE);
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

            myAvatarLayout = (RelativeLayout) findViewById(R.id.call_chat_my_image_layout);
            myAvatarLayout.setVisibility(View.VISIBLE);

            myImage = (RoundedImageView) findViewById(R.id.call_chat_my_image);
            myImageBorder = (RelativeLayout) findViewById(R.id.call_chat_my_image_rl);

            myInitialLetter = (TextView) findViewById(R.id.call_chat_my_image_initial_letter);

            contactAvatarLayout = (RelativeLayout) findViewById(R.id.call_chat_contact_image_layout);
            contactImage = (RoundedImageView) findViewById(R.id.call_chat_contact_image);
            contactInitialLetter = (TextView) findViewById(R.id.call_chat_contact_image_initial_letter);

            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));

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

                setCallInfo();

                if(callStatus==MegaChatCall.CALL_STATUS_RING_IN){
                    log("Incoming call");

                    relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.MATCH_PARENT;

                    contactAvatarLayout.setVisibility(View.VISIBLE);
                    flagMyAvatar = true;
                    setProfileMyAvatar();
                    flagContactAvatar = false;
                    setProfileContactAvatar();

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
                }
                else if(callStatus==MegaChatCall.CALL_STATUS_IN_PROGRESS){
                    updateScreenStatusInProgress();
                }
                else{
                    log("Outgoing call");

                    relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    flagMyAvatar = false;
                    setProfileMyAvatar();
                    flagContactAvatar = true;
                    setProfileContactAvatar();
                    int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (volume == 0) {
                        toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
                        toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE, 60000);
                    }
                    else {
                        thePlayer = MediaPlayer.create(getApplicationContext(), R.raw.outgoing_voice_video_call);
                        thePlayer.setLooping(true);
                        thePlayer.start();
                    }
                    updateLocalVideoStatus();
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

    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish(): "+request.getEmail());
    }


    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {}

    public void createDefaultAvatar(long userHandle,  String fullName) {
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
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

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
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), email + ".jpg");
        } else {
            avatar = new File(context.getCacheDir().getAbsolutePath(), email + ".jpg");
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
            p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
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
        mSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
//        this.width=0;
//        this.height=0;
        super.onResume();
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
        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
//            megaChatApi.removeChatVideoListener(chatId, userHandle, this);
        }

        if (customHandler != null){
            customHandler.removeCallbacksAndMessages(null);
        }

        stopAudioSignals();

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
        log("Type: "+request.getType());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
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
//                        microFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
//                    }
//                }
//                else if(request.getParamType()==MegaChatRequest.VIDEO){
//                    if(request.getFlag()==true){
//                        log("Enable video");
//                        myAvatarLayout.setVisibility(View.VISIBLE);
//                        myImageBorder.setVisibility(GONE);
//                        videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
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

        if(call.getChatid()==chatId){
            log("onChatCallUpdate: "+call.getStatus());
            this.callChat = call;

            if(callChat.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)){
                int callStatus = callChat.getStatus();
                switch (callStatus){
                    case MegaChatCall.CALL_STATUS_IN_PROGRESS:{

                        videoFAB.setOnClickListener(null);
                        answerCallFAB.setOnTouchListener(null);
                        videoFAB.setOnTouchListener(null);
                        videoFAB.setOnClickListener(this);
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
                            parentFS.setVisibility(View.GONE);
                            fragmentContainerLocalCameraFS.setVisibility(View.GONE);
                        }

                        updateLocalVideoStatus();
                        updateRemoteVideoStatus();

                        stopAudioSignals();

                        rtcAudioManager.start(null);

                        showInitialFABConfiguration();
                        updateSubTitle();
                        break;
                    }
                    case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:{
                        log("Terminating call of chat: "+chatId);
                        break;
                    }
                    case MegaChatCall.CALL_STATUS_DESTROYED:{
                        log("CALL_STATUS_DESTROYED:TERM code of the call: "+call.getTermCode());

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
                log("Session status have changed");
                userSession = callChat.getMegaChatSession(userHandle);
                log("Status of the session: "+userSession.getStatus());
                if(call.getPeerSessionStatusChange()==chat.getPeerHandle(0)){
                    updateSubTitle();
                }
                updateRemoteVideoStatus();
                updateRemoteAudioStatus();
            }
            else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_REMOTE_AVFLAGS)){
                log("Remote flags have changed");
                userSession = callChat.getMegaChatSession(userHandle);
                updateRemoteVideoStatus();
                updateRemoteAudioStatus();
            }
            else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)){
                log("Local flags have changed");
                updateLocalAudioStatus();
                updateLocalVideoStatus();
            }
            else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_RINGING_STATUS)){
                log("CHANGE_TYPE_RINGING_STATUS");
            }
            else{
                log("CHANGE_TYPE_RINGING_STATUS: "+call.getChanges());
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

    public Bitmap createDefaultAvatar(){
        log("createDefaultAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint paintText = new Paint();
        Paint paintCircle = new Paint();

        paintText.setColor(Color.WHITE);
        paintText.setTextSize(150);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);
        Typeface face = Typeface.SANS_SERIF;
        paintText.setTypeface(face);
        paintText.setAntiAlias(true);
        paintText.setSubpixelText(true);
        paintText.setStyle(Paint.Style.FILL);

        if(chat.isGroup()){
            paintCircle.setColor(ContextCompat.getColor(context,R.color.divider_upgrade_account));
        }
        else{
            String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(chat.getPeerHandle(0)));
            if(color!=null){
                log("The color to set the avatar is "+color);
                paintCircle.setColor(Color.parseColor(color));
                paintCircle.setAntiAlias(true);
            }
            else{
                log("Default color to the avatar");
                paintCircle.setColor(ContextCompat.getColor(context, R.color.lollipop_primary_color));
                paintCircle.setAntiAlias(true);
            }
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius,paintCircle);

        if(chat.getTitle()!=null){
            if(!chat.getTitle().isEmpty()){
                char title = chat.getTitle().charAt(0);
                String firstLetter = new String(title+"");

                if(!firstLetter.equals("(")){

                    log("Draw letter: "+firstLetter);
                    Rect bounds = new Rect();

                    paintText.getTextBounds(firstLetter,0,firstLetter.length(),bounds);
                    int xPos = (c.getWidth()/2);
                    int yPos = (int)((c.getHeight()/2)-((paintText.descent()+paintText.ascent()/2))+20);
                    c.drawText(firstLetter.toUpperCase(Locale.getDefault()), xPos, yPos, paintText);
                }

            }
        }
        return defaultAvatar;
    }

    int width = 0;
    int height = 0;
    Bitmap bitmap;

    @Override
    public void onClick(View v) {
        log("onClick");

        switch (v.getId()) {
            case R.id.call_chat_contact_image_layout:{
                remoteCameraClick();
                break;
            }
            case R.id.video_fab:{

                if(callChat.getStatus()==MegaChatCall.CALL_STATUS_RING_IN){
                    megaChatApi.answerChatCall(chatId, true, this);
                    answerCallFAB.clearAnimation();
                }
                else{
                    if(callChat.hasLocalVideo()){
                        megaChatApi.disableVideo(chatId, this);
                    }
                    else{
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
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));

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
        log("updateLocalVideoStatus: ");
        int callStatus = callChat.getStatus();

        if (callChat.hasLocalVideo()) {

            log("Video local connected");
            if (myAvatarLayout.getVisibility() == View.VISIBLE) {
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));

                if(callStatus==MegaChatCall.CALL_STATUS_REQUEST_SENT){
                    if(localCameraFragmentFS == null){
                        localCameraFragmentFS = LocalCameraCallFullScreenFragment.newInstance(chatId);
                        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                        ftFS.replace(R.id.fragment_container_local_cameraFS, localCameraFragmentFS, "localCameraFragmentFS");
                        ftFS.commitNowAllowingStateLoss();
                    }
                    contactAvatarLayout.setVisibility(GONE);
                    parentFS.setVisibility(View.VISIBLE);
                    fragmentContainerLocalCameraFS.setVisibility(View.VISIBLE);

                }else{
                    localCameraFragment = LocalCameraCallFragment.newInstance(chatId);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container_local_camera, localCameraFragment, "localCameraFragment");
                    ft.commitNowAllowingStateLoss();

                    myAvatarLayout.setVisibility(GONE);
                    parent.setVisibility(View.VISIBLE);
                    fragmentContainerLocalCamera.setVisibility(View.VISIBLE);
                }

            } else {
                log("No needed to refresh");
            }
        } else {
            log("Video local NOT connected");

            if(callStatus==MegaChatCall.CALL_STATUS_REQUEST_SENT){
                parentFS.setVisibility(View.GONE);
                fragmentContainerLocalCameraFS.setVisibility(View.GONE);
                if (localCameraFragmentFS != null) {
                    localCameraFragmentFS.setVideoFrame(false);
                    FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                    ftFS.remove(localCameraFragmentFS);
                    localCameraFragmentFS = null;

                }
                contactAvatarLayout.setVisibility(View.VISIBLE);

            }else{
                parent.setVisibility(View.GONE);
                fragmentContainerLocalCamera.setVisibility(View.GONE);
                if (localCameraFragment != null) {
                    localCameraFragment.setVideoFrame(false);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.remove(localCameraFragment);
                    localCameraFragment = null;
                }
                myAvatarLayout.setVisibility(View.VISIBLE);
            }

            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
        }
    }

    public void updateLocalAudioStatus(){
        log("updateLocalAudioStatus");
        if(callChat.hasLocalAudio()){

            log("Audio local connected");
            microFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            microFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_record_audio_w));

        }else{
            log("Audio local NOT connected");
            microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            microFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_off));
        }
    }

    public void updateRemoteVideoStatus(){
        log("updateRemoteVideoStatus");

        if(isRemoteVideo== REMOTE_VIDEO_NOT_INIT){

            if(userSession!=null && userSession.hasVideo()){
                log("Video remote connected");
                isRemoteVideo = REMOTE_VIDEO_ENABLED;
                if (contactAvatarLayout.getVisibility() == View.VISIBLE) {

                    remoteCameraFragmentFS = RemoteCameraCallFullScreenFragment.newInstance(chatId, userHandle);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container_remote_cameraFS, remoteCameraFragmentFS, "remoteCameraFragmentFS");
                    ft.commitNowAllowingStateLoss();

//                    contactAvatarLayout.setOnTouchListener(null);
                    contactAvatarLayout.setOnClickListener(null);
                    contactAvatarLayout.setVisibility(GONE);
                    parentRemoteFS.setVisibility(View.VISIBLE);
                    fragmentContainerRemoteCameraFS.setVisibility(View.VISIBLE);

                } else {
                    log("No needed to refresh");
                }

            }else{
                log("Video remote NOT connected");

                isRemoteVideo = REMOTE_VIDEO_DISABLED;
                parentRemoteFS.setVisibility(View.GONE);
                fragmentContainerRemoteCameraFS.setVisibility(View.GONE);
                    if (remoteCameraFragmentFS != null) {
                        remoteCameraFragmentFS.setVideoFrame(false);
                        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                        ft.remove(remoteCameraFragmentFS);
                        remoteCameraFragmentFS = null;
                    }
                contactAvatarLayout.setVisibility(View.VISIBLE);
                contactAvatarLayout.setOnClickListener(this);
            }
        }else{
            log("Change on remote video");
            if((isRemoteVideo==REMOTE_VIDEO_ENABLED)&&(!userSession.hasVideo())){

                isRemoteVideo = REMOTE_VIDEO_DISABLED;
                parentRemoteFS.setVisibility(View.GONE);
                fragmentContainerRemoteCameraFS.setVisibility(View.GONE);
                if (remoteCameraFragmentFS != null) {
                    remoteCameraFragmentFS.setVideoFrame(false);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.remove(remoteCameraFragmentFS);
                    remoteCameraFragmentFS = null;
                }
                contactAvatarLayout.setVisibility(View.VISIBLE);
                contactAvatarLayout.setOnClickListener(this);

            }else if((isRemoteVideo==REMOTE_VIDEO_DISABLED)&&(userSession.hasVideo())){

                isRemoteVideo = REMOTE_VIDEO_ENABLED;
                if (contactAvatarLayout.getVisibility() == View.VISIBLE) {
                    remoteCameraFragmentFS = RemoteCameraCallFullScreenFragment.newInstance(chatId, userHandle);
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_container_remote_cameraFS, remoteCameraFragmentFS, "remoteCameraFragmentFS");
                    ft.commitNowAllowingStateLoss();

                    contactAvatarLayout.setOnClickListener(null);
                    contactAvatarLayout.setVisibility(GONE);
                    parentRemoteFS.setVisibility(View.VISIBLE);
                    fragmentContainerRemoteCameraFS.setVisibility(View.VISIBLE);

                } else {
                    log("No needed to refresh");
                }
            }
        }
    }


    public void updateRemoteAudioStatus(){
        log("updateRemoteAudioStatus");
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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

        if(aB.isShowing()){
            hideActionBar();
            hideFABs();
        }else{
            showActionBar();
            showInitialFABConfiguration();
        }


    }

//    @Override
//    public boolean onTouch(View view, MotionEvent event){
//
//            final int X = (int) event.getRawX();
//            final int Y = (int) event.getRawY();
//
//            switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                case MotionEvent.ACTION_DOWN:
//                    if((view.getId() == R.id.parent_layout_remote_camera_FS)||(view.getId() == R.id.call_chat_contact_image_layout)){
//                        if(aB.isShowing()){
//                            hideActionBar();
//                            hideFABs();
//                        }else{
//                            showActionBar();
//                            showInitialFABConfiguration();
//                        }
//                    }
//                    break;
//
//                case MotionEvent.ACTION_MOVE:
//                    break;
//
//                default:
//                    return false;
//            }
//            return true;
//    }




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


//    private void startClock(){
//
//        timer = new Timer();
//        MyTimerTask myTimerTask = new MyTimerTask();
//
//        timer.schedule(myTimerTask, 0, 1000);
//    }
//
//    private class MyTimerTask extends TimerTask {
//
//        @Override
//        public void run() {
//            milliseconds = milliseconds +1000;
//            log("milliseconds: "+milliseconds);
//            SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
//            final String strDate = formatter.format(new Date(milliseconds));
//
//            runOnUiThread(new Runnable(){
//
//                @Override
//                public void run() {
//                    aB.setSubtitle(strDate);
//                }});
//        }
//    }

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
}
