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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

public class ChatExplorerActivity extends PinActivityLollipop implements View.OnClickListener, MegaChatRequestListenerInterface{

    Toolbar tB;
    ActionBar aB;
    FrameLayout fragmentContainer;
    ChatExplorerFragment chatExplorerFragment;
    FloatingActionButton fab;
    public long chatIdFrom=-1;

    private long[] nodeHandles;
    private long[] messagesIds;
    private long[] userHandles;

    MenuItem createFolderMenuItem;
    MenuItem newChatMenuItem;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

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

        setContentView(R.layout.activity_chat_explorer);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container_chat_explorer);
        fab = (FloatingActionButton) findViewById(R.id.fab_chat_explorer);
        fab.setOnClickListener(this);

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
            aB.setTitle(getString(R.string.choose_chat));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        }
        else{
            log("aB is null");
        }

        showFabButton(false);

        Intent intent = getIntent();

        if(intent!=null){
            log("Intent received");
            if(intent.getAction()!=null){
                if(intent.getAction()== Constants.ACTION_FORWARD_MESSAGES){
                    messagesIds = intent.getLongArrayExtra("ID_MESSAGES");
                    log("No of messages to forward: "+messagesIds.length);
                    chatIdFrom = intent.getLongExtra("ID_CHAT_FROM", -1);
                }
            }
            else{
                nodeHandles = intent.getLongArrayExtra("NODE_HANDLES");
                if(nodeHandles!=null){
                    log("Node handle is: "+nodeHandles[0]);
                }
                userHandles = intent.getLongArrayExtra("USER_HANDLES");
                if(userHandles!=null){
                    log("User handles size: "+userHandles.length);
                }
            }
        }

        if(chatExplorerFragment ==null){
            chatExplorerFragment = new ChatExplorerFragment().newInstance();
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container_chat_explorer, chatExplorerFragment, "chatExplorerFragment");
        ft.commitNow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenuLollipop");

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file_explorer_action, menu);

        createFolderMenuItem = menu.findItem(R.id.cab_menu_create_folder);
        newChatMenuItem = menu.findItem(R.id.cab_menu_new_chat);

        createFolderMenuItem.setVisible(false);
        newChatMenuItem.setVisible(true);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");

        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.cab_menu_new_chat:{

                if(megaApi!=null && megaApi.getRootNode()!=null){
                    ArrayList<MegaUser> contacts = megaApi.getContacts();
                    if(contacts==null){
                        showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
                    }
                    else {
                        if(contacts.isEmpty()){
                            showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
                        }
                        else{
                            Intent in = new Intent(this, AddContactActivityLollipop.class);
                            in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
                            startActivityForResult(in, Constants.REQUEST_CREATE_CHAT);
                        }
                    }
                }
                else{
                    log("Online but not megaApi");
                    Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        log("-------------------onActivityResult " + requestCode + "____" + resultCode);

        if (requestCode == Constants.REQUEST_CREATE_CHAT && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_CREATE_CHAT OK");

            if (intent == null) {
                log("Return.....");
                return;
            }

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);

            if (contactsData != null){
                if(contactsData.size()==1){
                    MegaUser user = megaApi.getContact(contactsData.get(0));
                    if(user!=null){
                        log("Chat with contact: "+contactsData.size());
                        startOneToOneChat(user);
                    }
                }
                else{
                    log("Create GROUP chat");
                    MegaChatPeerList peers = MegaChatPeerList.createInstance();
                    for (int i=0; i<contactsData.size(); i++){
                        MegaUser user = megaApi.getContact(contactsData.get(i));
                        if(user!=null){
                            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                        }
                    }
                    log("create group chat with participants: "+peers.size());

                    final String chatTitle = intent.getStringExtra(AddContactActivityLollipop.EXTRA_CHAT_TITLE);
                    final boolean isEKR = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_EKR, false);
                    if (isEKR) {
                        megaChatApi.createChat(true, peers, chatTitle, this);
                    }
                    else {
                        final boolean chatLink = intent.getBooleanExtra(AddContactActivityLollipop.EXTRA_CHAT_LINK, false);

                        if(chatLink){
                            if(chatTitle!=null && !chatTitle.isEmpty()){
                                CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, chatTitle);
                                megaChatApi.createPublicChat(peers, chatTitle, listener);
                            }
                            else{
                                Util.showAlert(this, getString(R.string.message_error_set_title_get_link), null);
                            }
                        }
                        else{
                            megaChatApi.createPublicChat(peers, chatTitle, this);
                        }
                    }
                }
            }
        }
    }

    public void startOneToOneChat(MegaUser user){
        log("startOneToOneChat");
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(user.getHandle());
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if(chat==null){
            log("No chat, create it!");
            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        }
        else{
            log("There is already a chat, open it!");
            showSnackbar(getString(R.string.chat_already_exists));
        }
    }

    public void showSnackbar(String s){
        log("showSnackbar: "+s);
        Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    public void chooseChats(ArrayList<MegaChatListItem> chatListItems){
        log("chooseChats");

        long[] longArray = new long[chatListItems.size()];
        for (int i=0; i<chatListItems.size(); i++){
            longArray[i] = chatListItems.get(i).getChatId();
        }

        Intent intent = new Intent();
        intent.putExtra("SELECTED_CHATS", longArray);

        if(nodeHandles!=null){
            intent.putExtra("NODE_HANDLES", nodeHandles);
        }

        if(userHandles!=null){
            intent.putExtra("USER_HANDLES", userHandles);
        }

        if(messagesIds!=null){
            intent.putExtra("ID_MESSAGES", messagesIds);
        }

        setResult(RESULT_OK, intent);
        log("finish!");
        finish();
    }

    public void showFabButton(boolean show){
        if(show){
            fab.setVisibility(View.VISIBLE);
        }
        else{
            fab.setVisibility(View.GONE);
        }
    }

    public static void log(String log) {
        Util.log("ChatExplorerActivity", log);
    }

    @Override
    public void onClick(View v) {
        log("onClick");

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        switch(v.getId()) {
            case R.id.fab_chat_explorer: {
                if(chatExplorerFragment!=null){
                    if(chatExplorerFragment.getSelectedChats()!=null){
                        chooseChats(chatExplorerFragment.getSelectedChats());
                    }
                }
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

       if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
            log("Create chat request finish.");
           onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), false);
        }
    }

    public void onRequestFinishCreateChat(int errorCode, long chatHandle, boolean publicLink){
        log("onRequestFinishCreateChat");

        if(errorCode==MegaChatError.ERROR_OK){
            log("Chat CREATED.");

            //Update chat view
            if(chatExplorerFragment!=null && chatExplorerFragment.isAdded()){
                chatExplorerFragment.setChats();
            }
        }
        else{
            log("EEEERRRRROR WHEN CREATING CHAT " + errorCode);
            showSnackbar(getString(R.string.create_chat_error));
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}
