package mega.privacy.android.app.lollipop.megachat.calls;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.videoengine.ViESurfaceRenderer;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.PhoneContactsActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
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

public class ChatCallActivity extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatCallListenerInterface, MegaChatVideoListenerInterface, MegaRequestListenerInterface, View.OnTouchListener, SurfaceHolder.Callback, View.OnClickListener, SensorEventListener {

    DatabaseHandler dbH = null;
    ChatItemPreferences chatPrefs = null;
    MegaUser myUser;

    String myUserMail;
    long chatHandle;
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

    //my avatar
    RelativeLayout callChatMyVideo;
    RelativeLayout myImageBorder;
    RoundedImageView myImage;
    TextView myInitialLetter;

    //contact avatar
    RelativeLayout imageLayout;
    RoundedImageView contactImage;
    TextView contactInitialLetter;
    RelativeLayout contactImageBorder;

    static ChatCallActivity chatCallActivityActivity = null;

    private MenuItem firstIcon;
    private MenuItem secondIcon;

    FloatingActionButton videoFAB;
    FloatingActionButton microFAB;
    FloatingActionButton hangFAB;
    FloatingActionButton answerCallFAB;

    Camera camera;
    SurfaceView surfaceView;
    ViESurfaceRenderer renderer;
    SurfaceHolder surfaceHolder;
    AudioManager audioManager;
    MediaPlayer thePlayer;

    String fullName = "";
    String email = "";
    long userHandle = -1;

    int var1=0;
    int var2=0;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_calls_chat, menu);

        firstIcon = menu.findItem(R.id.action_first_icon);
        secondIcon = menu.findItem(R.id.action_second_icon);

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
            case R.id.action_first_icon: {

                break;
            }
            case R.id.action_second_icon: {

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

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
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

        megaChatApi.addChatCallListener(this);
        megaChatApi.addChatRemoteVideoListener(this);

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
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

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

        surfaceView = (SurfaceView)findViewById(R.id.surface_remote_video);

        //surfaceHolder = surfaceView.getHolder();
        //surfaceHolder.addCallback(this);
        //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/
        renderer = new ViESurfaceRenderer(surfaceView);
//        surfaceView.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.very_transparent_black));
            }

            myImage = (RoundedImageView) findViewById(R.id.call_chat_my_image);
            myImageBorder = (RelativeLayout) findViewById(R.id.call_chat_my_image_rl);
            myImageBorder.setVisibility(GONE);
            callChatMyVideo = (RelativeLayout) findViewById(R.id.call_chat_my_video);
            callChatMyVideo.setVisibility(View.VISIBLE);
            myInitialLetter = (TextView) findViewById(R.id.call_chat_my_image_initial_letter);

            imageLayout = (RelativeLayout) findViewById(R.id.call_chat_contact_image_layout);
            imageLayout.setOnTouchListener(this);
            contactImageBorder = (RelativeLayout) findViewById(R.id.call_chat_contact_image_rl);
            contactImage = (RoundedImageView) findViewById(R.id.call_chat_contact_image);
            contactInitialLetter = (TextView) findViewById(R.id.call_chat_contact_image_initial_letter);

            setProfileMyAvatar();

            //Contact's avatar
            chatHandle = extras.getLong("chatHandle", -1);
            log("Chat handle to call: " + chatHandle);
            if (chatHandle != -1) {
                chat = megaChatApi.getChatRoom(chatHandle);
                callChat = megaChatApi.getChatCallByChatId(chatHandle);

                int callStatus = callChat.getStatus();
                log("The status of the callChat is: " + callStatus);
                switch (callStatus) {
                    case MegaChatCall.CALL_STATUS_IN_PROGRESS: {
                        break;
                    }
                    case MegaChatCall.CALL_STATUS_RING_IN:{

                        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        thePlayer = MediaPlayer.create(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
                        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
                        this.setVolumeControlStream(AudioManager.STREAM_RING);
                        this.setVolumeControlStream(AudioManager.STREAM_ALARM);
                        this.setVolumeControlStream(AudioManager.STREAM_NOTIFICATION);
                        this.setVolumeControlStream(AudioManager.STREAM_SYSTEM);
                        this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

                        thePlayer.start();
                    }
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
                aB.setSubtitle("01:20");

                setUserProfileAvatar(userHandle, fullName);
            }
        }

        if(checkPermissions()){
            showFABs();
        }


    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getName());
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
        log("createDefaultAvatar");

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);

        String color = megaApi.getUserAvatarColor(megaApi.handleToBase64(userHandle));
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
        log("setUserProfileAvatar");

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

        String color = megaApi.getUserAvatarColor(megaApi.handleToBase64(userHandle));
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
        log("setProfileMyAvatar");

        Bitmap myBitmap = null;
        File avatar = null;
        if (context != null) {
            log("context is not null");
            if (context.getExternalCacheDir() != null) {
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myUserMail + ".jpg");
            } else {
                avatar = new File(context.getCacheDir().getAbsolutePath(), myUserMail + ".jpg");
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if(aB.isShowing()){
                hideActionBar();
                showFABs();
            }else{
                showActionBar();
                showFABs();
            }
            //if(videoFAB.isShown()){
            //    hideFabButton();
            //}else{
            //   showFabButton();
            // }
        }
        return false;
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

    @Override public void onPause(){
        super.onPause();
       mSensorManager.unregisterListener(this);
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    protected void onResume() {
        log("onResume-ChatCallActivity");
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
    }
    @Override public void onDestroy(){
        super.onDestroy();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
            megaChatApi.removeChatVideoListener(this);
        }
    }


    @Override
    public void onBackPressed() {
//		if (overflowMenuLayout != null){
//			if (overflowMenuLayout.getVisibility() == View.VISIBLE){
//				overflowMenuLayout.setVisibility(View.GONE);
//				return;
//			}
//		}
        super.onBackPressed();
    }


    private void start_camera() {



        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    camera = Camera.open(camIdx);
                } catch (RuntimeException e) {
                }
            }
        }


        Camera.Parameters param;
        param = camera.getParameters();
        param.setPreviewFrameRate(20);
        param.setPreviewSize(176, 144);
        camera.setParameters(param);
        try {
            //Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

            //  if(display.getRotation() == Surface.ROTATION_0) {
            //     camera.setDisplayOrientation(90);
            // }else if(display.getRotation() == Surface.ROTATION_270) {
            //      camera.setDisplayOrientation(180);
            //   }
            camera.setDisplayOrientation(90);

            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            log("init_camera: " + e);
            return;
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        log("onRequestStart");
        log("Type: "+request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish");
        if(request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL){
            finish();
        }
        else if(request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Ok. CAll answered");
                if(request.getParamType()==1){
                    log("Ok. CAll answered");
                    videoFAB.setVisibility(View.VISIBLE);
                    microFAB.setVisibility(View.VISIBLE);
                    answerCallFAB.setVisibility(GONE);
                }
                else{
                    log("Rejected call");
                    finish();
                }
            }
            else{
                log("Error call: "+e.getErrorString());
//                showSnackbar(getString(R.string.clear_history_error));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestTemporaryError");
    }

    @Override
    public void onChatCallStart(MegaChatApiJava api, MegaChatCall call) {
        log("onChatCallStart");
    }

    @Override
    public void onChatCallIncoming(MegaChatApiJava api, MegaChatCall call) {
        log("onChatCallIncoming");
    }

    @Override
    public void onChatCallStateChange(MegaChatApiJava api, MegaChatCall call) {
        log("onChatCallStateChange");

        int callStatus = call.getStatus();
        log("The status of the call is: " + callStatus);
        if(callStatus!=MegaChatCall.CALL_STATUS_RING_IN){
            thePlayer.stop();
        }
    }

    @Override
    public void onChatCallTemporaryError(MegaChatApiJava api, MegaChatCall call, MegaChatError error) {
        log("onChatCallTemporaryError");
    }

    @Override
    public void onChatCallFinish(MegaChatApiJava api, MegaChatCall call, MegaChatError error) {
        log("onChatCallFinish");
        finish();
    }

    int width = 0;
    int height = 0;
    Bitmap bitmap;

    @Override
    public void onChatVideoData(MegaChatApiJava api, MegaChatCall chatCall, int width, int height, byte[] byteBuffer)
    {
        //log("onChatVideoData");
        if (this.width != width || this.height != height)
        {
            this.width = width;
            this.height = height;
            this.bitmap = renderer.CreateBitmap(width, height);
        }

        // Colors seem to be in a wrong order
        // Also, it's not very efficient to have an alpha channel
        // This require investigation and improvements
        for(int i = 0; i < width * height * 4; i += 4) {
            byte tmp = byteBuffer[i];
            byteBuffer[i] = byteBuffer[i + 2];
            byteBuffer[i + 2] = tmp;
        }

        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));

        // Instead of using this WebRTC renderer, we should probably draw the image by ourselves.
        // The renderer has been modified a bit and an update of WebRTC could break our app
        renderer.DrawBitmap();
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

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        switch (v.getId()) {
            case R.id.home: {
                break;
            }
            case R.id.video_fab:{
                if(callChatMyVideo.isShown()){
                    callChatMyVideo.setVisibility(GONE);
                    myImageBorder.setVisibility(View.VISIBLE);
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));


                }else{
                    callChatMyVideo.setVisibility(View.VISIBLE);
                    myImageBorder.setVisibility(GONE);
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                }

                //  surfaceView.setVisibility(View.VISIBLE);
//                 start_camera();
                break;
            }
            case R.id.micro_fab: {
                if(var1==1){

                    microFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));

                    var1=0;
                }else{
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));

                    var1=1;
                }
                break;
            }
            case R.id.hang_fab: {
                if(callChat!=null){
                    if(callChat.getStatus()==MegaChatCall.CALL_STATUS_RING_IN){
                        log("Reject call");
                        megaChatApi.rejectChatCall(chatHandle, this);
                    }
                    else{
                        log("Hang call");
                        megaChatApi.hangChatCall(chatHandle, this);
                    }
                }

                break;
            }
            case R.id.answer_call_fab:{
                megaChatApi.answerChatCall(chatHandle, true, this);
                break;
            }

        }
    }

    public boolean checkPermissions(){
        log("checkPermissions");

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

    public void showFABs(){
        if(callChat.getStatus()==MegaChatCall.CALL_STATUS_RING_IN){
            answerCallFAB.setVisibility(View.VISIBLE);
            answerCallFAB.setOnClickListener(this);
            hangFAB.setVisibility(View.VISIBLE);
            videoFAB.setVisibility(GONE);
            microFAB.setVisibility(GONE);
        }
        else{
            videoFAB.setVisibility(View.VISIBLE);
            microFAB.setVisibility(View.VISIBLE);
            answerCallFAB.setVisibility(GONE);
            hangFAB.setVisibility(View.VISIBLE);
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
                    if(checkPermissions()){
                       showFABs();
                    }
                }
                break;
            }
            case Constants.RECORD_AUDIO: {
                log("RECORD_AUDIO");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkPermissions()){
                        showFABs();
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}
