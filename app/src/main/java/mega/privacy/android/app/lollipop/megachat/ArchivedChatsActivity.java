package mega.privacy.android.app.lollipop.megachat;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
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

public class ArchivedChatsActivity extends PinActivityLollipop implements View.OnClickListener, MegaChatRequestListenerInterface, MegaChatListenerInterface {

    Toolbar tB;
    ActionBar aB;
    FrameLayout fragmentContainer;
    RecentChatsFragmentLollipop archivedChatsFragment;
    FloatingActionButton fab;

    MenuItem createFolderMenuItem;
    MenuItem newChatMenuItem;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    public long selectedChatItemId;

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

        setContentView(R.layout.activity_chat_explorer);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_chat_explorer);
        fab = (FloatingActionButton) findViewById(R.id.fab_chat_explorer);
        fab.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        //Set toolbar
        tB = (Toolbar) findViewById(R.id.toolbar_chat_explorer);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        if(aB!=null){
            aB.setTitle(getString(R.string.archived_chats_title_section));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        }
        else{
            log("aB is null");
        }


        if(archivedChatsFragment ==null){
            archivedChatsFragment = new RecentChatsFragmentLollipop().newInstance();
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_chat_explorer, archivedChatsFragment, "archivedChatsFragment");
        ft.commitNow();
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

    public void changeStatusBarColor() {
        log("changeStatusBarColor");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }
    }


    public static void log(String log) {
        Util.log("ArchivedChatsActivity", log);
    }

    @Override
    public void onClick(View v) {
        log("onClick");

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        switch(v.getId()) {
            case R.id.fab_chat_explorer: {

                break;
            }
        }
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
            MegaChatListItem chatItem = megaChatApi.getChatListItem(chatHandle);
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getFlag()){
                    log("Chat archived");
                    showSnackbar(getString(R.string.success_archive_chat, chatItem.getTitle()));
                }
                else{
                    log("Chat unarchived");
                    showSnackbar(getString(R.string.success_unarchive_chat, chatItem.getTitle()));
                }
            }
            else{
                if(request.getFlag()){
                    log("EEEERRRRROR WHEN ARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(getString(R.string.error_archive_chat, chatItem.getTitle()));
                }
                else{
                    log("EEEERRRRROR WHEN UNARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(getString(R.string.error_unarchive_chat, chatItem.getTitle()));
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
}
