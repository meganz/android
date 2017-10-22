package mega.privacy.android.app.lollipop.megachat.calls;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.webrtc.videoengine.ViESurfaceRenderer;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
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

import static mega.privacy.android.app.utils.Util.context;

public class ChatCallActivity extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatCallListenerInterface, MegaChatVideoListenerInterface, MegaRequestListenerInterface, View.OnTouchListener, SurfaceHolder.Callback, View.OnClickListener {

    DatabaseHandler dbH = null;
    ChatItemPreferences chatPrefs = null;
    MegaUser myUser;

    String myUserMail;
    String myUserHandle;
    long chatHandle;
    MegaChatRoom chat;
    MegaChatCall call;
    MegaUser user;
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

    int var1=0;
    int var2=0;

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

        surfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
        //surfaceHolder = surfaceView.getHolder();
        //surfaceHolder.addCallback(this);
        //surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/
        renderer = new ViESurfaceRenderer(surfaceView);

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
            myImageBorder.setVisibility(View.GONE);
            callChatMyVideo = (RelativeLayout) findViewById(R.id.call_chat_my_video);
            callChatMyVideo.setVisibility(View.VISIBLE);
            myInitialLetter = (TextView) findViewById(R.id.call_chat_my_image_initial_letter);

            imageLayout = (RelativeLayout) findViewById(R.id.call_chat_contact_image_layout);
            imageLayout.setOnTouchListener(this);
            contactImageBorder = (RelativeLayout) findViewById(R.id.call_chat_contact_image_rl);
            contactImage = (RoundedImageView) findViewById(R.id.call_chat_contact_image);
            contactInitialLetter = (TextView) findViewById(R.id.call_chat_contact_image_initial_letter);

            videoFAB = (FloatingActionButton) findViewById(R.id.first_fab);
            videoFAB.setOnClickListener(this);

            microFAB = (FloatingActionButton) findViewById(R.id.second_fab);
            microFAB.setOnClickListener(this);

            hangFAB = (FloatingActionButton) findViewById(R.id.third_fab);
            hangFAB.setOnClickListener(this);

            answerCallFAB = (FloatingActionButton) findViewById(R.id.answer_call_fab);
            answerCallFAB.setVisibility(View.GONE);

            //My avatar
            myUserMail = megaApi.getMyEmail();
            myUserHandle = megaApi.getMyUserHandle();

            String myFullName = "Ursula";
            String myFirstLetter=myFullName.charAt(0) + "";
            myFirstLetter = myFirstLetter.toUpperCase(Locale.getDefault());
            createMyDefaultAvatar(myUserHandle, myFirstLetter);

            File myavatar = null;

            if (context != null) {
                log("context is not null");

                if (context.getExternalCacheDir() != null) {
                    myavatar = new File(context.getExternalCacheDir().getAbsolutePath(), myUserMail + ".jpg");
                } else {
                    myavatar = new File(context.getCacheDir().getAbsolutePath(), myUserMail + ".jpg");
                }
            }

            setProfileMyAvatar(myavatar);
        }

        //Contact's avatar
        chatHandle = extras.getLong("chatHandle", -1);
        log("Chat handle to call: "+chatHandle);
        if (chatHandle != -1) {
            chat = megaChatApi.getChatRoom(chatHandle);

            long callId = extras.getLong("callId", -1);
            if(callId==-1){
                videoFAB.setVisibility(View.VISIBLE);
                microFAB.setVisibility(View.VISIBLE);
                answerCallFAB.setVisibility(View.GONE);
            }
            else{
                answerCallFAB.setVisibility(View.VISIBLE);
                answerCallFAB.setOnClickListener(this);
                videoFAB.setVisibility(View.GONE);
                microFAB.setVisibility(View.GONE);
            }

            user = megaApi.getContact(chat.getPeerEmail(0));
            myInitialLetter.setText("P");

            String fullName = "";
            if (user != null) {
                log("User handle: " + user.getHandle());
                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
                if (contactDB != null) {
                    log("Contact DB found!");
                    String firstNameText = "";
                    String lastNameText = "";
                    String email = "";

                    firstNameText = contactDB.getName();
                    lastNameText = contactDB.getLastName();
                    email = contactDB.getMail();

                    if (firstNameText.trim().length() <= 0) {
                        fullName = lastNameText;
                    } else {
                        fullName = firstNameText + " " + lastNameText;
                    }

                    if (fullName.trim().length() <= 0) {
                        log("Put email as fullname");
                        email = user.getEmail();
                        String[] splitEmail = email.split("[@._]");
                        fullName = splitEmail[0];
                    }

                } else {
                    log("The contactDB is null: ");
                }
            }

            aB.setTitle(fullName);
            aB.setSubtitle("01:20");
            String firstLetter=fullName.charAt(0) + "";
            firstLetter = firstLetter.toUpperCase(Locale.getDefault());
            createDefaultAvatar(user, firstLetter);

            File avatar = null;
            if (context.getExternalCacheDir() != null) {
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
            } else {
                avatar = new File(context.getCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
            }

            setProfileAvatar(avatar);

        }
    }



    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getName());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        log("onRequestFinish(): "+request.getEmail());

        if (e.getErrorCode() == MegaError.API_OK){
            boolean avatarExists = false;
            if (user.getEmail().compareTo(request.getEmail()) == 0){

                File avatar = null;
                if (context.getExternalCacheDir() != null){
                    avatar = new File(context.getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
                }
                else{
                    avatar = new File(context.getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
                }
//
                //   Bitmap bitmap = null;
                //  if (avatar.exists()){
                //  if (avatar.length() > 0){
                //    BitmapFactory.Options bOpts = new BitmapFactory.Options();
                //      bOpts.inPurgeable = true;
                //       bOpts.inInputShareable = true;
                //      bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                //       if (bitmap == null) {
                //           avatar.delete();
                //       }
                //       else{

                //           contactImage.setImageBitmap(bitmap);
                //            contactInitialLetter.setVisibility(View.GONE);

                //           if (bitmap != null && !bitmap.isRecycled()) {
                //               Palette palette = Palette.from(bitmap).generate();
                //               Palette.Swatch swatch =  palette.getDarkVibrantSwatch();
//
                //               contactImage.setBackgroundColor(swatch.getBodyTextColor());
                //           }

                //       }
                //     }
                //  }
                setProfileAvatar(avatar);

            }else if (myUserMail.compareTo(request.getEmail()) == 0){
                File myavatar = null;
                if (context.getExternalCacheDir() != null){
                    myavatar = new File(context.getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
                }
                else{
                    myavatar = new File(context.getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
                }
                setProfileMyAvatar(myavatar);
                //Bitmap mybitmap = null;
                //if (myavatar.exists()){
                //  if (myavatar.length() > 0){
                //      BitmapFactory.Options bOpts = new BitmapFactory.Options();
                //      bOpts.inPurgeable = true;
                //      bOpts.inInputShareable = true;
                //      mybitmap = BitmapFactory.decodeFile(myavatar.getAbsolutePath(), bOpts);
                //      if (mybitmap == null) {
                //          myavatar.delete();
                //      }
                //      else{

                //                          myImage.setImageBitmap(mybitmap);
                //          myInitialLetter.setVisibility(View.GONE);

                //                          if (mybitmap != null && !mybitmap.isRecycled()) {
                //              Palette palette = Palette.from(mybitmap).generate();
                //              Palette.Swatch swatch =  palette.getDarkVibrantSwatch();
                //             myImage.setBackgroundColor(swatch.getBodyTextColor());
                //          }

                //     }
                //                  }
                //}
            }
        }

    }


    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {}

    public void createDefaultAvatar(MegaUser user,  String firstLetter) {
        log("createDefaultAvatar");

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);

        String color = megaApi.getUserAvatarColor(user);
        if (color != null) {
            log("The color to set the avatar is " + color);
            p.setColor(Color.parseColor(color));
        } else {
            log("Default color to the avatar");
            p.setColor(context.getResources().getColor(R.color.lollipop_primary_color));
        }



        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        contactImage.setImageBitmap(defaultAvatar);

        contactInitialLetter.setText(firstLetter);
        contactInitialLetter.setTextSize(60);
        contactInitialLetter.setTextColor(Color.WHITE);
        contactInitialLetter.setVisibility(View.VISIBLE);


    }

    public void setProfileAvatar(File avatar){
        log("setProfileAvatar");
        Bitmap bitmap = null;
        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, bitmap, 3);
                if (bitmap == null) {
                    avatar.delete();
                    if (context.getExternalCacheDir() != null) {
                        megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + user.getEmail() + ".jpg", this);
                    } else {
                        megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + user.getEmail() + ".jpg", this);
                    }
                } else {
                    contactImage.setImageBitmap(bitmap);
                    contactInitialLetter.setVisibility(View.GONE);

                }
            } else {
                if (context.getExternalCacheDir() != null) {
                    megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + user.getEmail() + ".jpg", this);
                } else {
                    megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + user.getEmail() + ".jpg", this);
                }
            }
        } else {
            if (context.getExternalCacheDir() != null) {
                megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + user.getEmail() + ".jpg", this);
            } else {
                megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + user.getEmail() + ".jpg", this);
            }
        }
    }

    public void createMyDefaultAvatar(String myHandle, String firstLetter) {
        log("createMyDefaultAvatar");

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels,outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);


        String color = megaApi.getUserAvatarColor(myHandle);
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

        myInitialLetter.setTextSize(40);
        myInitialLetter.setTextColor(Color.WHITE);
        myInitialLetter.setVisibility(View.VISIBLE);


    }

    public void setProfileMyAvatar(File avatar) {
        log("setProfileMyAvatar");

        Bitmap mybitmap = null;
        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                mybitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                mybitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, mybitmap, 3);
                if (mybitmap == null) {
                    avatar.delete();
                    if (context.getExternalCacheDir() != null) {
                        megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                    } else {
                        megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                    }
                } else {
                    log("** 1");
                     myImage.setImageBitmap(mybitmap);
                    myInitialLetter.setVisibility(View.GONE);

                }
            } else {
                if (context.getExternalCacheDir() != null) {
                    megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                } else {
                    megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                }
            }
        } else {

            if (context.getExternalCacheDir() != null) {
                megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
            } else {
                megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
            }
        }




    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if(aB.isShowing()){
                hideActionBar();
                videoFAB.hide();
                microFAB.hide();
                hangFAB.hide();
            }else{
                showActionBar();
                videoFAB.show();
                microFAB.show();
                hangFAB.show();
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
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    protected void onResume() {
        log("onResume-ChatCallActivity");
        super.onResume();

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
                videoFAB.setVisibility(View.VISIBLE);
                microFAB.setVisibility(View.VISIBLE);
                answerCallFAB.setVisibility(View.GONE);
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
    }

    @Override
    public void onChatCallTemporaryError(MegaChatApiJava api, MegaChatCall call, MegaChatError error) {
        log("onChatCallTemporaryError");
    }

    @Override
    public void onChatCallFinish(MegaChatApiJava api, MegaChatCall call, MegaChatError error) {
        log("onChatCallFinish");
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
            case R.id.first_fab:{
                log("First FAB");
                if(callChatMyVideo.isShown()){
                    callChatMyVideo.setVisibility(View.GONE);
                    myImageBorder.setVisibility(View.VISIBLE);
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));


                }else{
                    callChatMyVideo.setVisibility(View.VISIBLE);
                    myImageBorder.setVisibility(View.GONE);
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                }

                //  surfaceView.setVisibility(View.VISIBLE);
                // start_camera();
                break;
            }
            case R.id.second_fab: {
                log("***Second FAB");
                if(var1==1){

                    microFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));

                    var1=0;
                }else{
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));

                    var1=1;
                }
                break;
            }
            case R.id.third_fab: {
                log("***Third FAB");
                megaChatApi.hangChatCall(chatHandle, this);
                break;
            }
            case R.id.answer_call_fab:{
                megaChatApi.answerChatCall(chatHandle, true, this);
                break;
            }

        }
    }

    public static void log(String message) {
        Util.log("ChatCallActivity", message);
    }
}
