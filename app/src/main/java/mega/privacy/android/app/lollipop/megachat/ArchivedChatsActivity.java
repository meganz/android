package mega.privacy.android.app.lollipop.megachat;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ChatBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;

public class ArchivedChatsActivity extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatListenerInterface {

    AppBarLayout abL;
    Toolbar tB;
    ActionBar aB;
    FrameLayout fragmentContainer;
    RecentChatsFragmentLollipop archivedChatsFragment;
    FloatingActionButton fab;

    MenuItem createFolderMenuItem;
    MenuItem newChatMenuItem;

    private BadgeDrawerArrowDrawable badgeDrawable;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    public long selectedChatItemId;

    DisplayMetrics outMetrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        log("onCreate first");
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication)getApplication()).getMegaApi();
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
        if(Util.isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
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
        }

        megaChatApi.addChatListener(this);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        setContentView(R.layout.activity_chat_explorer);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_chat_explorer);
        fab = (FloatingActionButton) findViewById(R.id.fab_chat_explorer);
        fab.setVisibility(View.GONE);

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

        //Set toolbar
        abL = (AppBarLayout) findViewById(R.id.app_bar_layout_chat_explorer);
        tB = (Toolbar) findViewById(R.id.toolbar_chat_explorer);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if(aB!=null){
            aB.setTitle(getString(R.string.archived_chats_title_section).toUpperCase());
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }
        else{
            log("aB is null");
        }

        badgeDrawable = new BadgeDrawerArrowDrawable(getSupportActionBar().getThemedContext());

        updateNavigationToolbarIcon();

        if(archivedChatsFragment ==null){
            archivedChatsFragment = new RecentChatsFragmentLollipop().newInstance();
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_chat_explorer, archivedChatsFragment, "archivedChatsFragment");
        ft.commitNow();
    }

    public void changeActionBarElevation(boolean whitElevation){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (whitElevation) {
                abL.setElevation(Util.px2dp(4, outMetrics));
            }
            else {
                abL.setElevation(0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        super.callToSuperBack = true;
    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        if (item != null){
            log("onChatListItemUpdate:" + item.getTitle());
        }
        else{
            log("onChatListItemUpdate");
        }

        if(archivedChatsFragment!=null){
            if(archivedChatsFragment.isAdded()){
                archivedChatsFragment.listItemUpdate(item);
            }
        }

        if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
            updateNavigationToolbarIcon();
        }
    }

    public void showChatPanel(MegaChatListItem chat){
        log("showChatPanel");

        if(chat!=null){
            this.selectedChatItemId = chat.getChatId();
            ChatBottomSheetDialogFragment bottomSheetDialogFragment = new ChatBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");

        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void showSnackbar(String s){
        log("showSnackbar: "+s);
        Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    public void changeStatusBarColor(int option) {
        log("changeStatusBarColor");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (option == 1){
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.accentColorDark));
            }
            else {
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color_secondary));
            }
        }
    }

    public void updateNavigationToolbarIcon(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int numberUnread = megaChatApi.getUnreadChats();

            if(numberUnread==0){
                aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black);
            }
            else{

                badgeDrawable.setProgress(1.0f);

                if(numberUnread>9){
                    badgeDrawable.setText("9+");
                }
                else{
                    badgeDrawable.setText(numberUnread+"");
                }

                aB.setHomeAsUpIndicator(badgeDrawable);
            }
        }
        else{
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black);
        }
    }

    public static void log(String log) {
        Util.log("ArchivedChatsActivity", log);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish(CHAT)");

        if(request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM){
            long chatHandle = request.getChatHandle();
            MegaChatRoom chat = megaChatApi.getChatRoom(chatHandle);

            String chatTitle = chat.getTitle();

            if(chatTitle==null){
                chatTitle = "";
            }
            else if(!chatTitle.isEmpty() && chatTitle.length()>60){
                chatTitle = chatTitle.substring(0,59)+"...";
            }

            if(!chatTitle.isEmpty() && chat.isGroup() && !chat.hasCustomTitle()){
                chatTitle = "\""+chatTitle+"\"";
            }

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getFlag()){
                    log("Chat archived");
                    showSnackbar(getString(R.string.success_archive_chat, chatTitle));
                }
                else{
                    log("Chat unarchived");
                    showSnackbar(getString(R.string.success_unarchive_chat, chatTitle));
                }
            }
            else{
                if(request.getFlag()){
                    log("EEEERRRRROR WHEN ARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(getString(R.string.error_archive_chat, chatTitle));
                }
                else{
                    log("EEEERRRRROR WHEN UNARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(getString(R.string.error_unarchive_chat, chatTitle));
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {

    }
}
