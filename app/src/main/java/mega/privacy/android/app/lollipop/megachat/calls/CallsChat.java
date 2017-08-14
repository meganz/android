package mega.privacy.android.app.lollipop.megachat.calls;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.tokenautocomplete.ContactInfo;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatItemPreferences;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Util.context;

/**
 * Created by mega on 10/07/17.
 */

public class CallsChat extends PinActivityLollipop implements MegaRequestListenerInterface, View.OnTouchListener, SurfaceHolder.Callback {

    DatabaseHandler dbH = null;
    ChatItemPreferences chatPrefs = null;
    MegaUser user;
    MegaUser myUser;

    String myUserMail;
    String myUserHandle;
    long chatHandle;
    String userEmailExtra;
    MegaChatRoom chat;
    boolean fromContacts = true;
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

    RelativeLayout imageLayout;
    RelativeLayout callChatMyVideo;
    RelativeLayout myImageBorder;
    ImageView contactImage;
    ImageView myImage;
    TextView myInitialLetter;

    private MenuItem firstIcon;
    private MenuItem secondIcon;

    FloatingActionButton firstFAB;
    FloatingActionButton secondFAB;
    FloatingActionButton thirdFAB;

    /************************************/
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    /**********************************************/



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

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);


        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        if (megaApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaApi = app.getMegaApi();
        }


        log("retryPendingConnections()");
        if (megaApi != null) {
            log("---------retryPendingConnections");
            megaApi.retryPendingConnections();
        }

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

        /*********************************************/
        /*surfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/

        /*********************************************/

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.very_transparent_black));
            }

            myImage = (RoundedImageView) findViewById(R.id.call_chat_my_image);
            myImageBorder = (RelativeLayout) findViewById(R.id.call_chat_my_image_border);
            callChatMyVideo = (RelativeLayout) findViewById(R.id.call_chat_my_video);
            myInitialLetter = (TextView) findViewById(R.id.call_chat_my_image_initial_letter);

            imageLayout = (RelativeLayout) findViewById(R.id.call_chat_contact_image_layout);
            imageLayout.setOnTouchListener(this);
            contactImage = (ImageView) findViewById(R.id.call_chat_contact_image);


            firstFAB = (FloatingActionButton) findViewById(R.id.first_fab);
            firstFAB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    log("***First FAB");
                    if(callChatMyVideo.isShown()){
                        callChatMyVideo.setVisibility(View.GONE);
                        myImageBorder.setVisibility(View.VISIBLE);

                    }else{
                        callChatMyVideo.setVisibility(View.VISIBLE);
                        myImageBorder.setVisibility(View.GONE);
                    }
                  //  surfaceView.setVisibility(View.VISIBLE);
                  // start_camera();
                }
            });
            secondFAB = (FloatingActionButton) findViewById(R.id.second_fab);
            secondFAB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    log("***Second FAB");
                }
            });
            thirdFAB = (FloatingActionButton) findViewById(R.id.third_fab);
            thirdFAB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    log("***Third FAB");
                }
            });



            /*My avatar*/
            myUserMail = megaApi.getMyEmail();
            myUserHandle = megaApi.getMyUserHandle();
            //megaApi.getMyUser().





        //String fullName = myAccountInfo.getFirstLetter();
          //  log("**#fullName: "+fullName);
           /* MegaContactDB contactDB = dbH.findContactByHandle(myUserHandle);
            if(contactDB!=null){
                fullName = contactDB.getName();
                log("***a fullName: "+fullName);

                String firstLetter=fullName.charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                myInitialLetter.setText(firstLetter);

            }else{
                log("***b fullName: "+fullName);

            }*/

            //createMyDefaultAvatar(myUserHandle, fullName);

            File myavatar = null;

            if (context != null) {

                log("** 1context is not null");

                if (context.getExternalCacheDir() != null) {
                    myavatar = new File(context.getExternalCacheDir().getAbsolutePath(), myUserMail + ".jpg");
                } else {
                    myavatar = new File(context.getCacheDir().getAbsolutePath(), myUserMail + ".jpg");
                }
            }

            /***/
            setProfileMyAvatar(myavatar);

            /*Bitmap mybitmap = null;
            if (myavatar.exists()) {
                log("** 2 myavatar exists");
                if (myavatar.length() > 0) {
                    BitmapFactory.Options bOpts = new BitmapFactory.Options();
                    bOpts.inPurgeable = true;
                    bOpts.inInputShareable = true;
                    mybitmap = BitmapFactory.decodeFile(myavatar.getAbsolutePath(), bOpts);
                    mybitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context, mybitmap, 3);
                    if (mybitmap == null) {
                        myavatar.delete();
                        if (context.getExternalCacheDir() != null) {
                            megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                        } else {
                            megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                        }
                    } else {
                        log("** 1");
                       // Bitmap roundBitmap = getRoundedCroppedBitmap(mybitmap);

                       // myImage.setImageBitmap(roundBitmap);
                    }
                } else {
                    if (context.getExternalCacheDir() != null) {
                        megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                    } else {
                        megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                    }
                }
            } else {
                log("** 3 myavatar not exists");

                if (context.getExternalCacheDir() != null) {
                    megaApi.getUserAvatar(user, context.getExternalCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                } else {
                    megaApi.getUserAvatar(user, context.getCacheDir().getAbsolutePath() + "/" + myUserMail + ".jpg", this);
                }
            }*/

        }


        /*Contact's avatar*/
        chatHandle = extras.getLong("handle", -1);

        if (chatHandle == -1) {
            log("From contacts!!");
            fromContacts = true;
            userEmailExtra = extras.getString("name");
            user = megaApi.getContact(userEmailExtra);

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
            createDefaultAvatar(user);

            File avatar = null;
            if (context.getExternalCacheDir() != null) {
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
            } else {
                avatar = new File(context.getCacheDir().getAbsolutePath(), user.getEmail() + ".jpg");
            }

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

                Bitmap bitmap = null;
                if (avatar.exists()){
                    if (avatar.length() > 0){
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                        }
                        else{

                            contactImage.setImageBitmap(bitmap);

                            if (bitmap != null && !bitmap.isRecycled()) {
                                Palette palette = Palette.from(bitmap).generate();
                                Palette.Swatch swatch =  palette.getDarkVibrantSwatch();

                                imageLayout.setBackgroundColor(swatch.getBodyTextColor());
                            }

                        }
                    }
                }
            }

            else if (myUserMail.compareTo(request.getEmail()) == 0){
                File myavatar = null;
                if (context.getExternalCacheDir() != null){
                    myavatar = new File(context.getExternalCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
                }
                else{
                    myavatar = new File(context.getCacheDir().getAbsolutePath(), request.getEmail() + ".jpg");
                }


                Bitmap mybitmap = null;
                if (myavatar.exists()){
                    if (myavatar.length() > 0){
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        mybitmap = BitmapFactory.decodeFile(myavatar.getAbsolutePath(), bOpts);
                        if (mybitmap == null) {
                            myavatar.delete();
                        }
                        else{

                           // Bitmap roundImage = getRoundedCornerBitmap(mybitmap);
                           // myImage.setImageBitmap(roundImage);
                            log("** 2");

                            //imageGradient.setVisibility(View.VISIBLE);

                            if (mybitmap != null && !mybitmap.isRecycled()) {
                                Palette palette = Palette.from(mybitmap).generate();
                                Palette.Swatch swatch =  palette.getDarkVibrantSwatch();
                                log("** 3");

                               // myImage.setBackgroundColor(swatch.getBodyTextColor());
                            }

                        }
                    }
                }
            }
        }

    }


    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {}

    public void createDefaultAvatar(MegaUser user) {
        log("setDefaultAvatar");

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);
        c.drawPaint(p);

        String color = megaApi.getUserAvatarColor(user);
        if (color != null) {
            log("The color to set the avatar is " + color);
            imageLayout.setBackgroundColor(Color.parseColor(color));
        } else {
            log("Default color to the avatar");
            imageLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.transparency_white));
        }

        contactImage.setImageBitmap(defaultAvatar);
    }

    public void createMyDefaultAvatar(String myHandle, String fullName) {
        log("** createMyDefaultAvatar");

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
        myInitialLetter.setText("");

        //myInitialLetter.setText("R");


        String firstLetter=fullName.charAt(0) + "";
        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
        myInitialLetter.setText(firstLetter);


        myInitialLetter.setTextSize(40);
        myInitialLetter.setTextColor(Color.WHITE);
        myInitialLetter.setVisibility(View.VISIBLE);


    }

    public void setProfileMyAvatar(File avatar) {
        log("setProfileAvatar");

        Bitmap mybitmap = null;
        if (avatar.exists()) {
            log("** 2 myavatar exists");
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
            log("** 3 myavatar not exists");

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
                firstFAB.hide();
                secondFAB.hide();
                thirdFAB.hide();
            }else{
                showActionBar();
                firstFAB.show();
                secondFAB.show();
                thirdFAB.show();
            }
            /*if(firstFAB.isShown()){
                hideFabButton();
            }else{
                showFabButton();
            }*/
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
    /*protected void hideFabButton(final FloatingActionButton fab){
        if (fab != null && fab.isShown()) {
            if(fab != null) {
                fab.animate().translationY(320).setDuration(800L)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                fab.hide();
                            }
                        }).start();
            } else {
                fab.hide();
            }
        }
    }

    protected void showFabButton(FloatingActionButton fab2){
        log("**1");
        if (fab2 != null && !fab2.isShown()) {
            log("**2");

            fab2.show();
            if(fab2 != null) {
                log("**3");

                fab2.animate().translationY(-320).setDuration(800L).start();
                fab2.setVisibility(View.VISIBLE);
            }
        }
    }*/

    /********/
    @Override public void onPause(){
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
        }
    }
    /*******/

    @Override
    protected void onResume() {
        log("onResume-CallsChat");
        super.onResume();

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
    }
    /********/
    @Override public void onDestroy(){
        super.onDestroy();
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
    /********/


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
/****************************************/


    private void start_camera() {


        /*****/

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
        /****/


        Camera.Parameters param;
        param = camera.getParameters();
        param.setPreviewFrameRate(20);
        param.setPreviewSize(176, 144);
        camera.setParameters(param);
        try {
            /*Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

            if(display.getRotation() == Surface.ROTATION_0) {
                camera.setDisplayOrientation(90);
            }else if(display.getRotation() == Surface.ROTATION_270) {
                camera.setDisplayOrientation(180);
            }*/
            camera.setDisplayOrientation(90);

            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            log("init_camera: " + e);
            return;
        }
    }


    /*public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }*/
//************************************/

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

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap){

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int radius = 40;

        Bitmap output = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF rectF = new RectF(0,0,w,h);

        canvas.drawRoundRect(rectF,radius,radius,paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rectF, paint);


        return output;
    }

    public static Bitmap getRoundedCroppedBitmap(Bitmap bitmap) {
        int radius=40;

        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                bitmap.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }


}
