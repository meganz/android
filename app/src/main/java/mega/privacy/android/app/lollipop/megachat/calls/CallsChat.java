package mega.privacy.android.app.lollipop.megachat.calls;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.tokenautocomplete.ContactInfo;
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

public class CallsChat extends PinActivityLollipop implements MegaRequestListenerInterface, View.OnTouchListener {

    DatabaseHandler dbH = null;
    ChatItemPreferences chatPrefs = null;
    MegaUser user;
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
    RelativeLayout myImageLayout;
    ImageView contactImage;
    ImageView myImage;
    private MenuItem firstIcon;
    private MenuItem secondIcon;

    FloatingActionButton firstFAB;
    FloatingActionButton secondFAB;
    FloatingActionButton thirdFAB;



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

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        handler = new Handler();
        setContentView(R.layout.activity_calls_chat);

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

        Bundle extras = getIntent().getExtras();
        if (extras != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.very_transparent_black));
            }

            imageLayout = (RelativeLayout) findViewById(R.id.call_chat_contact_image_layout);
            imageLayout.setOnTouchListener(this);
            myImageLayout = (RelativeLayout) findViewById(R.id.call_chat_my_image_layout);

            contactImage = (ImageView) findViewById(R.id.call_chat_contact_image);
            myImage = (ImageView) findViewById(R.id.call_chat_my_image);

            firstFAB = (FloatingActionButton) findViewById(R.id.first_fab);
            firstFAB.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    log("***First FAB");
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
            createMyDefaultAvatar(myUserHandle);

            File myavatar = null;

            if (context != null) {
                log("context is not null");

                if (context.getExternalCacheDir() != null) {
                    myavatar = new File(context.getExternalCacheDir().getAbsolutePath(), myUserMail + ".jpg");
                } else {
                    myavatar = new File(context.getCacheDir().getAbsolutePath(), myUserMail + ".jpg");
                }
            }


            Bitmap mybitmap = null;
            if (myavatar.exists()) {
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
                        myImage.setImageBitmap(mybitmap);
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
                            //imageGradient.setVisibility(View.VISIBLE);

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

                            myImage.setImageBitmap(mybitmap);
                            //imageGradient.setVisibility(View.VISIBLE);

                            if (mybitmap != null && !mybitmap.isRecycled()) {
                                Palette palette = Palette.from(mybitmap).generate();
                                Palette.Swatch swatch =  palette.getDarkVibrantSwatch();

                                myImageLayout.setBackgroundColor(swatch.getBodyTextColor());
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
    public void createMyDefaultAvatar(String myHandle) {
        log("setMyDefaultAvatar");

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);
        c.drawPaint(p);

        String color= megaApi.getUserAvatarColor(myHandle);
        if (color != null) {
            log("The color to set the avatar is " + color);
            myImageLayout.setBackgroundColor(Color.parseColor(color));
        } else {
            log("Default color to the avatar");
            myImageLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.transparency_white));
        }

        myImage.setImageBitmap(defaultAvatar);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {


        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if(aB.isShowing()){
                hideActionBar();
            }else{
                showActionBar();
            }
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

    @Override
    protected void onResume() {
        log("onResume-CallsChat");
        super.onResume();

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
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


}
