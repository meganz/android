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

public class ChatActivityLollipop extends PinActivityLollipop implements View.OnClickListener {

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
    RelativeLayout disabledWritingLayout;
    RelativeLayout inviteLayout;
    TextView inviteText;
    TextView invitationSentText;
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

    KeyboardListener keyboardListener;

    String intentAction;

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
        disabledWritingLayout = (RelativeLayout) findViewById(R.id.writing_disabled_linear_layout_chat);

        messageScrollView = (ScrollView) findViewById(R.id.message_scroll_view_chat_layout);

        keyboardButton = (ImageButton) findViewById(R.id.keyboard_icon_chat);
        textChat = (EditText) findViewById(R.id.edit_text_chat);

        inviteLayout  = (RelativeLayout) findViewById(R.id.invite_relative_chat_layout);
        inviteText = (TextView) findViewById(R.id.invite_text);
        invitationSentText = (TextView) findViewById(R.id.invitation_sent_text);

        sendIcon = (ImageButton) findViewById(R.id.send_message_icon_chat);
        sendIcon.setOnClickListener(this);

        messagesContainerLayout = (RelativeLayout) findViewById(R.id.message_container_chat_layout);

        fab = (FloatingActionButton) findViewById(R.id.fab_chat);

        Intent newIntent = getIntent();

        if (newIntent != null){
            intentAction = newIntent.getAction();
            if (intentAction != null){
                if (intentAction.equals(Constants.ACTION_CHAT_INVITE)){
                    fab.setVisibility(View.GONE);
                    inviteLayout.setVisibility(View.VISIBLE);
                    textChat.setOnFocusChangeListener(focus);
                    keyboardListener = new KeyboardListener();
                    messageScrollView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
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
    public void onDestroy(){

//        messageScrollView.getViewTreeObserver().removeOnGlobalLayoutListener();
        super.onDestroy();

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
        finish();
    }

    public static void log(String message) {
        Util.log("ChatActivityLollipop", message);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.home:{
                messageScrollView.getViewTreeObserver().removeGlobalOnLayoutListener(keyboardListener);
            }
			case R.id.send_message_icon_chat:{
                log("click on Send message");
                messageScrollView.getViewTreeObserver().removeGlobalOnLayoutListener(keyboardListener);

                writingLayout.setClickable(false);
                String text = textChat.getText().toString();
                textChat.getText().clear();
                textChat.setFocusable(false);
                textChat.setEnabled(false);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) disabledWritingLayout.getLayoutParams();
                params.height = writingLayout.getHeight();
                disabledWritingLayout.setLayoutParams(params);
                disabledWritingLayout.setVisibility(View.VISIBLE);

                inviteText.setVisibility(View.GONE);
                invitationSentText.setText(text);
                invitationSentText.setVisibility(View.VISIBLE);
			}
		}
    }

    private class KeyboardListener implements ViewTreeObserver.OnGlobalLayoutListener{
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
    }
}