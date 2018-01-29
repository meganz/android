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
import android.widget.FrameLayout;
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

    private LocalCameraCallFragment localCameraFragment;
    boolean flag = true;
    float dX, dY;
    float widthScreenPX, heightScreenPX;

    ViewGroup parent;

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

        parent = (ViewGroup) findViewById(R.id.parentLayout);

        videoFAB = (FloatingActionButton) findViewById(R.id.video_fab);
        videoFAB.setOnClickListener(this);

        microFAB = (FloatingActionButton) findViewById(R.id.micro_fab);
        microFAB.setOnClickListener(this);

        hangFAB = (FloatingActionButton) findViewById(R.id.hang_fab);
        hangFAB.setOnClickListener(this);

        answerCallFAB = (FloatingActionButton) findViewById(R.id.answer_call_fab);
        videoFAB.setVisibility(GONE);
        answerCallFAB.setVisibility(GONE);
        hangFAB.setVisibility(GONE);
        microFAB.setVisibility(GONE);

        remoteSurfaceView = (SurfaceView)findViewById(R.id.surface_remote_video);
//        remoteSurfaceView.setOnTouchListener(this);

        remoteRenderer = new MegaSurfaceRenderer(remoteSurfaceView);
        rtcAudioManager = AppRTCAudioManager.create(getApplicationContext());

        fragmentContainerLocalCamera = (FrameLayout) findViewById(R.id.fragment_container_local_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)fragmentContainerLocalCamera.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        fragmentContainerLocalCamera.setLayoutParams(params);
        fragmentContainerLocalCamera.setOnTouchListener(new OnDragTouchListener(fragmentContainerLocalCamera,parent));
        parent.setVisibility(View.GONE);
        fragmentContainerLocalCamera.setVisibility(View.GONE);

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
            myImage = (RoundedImageView) findViewById(R.id.call_chat_my_image);
            myImageBorder = (RelativeLayout) findViewById(R.id.call_chat_my_image_rl);

            myInitialLetter = (TextView) findViewById(R.id.call_chat_my_image_initial_letter);

            contactAvatarLayout = (RelativeLayout) findViewById(R.id.call_chat_contact_image_layout);
//            contactAvatarLayout.setOnTouchListener(this);
            contactImage = (RoundedImageView) findViewById(R.id.call_chat_contact_image);
            contactInitialLetter = (TextView) findViewById(R.id.call_chat_contact_image_initial_letter);

            setProfileMyAvatar();

            contactAvatarLayout.setVisibility(View.VISIBLE);
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

                audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                int callStatus = callChat.getStatus();
                log("The status of the callChat is: " + callStatus);

                if(callStatus==MegaChatCall.CALL_STATUS_RING_IN){
                    log("Incoming call");
                    Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                    ringerTimer = new Timer();
                    MyRingerTask myRingerTask = new MyRingerTask();
                    ringerTimer.schedule(myRingerTask, 0, 500);

                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    long[] pattern = {0, 1000, 500, 500, 1000};
                    if (vibrator != null){
                        if (vibrator.hasVibrator()){
                            //TODO
                            //FOR API>=26
                            //vibrator.vibrate(createWaveform(pattern, 0), USAGE_NOTIFICATION_RINGTONE); ??
                            vibrator.vibrate(pattern, 0);
                        }
                    }
                }
                else{
                    log("Outgoing call");

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

                setUserProfileAvatar(userHandle, fullName);
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
        contactImage.setImageBitmap(defaultAvatar);
        contactInitialLetter.setText(fullName.charAt(0)+"");
        contactInitialLetter.setTextSize(60);
        contactInitialLetter.setTextColor(Color.WHITE);
        contactInitialLetter.setVisibility(View.VISIBLE);
    }

    public void setUserProfileAvatar(long userHandle,  String fullName){
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
                    contactImage.setImageBitmap(bitmap);
                    contactInitialLetter.setVisibility(GONE);
                }
                else{
                    createDefaultAvatar(userHandle, fullName);
                }
            }
            else{
                createDefaultAvatar(userHandle, fullName);
            }
        }
        else{
            createDefaultAvatar(userHandle, fullName);
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
        myImage.setImageBitmap(defaultAvatar);

        myInitialLetter.setText(myFirstLetter);
        myInitialLetter.setTextSize(40);
        myInitialLetter.setTextColor(Color.WHITE);
        myInitialLetter.setVisibility(View.VISIBLE);
    }

    public void setProfileMyAvatar() {
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

                    myImage.setImageBitmap(myBitmap);
                    myInitialLetter.setVisibility(GONE);

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
//		if (overflowMenuLayout != null){
//			if (overflowMenuLayout.getVisibility() == View.VISIBLE){
//				overflowMenuLayout.setVisibility(View.GONE);
//				return;
//			}
//		}
//        super.onBackPressed();
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
                microFAB.setVisibility(View.VISIBLE);
                answerCallFAB.setVisibility(GONE);
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
            log("The status of the call is: " + callStatus);
            switch (callStatus){
                case MegaChatCall.CALL_STATUS_IN_PROGRESS:{
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

    //  private Bitmap getRoundedCornerBitmap(Bitmap bitmap){

    //     int w = bitmap.getWidth();
    //    int h = bitmap.getHeight();
    //     int radius = 40;

    //   Bitmap output = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    //    Canvas canvas = new Canvas(output);

    //    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    //    final RectF rectF = new RectF(0,0,w,h);

    //    canvas.drawRoundRect(rectF,radius,radius,paint);
    //   paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    //  canvas.drawBitmap(bitmap, null, rectF, paint);


    //    return output;
    // }

    // public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap) {
    //     int radius=40;

    //   Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
    //          bitmap.getHeight(), Bitmap.Config.ARGB_8888);
    //   Canvas canvas = new Canvas(output);

    //   final Paint paint = new Paint();
    //   final Rect rect = new Rect(0, 0, bitmap.getWidth(),bitmap.getHeight());

    //    paint.setAntiAlias(true);
    //  paint.setFilterBitmap(true);
    //  paint.setDither(true);
    //  canvas.drawARGB(0, 0, 0, 0);
    //  paint.setColor(Color.parseColor("#BAB399"));
    //  canvas.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, radius, paint);
    //  paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    //  canvas.drawBitmap(bitmap, rect, rect, paint);

//        return output;
    //  }



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
            answerCallFAB.show();
            answerCallFAB.setVisibility(View.VISIBLE);
            answerCallFAB.setOnClickListener(this);

            hangFAB.show();
            hangFAB.setVisibility(View.VISIBLE);

            videoFAB.setVisibility(View.VISIBLE);
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));

            microFAB.setVisibility(GONE);
        }
        else{
            videoFAB.show();
            videoFAB.setVisibility(View.VISIBLE);

            microFAB.show();
            microFAB.setVisibility(View.VISIBLE);

            answerCallFAB.setVisibility(GONE);
            hangFAB.show();
            hangFAB.setVisibility(View.VISIBLE);
        }
    }

    public void updateLocalVideoStatus(){
        log("updateLocalVideoStatus");
        if(callChat.hasLocalVideo()){
            log("Video local connected");
            if(myAvatarLayout.getVisibility()==View.VISIBLE){
                myAvatarLayout.setVisibility(GONE);
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));

                localCameraFragment = new LocalCameraCallFragment();
                parent.setVisibility(View.VISIBLE);
                fragmentContainerLocalCamera.setVisibility(View.VISIBLE);

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_local_camera, localCameraFragment, "localCameraFragment");
                ft.commitNowAllowingStateLoss();
            }
            else{
                log("No needed to refresh");
            }
        }
        else{
            log("Video local NOT connected");
            parent.setVisibility(View.GONE);
            fragmentContainerLocalCamera.setVisibility(View.GONE);
            if(localCameraFragment!=null){
                localCameraFragment.setVideoFrame(false);

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.remove(localCameraFragment);
                localCameraFragment = null;
            }

            myAvatarLayout.setVisibility(View.VISIBLE);
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));

        }
    }

    public void updateLocalAudioStatus(){
        if(callChat.hasLocalAudio()){
            log("Audio local connected");
            microFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            microFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_record_audio_w));

        }
        else{
            log("Audio local NOT connected");
            microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            microFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_off));

        }
    }

    public void updateRemoteVideoStatus(){
        if(callChat.hasRemoteVideo()){
            log("Video remote connected");
            contactAvatarLayout.setVisibility(View.GONE);
            contactAvatarLayout.setOnTouchListener(null);
            remoteSurfaceView.setOnTouchListener(this);
            megaChatApi.addChatRemoteVideoListener(this);
        }
        else{
            log("Video remote NOT connected");
            contactAvatarLayout.setVisibility(View.VISIBLE);
            remoteSurfaceView.setOnTouchListener(null);
            contactAvatarLayout.setOnTouchListener(this);
            megaChatApi.removeChatVideoListener(this);
        }
    }

    public void updateRemoteAudioStatus(){
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
                break;
            }
            case Constants.RECORD_AUDIO: {
                log("RECORD_AUDIO");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkPermissions()){
                        showInitialFABConfiguration();
                    }
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

//            float xCamera =  fragmentContainerLocalCamera.getX() ;
//            float yCamera = fragmentContainerLocalCamera.getY();

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
//                    }else if(view.getId() == R.id.fragment_container_local_camera){
//                        dX = view.getX() - event.getRawX();
//                        dY = view.getY() - event.getRawY();
//                    }
                    break;

                case MotionEvent.ACTION_MOVE:
//                    if(view.getId() == R.id.fragment_container_local_camera){
//                        if(flag){
//                            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
//                            params.leftMargin = (int )view.getX();
//                            params.topMargin = (int )view.getY();
//                            //***params.addRule(RelativeLayout.ABOVE, 0);
//                            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,0);
//                            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,0);
//                            view.setLayoutParams(params);
//                            flag = false;
//                        }
//                        view.animate()
//                            .x(event.getRawX() + dX)
//                            .y(event.getRawY() + dY)
//                            .setDuration(0)
//                            .start();
//                    }
                    break;

                default:
                    return false;
            }
       // parent.invalidate();
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
}
