package mega.privacy.android.app.lollipop.megachat;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ChatBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
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
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ArchivedChatsActivity extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatListenerInterface, MegaRequestListenerInterface {

    AppBarLayout abL;
    Toolbar tB;
    ActionBar aB;
    FrameLayout fragmentContainer;
    RecentChatsFragmentLollipop archivedChatsFragment;
    FloatingActionButton fab;

    private BadgeDrawerArrowDrawable badgeDrawable;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    public long selectedChatItemId;

    DisplayMetrics outMetrics;

    MenuItem searchMenuItem;
    SearchView searchView;

    ArchivedChatsActivity archivedChatsActivity;

    String querySearch = "";
    boolean isSearchExpanded = false;
    boolean pendingToOpenSearchView = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication)getApplication()).getMegaApi();
        }

        if(megaApi==null||megaApi.getRootNode()==null){
            logDebug("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if(isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
            }

            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
                logDebug("Refresh session - karere");
                Intent intent = new Intent(this, LoginActivityLollipop.class);
                intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
        }

        megaChatApi.addChatListener(this);

        archivedChatsActivity = this;

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
            logWarning("aB is null");
        }

//        badgeDrawable = new BadgeDrawerArrowDrawable(getSupportActionBar().getThemedContext());
        badgeDrawable = new BadgeDrawerArrowDrawable(this);

        updateNavigationToolbarIcon();

        if(archivedChatsFragment ==null){
            archivedChatsFragment = new RecentChatsFragmentLollipop().newInstance();
        }

        if (savedInstanceState != null) {
            querySearch = savedInstanceState.getString("querySearch", "");
            isSearchExpanded = savedInstanceState.getBoolean("isSearchExpanded", isSearchExpanded);

            if (isSearchExpanded) {
                pendingToOpenSearchView = true;
            }
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_chat_explorer, archivedChatsFragment, "archivedChatsFragment");
        ft.commitNow();
    }

    public void changeActionBarElevation(boolean whitElevation){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (whitElevation) {
                abL.setElevation(px2dp(4, outMetrics));
            }
            else {
                abL.setElevation(0);
            }
        }
    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        if (item != null){
            logDebug("Chat ID: " + item.getChatId());
        }
        else{
            logError("Item is NULL");
            return;
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
        logDebug("showChatPanel");

        if(chat!=null){
            this.selectedChatItemId = chat.getChatId();
            ChatBottomSheetDialogFragment bottomSheetDialogFragment = new ChatBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("querySearch", querySearch);
        outState.putBoolean("isSearchExpanded", isSearchExpanded);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_archived_chats, menu);

        searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.setIcon(mutateIconSecondary(this, R.drawable.ic_menu_search, R.color.black));

        searchView = (SearchView) searchMenuItem.getActionView();

        SearchView.SearchAutoComplete searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setTextColor(ContextCompat.getColor(this, R.color.black));
        searchAutoComplete.setHintTextColor(ContextCompat.getColor(this, R.color.status_bar_login));
        searchAutoComplete.setHint(getString(R.string.hint_action_search));
        View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

        if (searchView != null){
            searchView.setIconifiedByDefault(true);
        }

        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                isSearchExpanded = true;
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                isSearchExpanded = false;
                archivedChatsFragment.closeSearch();
                supportInvalidateOptionsMenu();
                return true;
            }
        });

        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                logDebug("Query: " + query);
                hideKeyboard(archivedChatsActivity, 0);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                querySearch = newText;
                archivedChatsFragment.filterChats(newText);
                return true;
            }
        });

        if (pendingToOpenSearchView) {
            String query = querySearch;
            searchMenuItem.expandActionView();
            searchView.setQuery(query, false);
            pendingToOpenSearchView = false;
        }

        ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();
        if (archivedChats != null && !archivedChats.isEmpty()) {
            searchMenuItem.setVisible(true);
        }
        else {
            searchMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public void closeSearchView () {
        if (searchMenuItem != null && searchMenuItem.isActionViewExpanded()) {
            searchMenuItem.collapseActionView();
        }
    }

    public void showSnackbar(String s){
        logDebug("showSnackbar: " + s);
        showSnackbar(fragmentContainer, s);
    }

    public void changeStatusBarColor(int option) {
        logDebug("Option: " + option);
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

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish(CHAT)");

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
                    logDebug("Chat archived");
                    showSnackbar(getString(R.string.success_archive_chat, chatTitle));
                }
                else{
                    logDebug("Chat unarchived");
                    showSnackbar(getString(R.string.success_unarchive_chat, chatTitle));
                }
            }
            else{
                if(request.getFlag()){
                    logError("ERROR WHEN ARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(getString(R.string.error_archive_chat, chatTitle));
                }
                else{
                    logError("ERROR WHEN UNARCHIVING CHAT " + e.getErrorString());
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

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            long requestNumber = request.getNumber();
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + requestNumber);
            int errorCode = e.getErrorCode();
            if (errorCode == MegaError.API_OK && requestNumber == MegaContactRequest.INVITE_ACTION_ADD) {
                showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
            } else if (errorCode == MegaError.API_EEXIST) {
                showSnackbar(getString(R.string.context_contact_already_exists, request.getEmail()));
            } else if (errorCode == MegaError.API_EARGS && requestNumber == MegaContactRequest.INVITE_ACTION_ADD) {
                showSnackbar(getString(R.string.error_own_email_as_contact));
            } else {
                showSnackbar(getString(R.string.general_error));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
