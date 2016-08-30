package mega.privacy.android.app.lollipop;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class ChatActivityLollipop extends PinActivityLollipop {

    MegaApiAndroid megaApi;
    Handler handler;

    ActionBar aB;
    Toolbar tB;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    RelativeLayout fragmentContainer;
    RelativeLayout writingContainerLayout;
    RelativeLayout writingLayout;
    RelativeLayout inviteLayout;
    TextView inviteText;
    ImageButton keyboardButton;
    EditText textChat;
    ImageButton sendIcon;
    RelativeLayout messagesContainerLayout;
    ScrollView messageScrollView;
    FloatingActionButton fab;

    ChatActivityLollipop chatActivity;

    MenuItem callMenuItem;
    MenuItem videoMenuItem;
    MenuItem inviteMenuItem;

    int diffMeasure;
    boolean focusChanged=false;

    View.OnClickListener foco = new View.OnClickListener() {


        @Override
        public void onClick(View v) {
            log("onClick");
        }
    };

    View.OnFocusChangeListener focus = new View.OnFocusChangeListener() {


        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            log("onFocusChange");
            if(!focusChanged){
                focusChanged = true;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        if (megaApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaApi = app.getMegaApi();
        }

        chatActivity = this;

        setContentView(R.layout.activity_chat);

        //Set toolbar
        tB = (Toolbar) findViewById(R.id.toolbar_chat);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
//		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;
        handler = new Handler();

        aB.setTitle(getResources().getString(R.string.section_chat));
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

        fragmentContainer = (RelativeLayout) findViewById(R.id.fragment_container_chat);

        writingContainerLayout = (RelativeLayout) findViewById(R.id.writing_container_layout_chat_layout);
        writingLayout = (RelativeLayout) findViewById(R.id.writing_linear_layout_chat);

        messageScrollView = (ScrollView) findViewById(R.id.message_scroll_view_chat_layout);

        keyboardButton = (ImageButton) findViewById(R.id.keyboard_icon_chat);
        textChat = (EditText) findViewById(R.id.edit_text_chat);

        inviteLayout  = (RelativeLayout) findViewById(R.id.invite_relative_chat_layout);
        inviteText = (TextView) findViewById(R.id.invite_text);

        sendIcon = (ImageButton) findViewById(R.id.send_message_icon_chat);
        messagesContainerLayout = (RelativeLayout) findViewById(R.id.message_container_chat_layout);

        fab = (FloatingActionButton) findViewById(R.id.fab_chat);

        Intent newIntent = getIntent();

        if (newIntent != null){
            if (newIntent.getAction() != null){
                if (newIntent.getAction().equals(Constants.ACTION_CHAT_INVITE)){
                    fab.setVisibility(View.GONE);
                    inviteLayout.setVisibility(View.VISIBLE);
                    textChat.setOnFocusChangeListener(focus);
                    messageScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {

                            if(!focusChanged){
                                diffMeasure = messageScrollView.getHeight();
                                log("Store Scroll height: "+messageScrollView.getHeight());
                                RelativeLayout.LayoutParams inviteTextViewParams = (RelativeLayout.LayoutParams)inviteText.getLayoutParams();
                                inviteTextViewParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(150, outMetrics), Util.scaleWidthPx(56, outMetrics), 0);
                                inviteText.setLayoutParams(inviteTextViewParams);
                            }
                            else{
                                int newMeasure = messageScrollView.getHeight();
                                log("New Scroll height: "+messageScrollView.getHeight());
                                if(newMeasure < (diffMeasure-200)){
                                    log("Keyboard shown!!!");
                                    RelativeLayout.LayoutParams inviteTextViewParams = (RelativeLayout.LayoutParams)inviteText.getLayoutParams();
                                    inviteTextViewParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(56, outMetrics), 0);
                                    inviteText.setLayoutParams(inviteTextViewParams);
                                }
                                else{
                                    log("Keyboard hidden!!!");
                                    RelativeLayout.LayoutParams inviteTextViewParams = (RelativeLayout.LayoutParams)inviteText.getLayoutParams();
                                    inviteTextViewParams.setMargins(Util.scaleWidthPx(43, outMetrics), Util.scaleHeightPx(150, outMetrics), Util.scaleWidthPx(56, outMetrics), 0);
                                    inviteText.setLayoutParams(inviteTextViewParams);
                                }
                            }
                        }
                    });
                    textChat.setText("Hi there!\nLet's chat!\nPlease accept my invitation.");
                }
            }
        }

    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenuLollipop");

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_action, menu);

        callMenuItem = menu.findItem(R.id.cab_menu_call_chat);
        videoMenuItem = menu.findItem(R.id.cab_menu_video_chat);
        inviteMenuItem = menu.findItem(R.id.cab_menu_invite_chat);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.cab_menu_call_chat:{

                break;
            }
            case R.id.cab_menu_video_chat:{

                break;
            }
            case R.id.cab_menu_invite_chat:{

                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        log("onBackPressedLollipop");
    }

    public static void log(String message) {
        Util.log("ChatActivityLollipop", message);
    }
}
