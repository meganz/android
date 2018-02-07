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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.OnSwipeTouchListener;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
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
import nz.mega.sdk.MegaChatVideoListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static android.view.View.GONE;
import static mega.privacy.android.app.utils.Util.context;

public class ChatCallActivity extends AppCompatActivity implements MegaChatRequestListenerInterface,View.OnTouchListener, MegaChatCallListenerInterface, MegaChatVideoListenerInterface, MegaRequestListenerInterface, View.OnClickListener, SensorEventListener, KeyEvent.Callback {

    DatabaseHandler dbH = null;
    ChatItemPreferences chatPrefs = null;
    MegaUser myUser;

    public static int REMOTE_VIDEO_NOT_INIT = -1;
    public static int REMOTE_VIDEO_ENABLED = 1;
    public static int REMOTE_VIDEO_DISABLED = 0;


    private LocalCameraCallFragment localCameraFragment;
    private LocalCameraCallFullScreenFragment localCameraFragmentFS = null;

    float widthScreenPX, heightScreenPX;

    ViewGroup parent;
    ViewGroup parentFS;

    long chatId;
    long callId;
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

//    LocalVideoDataListener localVideoListener;

    static ChatCallActivity chatCallActivityActivity = null;

    private MenuItem remoteAudioIcon;

    FloatingActionButton videoFAB;
    FloatingActionButton microFAB;
    FloatingActionButton hangFAB;
    FloatingActionButton answerCallFAB;

    SurfaceView remoteSurfaceView;
    MegaSurfaceRenderer remoteRenderer;
    AudioManager audioManager;
    MediaPlayer thePlayer;

    FrameLayout fragmentContainerLocalCamera;
    FrameLayout fragmentContainerLocalCameraFS;

    float heightFAB;

    String fullName = "";
    String email = "";
    long userHandle = -1;

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
        if(callChat!=null){
            if(callChat.hasRemoteAudio()){
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
                megaChatApi.hangChatCall(chatId, null);
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
        return super.onOptionsItemSelected(item);
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

        remoteSurfaceView = (SurfaceView)findViewById(R.id.surface_remote_video);
        remoteRenderer = new MegaSurfaceRenderer(remoteSurfaceView);
        rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());

        parent = (ViewGroup) findViewById(R.id.parentLayout);
        fragmentContainerLocalCamera = (FrameLayout) findViewById(R.id.fragment_container_local_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)fragmentContainerLocalCamera.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        fragmentContainerLocalCamera.setLayoutParams(params);
        fragmentContainerLocalCamera.setOnTouchListener(new OnDragTouchListener(fragmentContainerLocalCamera,parent));
        parent.setVisibility(View.GONE);
        fragmentContainerLocalCamera.setVisibility(View.GONE);

        parentFS = (ViewGroup) findViewById(R.id.parentLayoutFS);
        fragmentContainerLocalCameraFS = (FrameLayout) findViewById(R.id.fragment_container_local_cameraFS);
        RelativeLayout.LayoutParams paramsFS = (RelativeLayout.LayoutParams)fragmentContainerLocalCameraFS.getLayoutParams();
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,RelativeLayout.TRUE);
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerLocalCameraFS.setLayoutParams(paramsFS);

        parentFS.setVisibility(View.GONE);
        fragmentContainerLocalCameraFS.setVisibility(View.GONE);


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

            //RelativeLayout.LayoutParams myAvatarLayoutParams= new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            //myAvatarLayoutParams.setMargins(0,0,Util.scaleHeightPx(20, outMetrics),Util.scaleHeightPx(110, outMetrics));
//            myAvatarLayoutParams.addRule(RelativeLayout.ABOVE, R.id.linear_buttons);
           // myAvatarLayout.setLayoutParams(myAvatarLayoutParams);

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

                Intent intentService = new Intent(this, CallService.class);
                intentService.putExtra("chatHandle", callChat.getChatid());
                this.startService(intentService);

                audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                int callStatus = callChat.getStatus();
                log("The status of the callChat is: " + callStatus);
                fullName = chat.getPeerFullname(0);
                email = chat.getPeerEmail(0);
                userHandle = chat.getPeerHandle(0);

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

                if(callStatus==MegaChatCall.CALL_STATUS_RING_IN){
                    log("Incoming call");

                    relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.MATCH_PARENT;

                    contactAvatarLayout.setVisibility(View.VISIBLE);
                    setProfileMyAvatar(true);
                    setProfileContactAvatar(userHandle, fullName, false);

                    Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
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
                    relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    setProfileMyAvatar(false);
                    setProfileContactAvatar(userHandle, fullName, true);

                    updateLocalVideoStatus();
                    updateLocalAudioStatus();
                    updateRemoteAudioStatus();
                    updateRemoteVideoStatus();
                }
                else{
                    log("Outgoing call");

                    relativeVideo.getLayoutParams().height= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    relativeVideo.getLayoutParams().width= RelativeLayout.LayoutParams.WRAP_CONTENT;
                    setProfileMyAvatar(false);
                    setProfileContactAvatar(userHandle, fullName, true);

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

    public void createDefaultAvatar(long userHandle,  String fullName, boolean flag) {
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
        if(flag){
            myImage.setImageBitmap(defaultAvatar);
            myInitialLetter.setText(fullName.charAt(0) + "");
            myInitialLetter.setTextSize(40);
            myInitialLetter.setTextColor(Color.WHITE);
            myInitialLetter.setVisibility(View.VISIBLE);
        }else {
            contactImage.setImageBitmap(defaultAvatar);
            contactInitialLetter.setText(fullName.charAt(0) + "");
            contactInitialLetter.setTextSize(60);
            contactInitialLetter.setTextColor(Color.WHITE);
            contactInitialLetter.setVisibility(View.VISIBLE);
        }
    }

    public void setProfileContactAvatar(long userHandle,  String fullName, boolean flag){
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
                    if(flag){
                        myImage.setImageBitmap(bitmap);
                        myInitialLetter.setVisibility(GONE);
                    }else{
                        contactImage.setImageBitmap(bitmap);
                        contactInitialLetter.setVisibility(GONE);
                    }

                }
                else{
                    createDefaultAvatar(userHandle, fullName, flag);
                }
            }
            else{
                createDefaultAvatar(userHandle, fullName, flag);
            }
        }
        else{
            createDefaultAvatar(userHandle, fullName, flag);
        }
    }

    public void createMyDefaultAvatar(boolean flag) {
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
        if(flag){
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

    public void setProfileMyAvatar(boolean flag) {
        log("setProfileMyAvatar: "+flag);
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
                    if(flag){
                        myImage.setImageBitmap(myBitmap);
                        myInitialLetter.setVisibility(GONE);
                    }else{
                        contactImage.setImageBitmap(myBitmap);
                        contactInitialLetter.setVisibility(GONE);
                    }
                }
                else{
                    createMyDefaultAvatar(flag);
                }
            }
            else {
                createMyDefaultAvatar(flag);
            }
        } else {
            createMyDefaultAvatar(flag);
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
        this.width=0;
        this.height=0;
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        MegaApplication.activityResumed();
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
    }


    @Override
    public void onDestroy(){
        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
            megaChatApi.removeChatVideoListener(this);
        }

        if(thePlayer!=null){
            thePlayer.stop();
            thePlayer.release();
            thePlayer=null;
        }

        if (toneGenerator != null) {
            toneGenerator.stopTone();
            toneGenerator.release();
            toneGenerator = null;
        }

        if(ringtone!=null){
            ringtone.stop();
        }

        if (timer!=null){
            timer.cancel();
            timer = null;
        }

        if (ringerTimer != null) {
            ringerTimer.cancel();
            ringerTimer = null;
        }

        if (vibrator != null){
            if (vibrator.hasVibrator()){
                vibrator.cancel();
            }
        }

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
            megaChatApi.removeChatVideoListener(this);
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

        this.callChat = call;
        if(callChat.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)){
            int callStatus = callChat.getStatus();
            switch (callStatus){
                case MegaChatCall.CALL_STATUS_IN_PROGRESS:{

                    videoFAB.setOnClickListener(null);
                    answerCallFAB.setOnTouchListener(null);
                    videoFAB.setOnTouchListener(null);
                    videoFAB.setOnClickListener(this);

                    setProfileMyAvatar(true);
                    setProfileContactAvatar(userHandle, fullName, false);

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

                    if(thePlayer!=null){
                        thePlayer.stop();
                        thePlayer.release();
                        thePlayer=null;
                    }

                    if (toneGenerator != null) {
                        toneGenerator.stopTone();
                        toneGenerator.release();
                        toneGenerator = null;
                    }

                    if (ringtone!=null){
                        ringtone.stop();
                    }

                    if (ringerTimer != null) {
                        ringerTimer.cancel();
                        ringerTimer = null;
                    }

                    if (vibrator != null){
                        if (vibrator.hasVibrator()){
                            vibrator.cancel();
                        }
                    }

                    rtcAudioManager.start(null);

                    showInitialFABConfiguration();
                    startClock();
                    if(callChat.hasRemoteAudio()){
                        log("Remote audio is connected");
                    }
                    else{
                        log("Remote audio is NOT connected");
                    }
                    if(callChat.hasRemoteVideo()){
                        log("Remote video is connected");
                    }
                    else{
                        log("Remote video is NOT connected");
                    }
                    break;
                }
                case MegaChatCall.CALL_STATUS_DESTROYED:{

                    if(thePlayer != null){
                        thePlayer.stop();
                        thePlayer.release();
                        thePlayer=null;
                    }

                    if (toneGenerator != null) {
                        toneGenerator.stopTone();
                        toneGenerator.release();
                        toneGenerator = null;
                    }

                    if(ringtone != null){
                        ringtone.stop();
                    }

                    if (timer != null){
                        timer.cancel();
                        timer = null;
                    }

                    if (ringerTimer != null) {
                        ringerTimer.cancel();
                        ringerTimer = null;
                    }

                    if (vibrator != null){
                        if (vibrator.hasVibrator()) {
                            vibrator.cancel();
                        }
                    }

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
        else if(call.hasChanged(MegaChatCall.CHANGE_TYPE_REMOTE_AVFLAGS)){
            log("Remote flags have changed");
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

    int width = 0;
    int height = 0;
    Bitmap bitmap;

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer)
    {
        if((width == 0) || (height == 0)){
            return;
        }

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            SurfaceHolder holder = remoteSurfaceView.getHolder();
            if (holder != null) {
                int viewWidth = remoteSurfaceView.getWidth();
                int viewHeight = remoteSurfaceView.getHeight();
                if ((viewWidth != 0) && (viewHeight != 0)) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = remoteRenderer.CreateBitmap(width, height);
                    holder.setFixedSize(holderWidth, holderHeight);
                }
            }
        }

        if (bitmap != null) {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));

            // Instead of using this WebRTC renderer, we should probably draw the image by ourselves.
            // The renderer has been modified a bit and an update of WebRTC could break our app
            remoteRenderer.DrawBitmap(false);
        }
    }


    @Override
    public void onClick(View v) {
        log("onClick");
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        switch (v.getId()) {
            case R.id.home: {
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

                //  surfaceView.setVisibility(View.VISIBLE);
//                 start_camera();
                break;
            }
            case R.id.micro_fab: {

                if(callChat.hasLocalAudio()){
                    megaChatApi.disableAudio(chatId, this);
                }
                else{
                    megaChatApi.enableAudio(chatId, this);
                }
                break;
            }
            case R.id.hang_fab: {
                log("Click on hang fab");
                megaChatApi.hangChatCall(chatId, this);
                break;
            }
            case R.id.answer_call_fab:{
                log("Click on answer fab");
                megaChatApi.answerChatCall(chatId, false, this);
                videoFAB.clearAnimation();

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

            if (callChat.hasRemoteVideo()) {

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
                        localCameraFragmentFS = new LocalCameraCallFullScreenFragment();
                        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                        ftFS.replace(R.id.fragment_container_local_cameraFS, localCameraFragmentFS, "localCameraFragmentFS");
                        ftFS.commitNowAllowingStateLoss();
                    }
                    contactAvatarLayout.setVisibility(GONE);
                    parentFS.setVisibility(View.VISIBLE);
                    fragmentContainerLocalCameraFS.setVisibility(View.VISIBLE);

                }else{
                    localCameraFragment = new LocalCameraCallFragment();
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
            if(callChat.hasRemoteVideo()){
                log("Video remote connected");
                isRemoteVideo = REMOTE_VIDEO_ENABLED;
                contactAvatarLayout.setVisibility(View.GONE);
                contactAvatarLayout.setOnTouchListener(null);
                remoteSurfaceView.setOnTouchListener(this);
                log("Register remote video listener");
                megaChatApi.addChatRemoteVideoListener(this);
            }
            else{
                log("Video remote NOT connected");
                isRemoteVideo = REMOTE_VIDEO_DISABLED;
                contactAvatarLayout.setVisibility(View.VISIBLE);
                remoteSurfaceView.setOnTouchListener(null);
                contactAvatarLayout.setOnTouchListener(this);
                megaChatApi.removeChatVideoListener(this);
            }
        }
        else{
            log("Change on remote video");
            if((isRemoteVideo==REMOTE_VIDEO_ENABLED)&&(!callChat.hasRemoteVideo())){
                isRemoteVideo = REMOTE_VIDEO_DISABLED;
                contactAvatarLayout.setVisibility(View.VISIBLE);
                remoteSurfaceView.setOnTouchListener(null);
                contactAvatarLayout.setOnTouchListener(this);
                megaChatApi.removeChatVideoListener(this);
            }
            else if((isRemoteVideo==REMOTE_VIDEO_DISABLED)&&(callChat.hasRemoteVideo())){
                isRemoteVideo = REMOTE_VIDEO_ENABLED;
                contactAvatarLayout.setVisibility(View.GONE);
                contactAvatarLayout.setOnTouchListener(null);
                remoteSurfaceView.setOnTouchListener(this);
                megaChatApi.addChatRemoteVideoListener(this);
            }
        }
    }

    public void updateLocalFullVideoStatus(){
        log("updateLocalFullVideoStatus: ");

        if (localCameraFragmentFS == null) {
            localCameraFragmentFS = new LocalCameraCallFullScreenFragment();
            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
            ftFS.replace(R.id.fragment_container_local_cameraFS, localCameraFragmentFS, "localCameraFragment");
            ftFS.commitNowAllowingStateLoss();
        }else {
            contactAvatarLayout.setVisibility(View.VISIBLE);
            parentFS.setVisibility(View.GONE);
            fragmentContainerLocalCameraFS.setVisibility(View.GONE);

            localCameraFragmentFS.setVideoFrame(false);
            FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
            ftFS.remove(localCameraFragmentFS);
            localCameraFragmentFS = null;
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

    @Override
    public boolean onTouch(View view, MotionEvent event){

            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if((view.getId() == R.id.surface_remote_video) || (view.getId() == R.id.call_chat_contact_image_layout)){
                        if(aB.isShowing()){
                            hideActionBar();
                            hideFABs();
                        }else{
                            showActionBar();
                            showInitialFABConfiguration();
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    break;

                default:
                    return false;
            }
            return true;
    }




    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void startClock(){

        timer = new Timer();
        MyTimerTask myTimerTask = new MyTimerTask();

        timer.schedule(myTimerTask, 0, 1000);
    }

    private class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            milliseconds = milliseconds +1000;
            SimpleDateFormat formatter = new SimpleDateFormat("mm:ss", Locale.getDefault());
            final String strDate = formatter.format(new Date(milliseconds));

            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    aB.setSubtitle(strDate);
                }});
        }
    }

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
