package mega.privacy.android.app.lollipop.megachat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.lollipop.listeners.MultipleGroupChatRequestListener;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaParticipantsChatLollipopAdapter;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ParticipantBottomSheetDialogFragment;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet.isBottomSheetDialogShown;


public class GroupChatInfoActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatListenerInterface, View.OnClickListener, MegaRequestListenerInterface, AdapterView.OnItemClickListener {

    GroupChatInfoActivityLollipop groupChatInfoActivity;

    public long chatHandle;
    MegaChatRoom chat;

    public long selectedHandleParticipant;

    AlertDialog permissionsDialog;
    AlertDialog changeTitleDialog;
    AlertDialog chatLinkDialog;

    MenuItem addParticipantItem;
    MenuItem changeTitleItem;

    private MegaApiAndroid megaApi = null;
    MegaChatApiAndroid megaChatApi = null;

    Display display;
    DisplayMetrics outMetrics;
    float density;
    float scaleW;
    float scaleH;
    float scaleText;

    DatabaseHandler dbH = null;
    ChatItemPreferences chatPrefs = null;
    ChatSettings chatSettings = null;
    boolean generalChatNotifications = true;

    CoordinatorLayout fragmentContainer;
    TextView initialLetter;
    RoundedImageView avatarImageView;

    android.support.v4.widget.NestedScrollView scrollView;

    LinearLayout infoLayout;
    RelativeLayout avatarLayout;
    TextView infoTitleChatText;
    TextView infoNumParticipantsText;
    RelativeLayout infoTextContainerLayout;
    ImageView editImageView;

    LinearLayout notificationsLayout;
    SwitchCompat notificationsSwitch;
    TextView notificationsTitle;
    TextView notificationSelectedText;
    View dividerNotifications;

    LinearLayout chatLinkLayout;
    TextView chatLinkTitleText;
    View chatLinkSeparator;
    String chatLink;

    LinearLayout privateLayout;
    View privateSeparator;

    RelativeLayout sharedFilesLayout;
    View dividerSharedFilesLayout;

    RelativeLayout clearChatLayout;
    View dividerClearLayout;
    RelativeLayout leaveChatLayout;
    View dividerLeaveLayout;

    RelativeLayout archiveChatLayout;
    TextView archiveChatTitle;
    ImageView archiveChatIcon;
    View archiveChatSeparator;

    RelativeLayout observersLayout;
    TextView observersNumberText;
    View observersSeparator;

    TextView participantsTitle;
    long participantsCount;

    RecyclerView recyclerView;
    MegaParticipantsChatLollipopAdapter adapter;
    ArrayList<MegaChatParticipant> participants;

    Toolbar toolbar;
    ActionBar aB;

    private ParticipantBottomSheetDialogFragment bottomSheetDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        log("onCreate");
        groupChatInfoActivity = this;
        if (megaApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaApi = app.getMegaApi();
        }

        if (megaChatApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaChatApi = app.getMegaChatApi();
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

        log("addChatListener");
        megaChatApi.addChatListener(this);

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        if (scaleH < scaleW){
            scaleText = scaleH;
        }
        else{
            scaleText = scaleW;
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            chatHandle = extras.getLong("handle", -1);
            if (chatHandle == -1) {
                finish();
                return;
            }

            chat = megaChatApi.getChatRoom(chatHandle);

            if(chat==null){
                log("Chatroom NULL cannot be recovered");
                finish();
                return;
            }

            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
            chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));

            setContentView(R.layout.activity_group_chat_properties);

            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

            fragmentContainer = (CoordinatorLayout) findViewById(R.id.fragment_container_group_chat);
            toolbar = (Toolbar) findViewById(R.id.toolbar_group_chat_properties);
            setSupportActionBar(toolbar);
            aB = getSupportActionBar();

            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);

            aB.setTitle(getString(R.string.group_chat_info_label).toUpperCase());

            scrollView = (android.support.v4.widget.NestedScrollView) findViewById(R.id.scroll_view_group_chat_properties);
            new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (scrollView.canScrollVertically(-1)){
                        changeActionBarElevation(true);
                    }
                    else {
                        changeActionBarElevation(false);
                    }
                }
            });

            //Info Layout
            avatarImageView = (RoundedImageView) findViewById(R.id.chat_group_properties_thumbnail);
            initialLetter = (TextView) findViewById(R.id.chat_group_properties_initial_letter);

            if (chat.getTitle().length() > 0){
                String chatTitle = chat.getTitle().trim();

                String firstLetter = "";
                if(!chatTitle.isEmpty()){
                    firstLetter = chatTitle.charAt(0) + "";
                    firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                }

                initialLetter.setText(firstLetter);
            }

            createGroupChatAvatar();

            infoLayout = (LinearLayout) findViewById(R.id.chat_group_contact_properties_info_layout);
            infoLayout.setVisibility(View.VISIBLE);
            avatarLayout = (RelativeLayout) findViewById(R.id.chat_group_properties_avatar_layout);

            infoTextContainerLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_info_text_container);
            LinearLayout.LayoutParams paramsInfoText = (LinearLayout.LayoutParams) infoTextContainerLayout.getLayoutParams();
            paramsInfoText.leftMargin = Util.scaleWidthPx(16, outMetrics);
            infoTextContainerLayout.setLayoutParams(paramsInfoText);

            infoTitleChatText = (TextView) findViewById(R.id.chat_group_contact_properties_info_title);
            log("The full title of chat: "+chat.getTitle());
            infoTitleChatText.setText(chat.getTitle());
            infoNumParticipantsText = (TextView) findViewById(R.id.chat_group_contact_properties_info_participants);

            infoTitleChatText.setMaxWidth(Util.scaleWidthPx(240, outMetrics));

            editImageView = (ImageView) findViewById(R.id.chat_group_contact_properties_edit_icon);
            RelativeLayout.LayoutParams paramsEditIcon = (RelativeLayout.LayoutParams) editImageView.getLayoutParams();
            paramsEditIcon.leftMargin = Util.scaleWidthPx(8, outMetrics);
            editImageView.setLayoutParams(paramsEditIcon);
            editImageView.setOnClickListener(this);

            //Notifications Layout

            notificationsLayout = (LinearLayout) findViewById(R.id.chat_group_contact_properties_notifications_layout);
            notificationsLayout.setVisibility(View.VISIBLE);

            notificationsTitle = (TextView) findViewById(R.id.chat_group_contact_properties_notifications_title);
//            notificationSelectedText = (TextView) findViewById(R.id.chat_group_contact_properties_notifications_option);

            notificationsSwitch = (SwitchCompat) findViewById(R.id.chat_group_contact_properties_switch);
            notificationsSwitch.setOnClickListener(this);

            dividerNotifications = (View) findViewById(R.id.divider_notifications_layout);

            //Chat links
            chatLinkLayout = (LinearLayout) findViewById(R.id.chat_group_contact_properties_chat_link_layout);
            chatLinkTitleText = (TextView) findViewById(R.id.chat_group_contact_properties_chat_link);
            chatLinkSeparator = (View) findViewById(R.id.divider_chat_link_layout);

            //Private chat
            privateLayout = (LinearLayout) findViewById(R.id.chat_group_contact_properties_private_layout);
            privateSeparator = (View) findViewById(R.id.divider_private_layout);

            //Chat Shared Files Layout

            sharedFilesLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_chat_files_shared_layout);
            sharedFilesLayout.setOnClickListener(this);

            dividerSharedFilesLayout = (View) findViewById(R.id.divider_chat_files_shared_layout);

            //Clear chat Layout
            clearChatLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_clear_layout);
            clearChatLayout.setOnClickListener(this);

            dividerClearLayout = (View) findViewById(R.id.divider_clear_layout);

            //Archive chat Layout
            archiveChatLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_archive_layout);
            archiveChatLayout.setOnClickListener(this);

            archiveChatSeparator = (View) findViewById(R.id.divider_archive_layout);

            archiveChatTitle = (TextView) findViewById(R.id.chat_group_contact_properties_archive);
            archiveChatIcon = (ImageView) findViewById(R.id.chat_group_contact_properties_archive_icon);

            //Leave chat Layout
            leaveChatLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_leave_layout);
            leaveChatLayout.setOnClickListener(this);
            dividerLeaveLayout = (View) findViewById(R.id.divider_leave_layout);

            //Observers layout
            observersLayout = (RelativeLayout) findViewById(R.id.chat_group_observers_layout);
            observersNumberText = (TextView) findViewById(R.id.chat_group_observers_number_text);
            observersSeparator = (View) findViewById(R.id.divider_observers_layout);

            participantsTitle = (TextView) findViewById(R.id.chat_group_contact_properties_title_text);

            recyclerView = (RecyclerView) findViewById(R.id.chat_group_contact_properties_list);
//            recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setFocusable(false);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setNestedScrollingEnabled(false);

            if(chat.isPreview()){
                notificationsLayout.setVisibility(View.GONE);
                dividerNotifications.setVisibility(View.GONE);
                chatLinkLayout.setVisibility(View.GONE);
                chatLinkSeparator.setVisibility(View.GONE);
                privateLayout.setVisibility(View.GONE);
                privateSeparator.setVisibility(View.GONE);
                clearChatLayout.setVisibility(View.GONE);
                dividerClearLayout.setVisibility(View.GONE);
                archiveChatLayout.setVisibility(View.GONE);
                archiveChatSeparator.setVisibility(View.GONE);
                leaveChatLayout.setVisibility(View.GONE);
                dividerLeaveLayout.setVisibility(View.GONE);
                editImageView.setVisibility(View.GONE);
            }
            else{
                setChatPermissions();

                if(chat.isArchived()){
                    archiveChatTitle.setText(getString(R.string.general_unarchive));
                    archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_unarchive));
                }
                else{
                    archiveChatTitle.setText(getString(R.string.general_archive));
                    archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_archive));
                }

                chatSettings = dbH.getChatSettings();
                if(chatSettings==null){
                    log("Chat settings null - notifications ON");
                    setUpIndividualChatNotifications();
                }
                else {
                    log("There is chat settings");
                    if (chatSettings.getNotificationsEnabled() == null) {
                        generalChatNotifications = true;

                    } else {
                        generalChatNotifications = Boolean.parseBoolean(chatSettings.getNotificationsEnabled());

                    }

                    if (generalChatNotifications) {
                        setUpIndividualChatNotifications();
                    } else {
                        log("General notifications OFF");
                        notificationsSwitch.setChecked(false);
                    }
                }
            }

            if(megaChatApi.isSignalActivityRequired()){
                megaChatApi.signalPresenceActivity();
            }

            //Set participants
            participants = new ArrayList<>();

            setParticipants();

            updatePreviewers();
        }
    }

    public void updatePreviewers(){
        log("updatePreviewers");

        if(chat.getNumPreviewers()<1){
            observersSeparator.setVisibility(View.GONE);
            observersLayout.setVisibility(View.GONE);
        }
        else{
            observersSeparator.setVisibility(View.VISIBLE);
            observersLayout.setVisibility(View.VISIBLE);
            observersNumberText.setText(chat.getNumPreviewers()+"");
        }
    }

    public void changeActionBarElevation(boolean whitElevation){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (whitElevation) {
                aB.setElevation(Util.px2dp(4, outMetrics));
            }
            else {
                aB.setElevation(0);
            }
        }
    }

    public void setUpIndividualChatNotifications(){
        log("setUpIndividualChatNotifications");
        //SET Preferences (if exist)
        if(chatPrefs!=null){
            log("There is individual chat preferences");
            boolean notificationsEnabled = true;
            if (chatPrefs.getNotificationsEnabled() != null){
                notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
            }
            notificationsSwitch.setChecked(notificationsEnabled);

        }
        else{
            log("NO individual chat preferences");
            notificationsSwitch.setChecked(true);
        }
    }

    public void setChatPermissions(){
        log("setChatPermissions");

        if(chat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
            editImageView.setVisibility(View.VISIBLE);
            dividerClearLayout.setVisibility(View.VISIBLE);
            clearChatLayout.setVisibility(View.VISIBLE);
            dividerLeaveLayout.setVisibility(View.VISIBLE);

            if(chat.isPublic()){
                privateLayout.setVisibility(View.VISIBLE);
                privateLayout.setOnClickListener(this);
                privateSeparator.setVisibility(View.VISIBLE);
            }
            else{
                log("Private chat");
                privateLayout.setVisibility(View.GONE);
                privateSeparator.setVisibility(View.GONE);
            }
        }
        else{
            editImageView.setVisibility(View.GONE);
            dividerClearLayout.setVisibility(View.GONE);
            clearChatLayout.setVisibility(View.GONE);
            privateLayout.setVisibility(View.GONE);
            privateSeparator.setVisibility(View.GONE);

            if(chat.getOwnPrivilege()<MegaChatRoom.PRIV_RO){
                leaveChatLayout.setVisibility(View.GONE);
                dividerLeaveLayout.setVisibility(View.GONE);
            }
        }

        if (chat.isPublic() && chat.getOwnPrivilege() >= MegaChatRoom.PRIV_RO) {
            chatLinkLayout.setVisibility(View.VISIBLE);
            chatLinkLayout.setOnClickListener(this);
            chatLinkSeparator.setVisibility(View.VISIBLE);
        }
        else {
            chatLinkLayout.setVisibility(View.GONE);
            chatLinkSeparator.setVisibility(View.GONE);
            chatLink = null;
        }
    }

    @Override
    protected void onDestroy(){
        log("onDestroy()");

        if (megaChatApi != null) {
            megaChatApi.removeChatListener(this);
        }

        super.onDestroy();
    }

    public void setParticipants(){
        log("setParticipants: "+participants.size());
        //Set the first element = me
        participantsCount = chat.getPeerCount();
        log("Participants count: "+participantsCount);
        if (chat.isPreview()) {
            infoNumParticipantsText.setText(getString(R.string.number_of_participants, participantsCount));
        }
        else {
            long participantsLabel = participantsCount+1; //Add one to include me
            infoNumParticipantsText.setText(getString(R.string.number_of_participants, participantsLabel));
        }

        for(int i=0;i<participantsCount;i++){
            int peerPrivilege = chat.getPeerPrivilege(i);
            if(peerPrivilege==MegaChatRoom.PRIV_RM){
                log("Continue");
                continue;
            }

            long peerHandle = chat.getPeerHandle(i);

            String fullName = getParticipantFullName(i);
            String participantEmail = chat.getPeerEmail(i);

            log(i+"FullName of the peer: "+fullName + "email " + chat.getPeerEmail(i) + " privilege: "+peerPrivilege);

            MegaChatParticipant participant = new MegaChatParticipant(peerHandle, "", "", fullName, participantEmail, peerPrivilege);

            participants.add(participant);

            int userStatus = megaChatApi.getUserOnlineStatus(participant.getHandle());
            if(userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID){
                log("Request last green for user");
                megaChatApi.requestLastGreen(participant.getHandle(), null);
            }
        }

        if(!chat.isPreview()){
            log("Is not preview - add me as participant");
            String myFullName =  megaChatApi.getMyFullname();
            if(myFullName!=null){
                if(myFullName.trim().isEmpty()){
                    myFullName =  megaChatApi.getMyEmail();
                }
            }
            else{
                myFullName =  megaChatApi.getMyEmail();
            }

            MegaChatParticipant me = new MegaChatParticipant(megaChatApi.getMyUserHandle(), null, null, getString(R.string.chat_me_text_bracket, myFullName), megaChatApi.getMyEmail(), chat.getOwnPrivilege());

            participants.add(me);
        }

        log("number of participants with me: "+participants.size());
        if (adapter == null){
            if(chat.isPreview()){
                adapter = new MegaParticipantsChatLollipopAdapter(this, participants, recyclerView, true);
            }
            else{
                adapter = new MegaParticipantsChatLollipopAdapter(this, participants, recyclerView, false);
            }

            adapter.setHasStableIds(true);
            adapter.setPositionClicked(-1);
            recyclerView.setAdapter(adapter);
        }
        else{
            adapter.setParticipants(participants);
        }
    }

    public String getParticipantFullName(long i){

        String fullName = chat.getPeerFullname(i);

        if(fullName!=null){
            if(fullName.isEmpty()){
                log("1-Put email as fullname");
                fullName = chat.getPeerEmail(i);
                return fullName;
            }
            else{
                if (fullName.trim().length() <= 0){
                    log("2-Put email as fullname");
                    fullName = chat.getPeerEmail(i);
                    return fullName;
                }
                else{
                    return fullName;
                }
            }
        }
        else{
            log("3-Put email as fullname");
            fullName = chat.getPeerEmail(i);
            return fullName;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenuLollipop");

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_group_chat_info, menu);

        addParticipantItem = menu.findItem(R.id.action_add_participants);
        changeTitleItem =menu.findItem(R.id.action_rename);

        int permission = chat.getOwnPrivilege();
        log("Permision: "+permission);

        if(permission==MegaChatRoom.PRIV_MODERATOR) {
            addParticipantItem.setVisible(true);
            changeTitleItem.setVisible(true);
        }
        else {
            addParticipantItem.setVisible(false);
            changeTitleItem.setVisible(false);
        }

        log("Call to super onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.action_add_participants:{
                //
                chooseAddParticipantDialog();
                break;
            }
            case R.id.action_rename:{
                showRenameGroupDialog(false);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void chooseAddParticipantDialog(){
        log("chooseAddContactDialog");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if(megaApi!=null && megaApi.getRootNode()!=null){
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if(contacts==null){
                showSnackbar(getString(R.string.no_contacts_invite));
            }
            else {
                if(contacts.isEmpty()){
                    showSnackbar(getString(R.string.no_contacts_invite));
                }
                else{
                    Intent in = new Intent(this, AddContactActivityLollipop.class);
                    in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
                    in.putExtra("chat", true);
                    in.putExtra("chatId", chatHandle);
                    in.putExtra("aBtitle", getString(R.string.add_participants_menu_item));
                    startActivityForResult(in, Constants.REQUEST_ADD_PARTICIPANTS);
                }
            }
        }
        else{
            log("Online but not megaApi");
            Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void showRemoveParticipantConfirmation (long handle, MegaChatRoom chatToChange){
        log("showRemoveParticipantConfirmation");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:{
                        removeParticipant();
                        break;
                    }

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(groupChatInfoActivity, R.style.AppCompatAlertDialogStyle);
        String name = chatToChange.getPeerFullnameByHandle(handle);
        String message= getResources().getString(R.string.confirmation_remove_chat_contact,name);
        builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();

    }

    public void removeParticipant(){
        log("removeParticipant: "+selectedHandleParticipant);
        log("before remove, participants: "+chat.getPeerCount());
        ChatController cC = new ChatController(groupChatInfoActivity);
        cC.removeParticipant(chatHandle, selectedHandleParticipant);
    }

    public void changeTitle(String title){
        log("changeTitle: "+title);
        ChatController cC = new ChatController(groupChatInfoActivity);
        cC.changeTitle(chatHandle, title);
    }

    public void showChangePermissionsDialog(long handle, MegaChatRoom chatToChange){
        //Change permissions

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        final long handleToChange = handle;

        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.change_permissions_dialog, null);

        final LinearLayout administratorLayout = (LinearLayout) dialoglayout.findViewById(R.id.change_permissions_dialog_administrator_layout);
        final CheckedTextView administratorCheck = (CheckedTextView) dialoglayout.findViewById(R.id.change_permissions_dialog_administrator);
        administratorCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams administratorMLP = (ViewGroup.MarginLayoutParams) administratorCheck.getLayoutParams();
        administratorMLP.setMargins(Util.scaleWidthPx(17, outMetrics), 0, 0, 0);

        final TextView administratorTitle = (TextView) dialoglayout.findViewById(R.id.administrator_title);
        administratorTitle.setText(getString(R.string.administrator_permission_label_participants_panel));
        administratorTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView administratorSubtitle = (TextView) dialoglayout.findViewById(R.id.administrator_subtitle);
        administratorSubtitle.setText(getString(R.string.file_properties_shared_folder_full_access));
        administratorSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout administratorTextLayout = (LinearLayout) dialoglayout.findViewById(R.id.administrator_text_layout);
        ViewGroup.MarginLayoutParams administratorSubtitleMLP = (ViewGroup.MarginLayoutParams) administratorTextLayout.getLayoutParams();
        administratorSubtitleMLP.setMargins(Util.scaleHeightPx(10, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(15, outMetrics));

        final LinearLayout memberLayout = (LinearLayout) dialoglayout.findViewById(R.id.change_permissions_dialog_member_layout);
        final CheckedTextView memberCheck = (CheckedTextView) dialoglayout.findViewById(R.id.change_permissions_dialog_member);
        memberCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams memberMLP = (ViewGroup.MarginLayoutParams) memberCheck.getLayoutParams();
        memberMLP.setMargins(Util.scaleWidthPx(17, outMetrics), 0, 0, 0);

        final TextView memberTitle = (TextView) dialoglayout.findViewById(R.id.member_title);
        memberTitle.setText(getString(R.string.standard_permission_label_participants_panel));
        memberTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView memberSubtitle = (TextView) dialoglayout.findViewById(R.id.member_subtitle);
        memberSubtitle.setText(getString(R.string.file_properties_shared_folder_read_write));
        memberSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout memberTextLayout = (LinearLayout) dialoglayout.findViewById(R.id.member_text_layout);
        ViewGroup.MarginLayoutParams memberSubtitleMLP = (ViewGroup.MarginLayoutParams) memberTextLayout.getLayoutParams();
        memberSubtitleMLP.setMargins(Util.scaleHeightPx(10, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(15, outMetrics));

        final LinearLayout observerLayout = (LinearLayout) dialoglayout.findViewById(R.id.change_permissions_dialog_observer_layout);
        final CheckedTextView observerCheck = (CheckedTextView) dialoglayout.findViewById(R.id.change_permissions_dialog_observer);
        observerCheck.setCompoundDrawablePadding(Util.scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams observerMLP = (ViewGroup.MarginLayoutParams) observerCheck.getLayoutParams();
        observerMLP.setMargins(Util.scaleWidthPx(17, outMetrics), 0, 0, 0);

        final TextView observerTitle = (TextView) dialoglayout.findViewById(R.id.observer_title);
        observerTitle.setText(getString(R.string.observer_permission_label_participants_panel));
        observerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView observerSubtitle = (TextView) dialoglayout.findViewById(R.id.observer_subtitle);
        observerSubtitle.setText(getString(R.string.subtitle_read_only_permissions));
        observerSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout observerTextLayout = (LinearLayout) dialoglayout.findViewById(R.id.observer_text_layout);
        ViewGroup.MarginLayoutParams observerSubtitleMLP = (ViewGroup.MarginLayoutParams) observerTextLayout.getLayoutParams();
        observerSubtitleMLP.setMargins(Util.scaleHeightPx(10, outMetrics), Util.scaleHeightPx(15, outMetrics), 0, Util.scaleHeightPx(15, outMetrics));

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setView(dialoglayout);

        builder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
        permissionsDialog = builder.create();

        permissionsDialog.show();

        int permission = chatToChange.getPeerPrivilegeByHandle(handleToChange);

        if(permission==MegaChatRoom.PRIV_STANDARD) {
            administratorCheck.setChecked(false);
            memberCheck.setChecked(true);
            observerCheck.setChecked(false);
        }
        else if(permission==MegaChatRoom.PRIV_MODERATOR){
            administratorCheck.setChecked(true);
            memberCheck.setChecked(false);
            observerCheck.setChecked(false);
        }
        else{
            administratorCheck.setChecked(false);
            memberCheck.setChecked(false);
            observerCheck.setChecked(true);
        }

        final AlertDialog dialog = permissionsDialog;

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.change_permissions_dialog_administrator_layout: {
                        changePermissions(MegaChatRoom.PRIV_MODERATOR);
                        break;
                    }
                    case R.id.change_permissions_dialog_member_layout: {
                        changePermissions(MegaChatRoom.PRIV_STANDARD);
                        break;
                    }
                    case R.id.change_permissions_dialog_observer_layout: {
                        changePermissions(MegaChatRoom.PRIV_RO);
                        break;
                    }
                }
                if (dialog != null){
                    dialog.dismiss();
                }
            }
        };
        administratorLayout.setOnClickListener(clickListener);
        memberLayout.setOnClickListener(clickListener);
        observerLayout.setOnClickListener(clickListener);

        log("Change permissions");
    }

    public void changePermissions(int newPermissions){
        log("changePermissions: "+newPermissions);
        ChatController cC = new ChatController(groupChatInfoActivity);
        cC.alterParticipantsPermissions(chatHandle, selectedHandleParticipant, newPermissions);
    }

    public void createGroupChatAvatar(){
        log("createGroupChatAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(ContextCompat.getColor(this,R.color.divider_upgrade_account));

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        avatarImageView.setImageBitmap(defaultAvatar);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float density  = getResources().getDisplayMetrics().density;


        int avatarTextSize = getAvatarTextSize(density);
        log("DENSITY: " + density + ":::: " + avatarTextSize);


        String firstLetter = initialLetter.getText().toString();

        if(firstLetter.trim().isEmpty()){
            initialLetter.setVisibility(View.INVISIBLE);
        }
        else{
            if(firstLetter.equals("(")){
                initialLetter.setVisibility(View.INVISIBLE);
            }
            else{
                initialLetter.setText(firstLetter);
                initialLetter.setTextColor(Color.WHITE);
                initialLetter.setVisibility(View.VISIBLE);
                initialLetter.setTextSize(24);
            }
        }
    }

    private int getAvatarTextSize (float density){
        float textSize = 0.0f;

        if (density > 3.0){
            textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
        }
        else if (density > 2.0){
            textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
        }
        else if (density > 1.5){
            textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
        }
        else if (density > 1.0){
            textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
        }
        else if (density > 0.75){
            textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
        }
        else{
            textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f);
        }

        return (int)textSize;
    }

    public void showParticipantsPanel(MegaChatParticipant participant){
        log("showParticipantsPanel");

        if (participant == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedHandleParticipant = participant.getHandle();
        bottomSheetDialogFragment = new ParticipantBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public MegaChatRoom getChat() {
        return chat;
    }

    public void setChat(MegaChatRoom chat) {
        this.chat = chat;
    }

    @Override
    public void onClick(View view) {
        log("onClick");

        switch (view.getId()) {

            case R.id.chat_group_contact_properties_edit_icon: {
                showRenameGroupDialog(false);
                break;
            }
            case R.id.chat_group_contact_properties_leave_layout: {
                showConfirmationLeaveChat(chat);
                break;
            }
            case R.id.chat_group_contact_properties_clear_layout: {
                log("Clear chat option");
                showConfirmationClearChat();
                break;
            }
            case R.id.chat_group_contact_properties_archive_layout:{
                ChatController chatC = new ChatController(this);
                chatC.archiveChat(chat);
                break;
            }
            case R.id.chat_group_contact_properties_switch:{
                log("click on switch notifications");
                if(!generalChatNotifications){
                    notificationsSwitch.setChecked(false);
                    showSnackbar("The chat notifications are disabled, go to settings to set up them");
                }
                else{
                    boolean enabled = notificationsSwitch.isChecked();

                    ChatController chatC = new ChatController(this);
                    if(enabled){
                        chatC.unmuteChat(chatHandle);
                    }
                    else{
                        chatC.muteChat(chatHandle);
                    }
                }

                break;

            }
            case R.id.chat_group_contact_properties_chat_link_layout:{
                megaChatApi.queryChatLink(chatHandle, groupChatInfoActivity);
                break;
            }
            case R.id.chat_group_contact_properties_private_layout: {
                log("Make chat private");
                showConfirmationPrivateChatDialog();
                break;
            }
            case R.id.chat_group_contact_properties_chat_files_shared_layout:{
                Intent nodeHistoryIntent = new Intent(this, NodeAttachmentHistoryActivity.class);
                if(chat!=null){
                    nodeHistoryIntent.putExtra("chatId", chat.getChatId());
                }
                startActivity(nodeHistoryIntent);
                break;
            }
        }

    }

    public void copyLink(){
        log("copyLink");
        if(chatLink!=null) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", chatLink);
            clipboard.setPrimaryClip(clip);
            showSnackbar(getString(R.string.chat_link_copied_clipboard));
        }
        else {
            showSnackbar(getString(R.string.general_text_error));
        }
    }

    public void removeChatLink(){
        log("removeChatLink");
        megaChatApi.removeChatLink(chatHandle, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        log("onActivityResult, resultCode: "+resultCode);

       if (requestCode == Constants.REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_ADD_PARTICIPANTS OK");

            if (intent == null) {
                log("Return.....");
                return;
            }

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
            MultipleGroupChatRequestListener multipleListener = null;

            if (contactsData != null) {

                if (contactsData.size() == 1) {
                    MegaUser user = megaApi.getContact(contactsData.get(0));
                    if (user != null) {
                        megaChatApi.inviteToChat(chatHandle, user.getHandle(), MegaChatPeerList.PRIV_STANDARD, this);
                    }
                } else {
                    log("Add multiple participants "+contactsData.size());
                    multipleListener = new MultipleGroupChatRequestListener(this);
                    for (int i = 0; i < contactsData.size(); i++) {
                        MegaUser user = megaApi.getContact(contactsData.get(i));
                        if (user != null) {
                            megaChatApi.inviteToChat(chatHandle, user.getHandle(), MegaChatPeerList.PRIV_STANDARD, multipleListener);
                        }
                    }
                }
            }
        }
        else{
            log("Error onActivityResult: REQUEST_ADD_PARTICIPANTS");
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    public void showRenameGroupDialog(boolean fromGetLink){
        log("showRenameGroupDialog");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        if(fromGetLink){
            final TextView alertRename = new TextView(this);
            alertRename.setText(getString(R.string.message_error_set_title_get_link));

            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsText.setMargins(Util.scaleWidthPx(24, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(12, outMetrics), 0);
            alertRename.setLayoutParams(paramsText);
            layout.addView(alertRename);

            params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(8, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

        }
        else{
            params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(16, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);
        }

        final EditText input = new EditText(this);
        int maxLength = 30;
        input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});

        layout.addView(input, params);

        input.setSingleLine();
        input.setText(chat.getTitle());
        input.setSelectAllOnFocus(true);
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String title = input.getText().toString();
                    if(title.equals("")||title.isEmpty()){
                        log("input is empty");
                        input.setError(getString(R.string.invalid_string));
                        input.requestFocus();
                    }
                    else if(title.trim().isEmpty()){
                        log("title trim is empty");
                        input.setError(getString(R.string.invalid_string));
                        input.requestFocus();
                    }
                    else {
                        log("action DONE ime - change title");
                        changeTitle(title);
                        changeTitleDialog.dismiss();
                    }
                }
                else{
                    log("other IME" + actionId);
                }
                return false;
            }
        });
        input.setImeActionLabel(getString(R.string.context_rename),EditorInfo.IME_ACTION_DONE);
        builder.setTitle(getString(R.string.context_rename));
        builder.setPositiveButton(getString(R.string.context_rename),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String title = input.getText().toString();
                        if(title.equals("")||title.isEmpty()){
                            log("input is empty");
                            input.setError(getString(R.string.invalid_string));
                            input.requestFocus();
                        }
                        else if(title.trim().isEmpty()){
                            log("title trim is empty");
                            input.setError(getString(R.string.invalid_string));
                            input.requestFocus();
                        }
                        else {
                            log("positive button pressed - change title");
                            changeTitle(title);
                            changeTitleDialog.dismiss();
                        }
                    }
                });


        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Util.hideKeyboard(groupChatInfoActivity, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setView(layout);
        changeTitleDialog = builder.create();
        changeTitleDialog.show();

        changeTitleDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("OK BTTN CHANGE");
                String title = input.getText().toString();
                if(title.equals("")||title.isEmpty()){
                    log("input is empty");
                    input.setError(getString(R.string.invalid_string));
                    input.requestFocus();
                }
                else if(title.trim().isEmpty()){
                    log("title trim is empty");
                    input.setError(getString(R.string.invalid_string));
                    input.requestFocus();
                }
                else {
                    log("positive button pressed - change title");
                    changeTitle(title);
                    changeTitleDialog.dismiss();
                }
            }
        });

    }

    public void showConfirmationClearChat(){
        log("showConfirmationClearChat");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        log("Clear chat!");
//						megaChatApi.truncateChat(chatHandle, MegaChatHandle.MEGACHAT_INVALID_HANDLE);
                        log("Clear history selected!");
                        ChatController chatC = new ChatController(groupChatInfoActivity);
                        chatC.clearHistory(chat);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message= getResources().getString(R.string.confirmation_clear_group_chat);
        builder.setTitle(R.string.title_confirmation_clear_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_clear, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationLeaveChat (final MegaChatRoom c){
        log("showConfirmationLeaveChat");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        ChatController chatC = new ChatController(groupChatInfoActivity);
                        chatC.leaveChat(c);
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
        String message= getResources().getString(R.string.confirmation_leave_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void inviteContact (String email){
        log("inviteContact");

        ContactController cC = new ContactController(this);
        cC.inviteContact(email);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish CHAT: "+request.getType()+ " "+e.getErrorCode());

        if(request.getType() == MegaChatRequest.TYPE_UPDATE_PEER_PERMISSIONS){
            log("Permissions changed");
            int index=-1;
            MegaChatParticipant participantToUpdate=null;

            log("Participants count: "+participantsCount);
            for(int i=0;i<participantsCount;i++){
                if(request.getUserHandle()==participants.get(i).getHandle()){
                    participantToUpdate = participants.get(i);

                    participantToUpdate.setPrivilege(request.getPrivilege());
                    index=i;
                    break;
                }
            }

            if(index!=-1 && participantToUpdate!=null){
                participants.set(index,participantToUpdate);
//                adapter.setParticipants(participants);
                adapter.updateParticipant(index+1, participants);
            }

        }
        else if(request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM){
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

            if(chat.isArchived()){
                archiveChatTitle.setText(getString(R.string.general_unarchive));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_b_unarchive));
            }
            else{
                archiveChatTitle.setText(getString(R.string.general_archive));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_b_archive));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM){
            log("Remove participant: "+request.getUserHandle());
            log("My user handle: "+megaChatApi.getMyUserHandle());

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getUserHandle()==-1){
                    log("I left the chatroom");
                    finish();
                }
                else{
                    log("Removed from chat");

                    chat = megaChatApi.getChatRoom(chatHandle);
                    log("Peers after onChatListItemUpdate: "+chat.getPeerCount());
//                chat = megaChatApi.getChatRoom(chatHandle);
                    participants.clear();
                    setParticipants();

//                    for(int i=0;i<participantsCount;i++){
//
//                        if(request.getUserHandle()==participants.get(i).getHandle()){
//                            participantToUpdate = participants.get(i);
//
//                            participantToUpdate.setPrivilege(request.getPrivilege());
//                            index=i;
//                            break;
//                        }
//                    }
//
//                    if(index!=-1&&participantToUpdate!=null){
//                        participants.remove(index);
//                        adapter.removeParticipant(index, participants);
//                    }

                    showSnackbar(getString(R.string.remove_participant_success));
                }
            }
            else{

                if(request.getUserHandle()==-1){
                    log("EEEERRRRROR WHEN LEAVING CHAT" + e.getErrorString());
                    showSnackbar("Error.Chat not left");
                }
                else{
                    log("EEEERRRRROR WHEN TYPE_REMOVE_FROM_CHATROOM " + e.getErrorString());
                    showSnackbar(getString(R.string.remove_participant_error));
                }
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_EDIT_CHATROOM_NAME) {
            log("Change title");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getText()!=null) {
                    log("NEW title: "+request.getText());
                    infoTitleChatText.setText(request.getText());
                }
            }
            else{

                log("EEEERRRRROR WHEN TYPE_EDIT_CHATROOM_NAME " + e.getErrorString());
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
            log("Truncate history request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Ok. Clear history done");
                showSnackbar(getString(R.string.clear_history_success));
            }
            else{
                log("Error clearing history: "+e.getErrorString());
                showSnackbar(getString(R.string.clear_history_error));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_INVITE_TO_CHATROOM) {
            log("Invite to chatroom request finish!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                log("Ok. Invited");
                showSnackbar(getString(R.string.add_participant_success));
                if(request.getChatHandle()==chatHandle){
                    log("Changes in my chat");
                    chat = megaChatApi.getChatRoom(chatHandle);
                    log("Peers after onChatListItemUpdate: "+chat.getPeerCount());
//                chat = megaChatApi.getChatRoom(chatHandle);
                    participants.clear();
                    setParticipants();
//                scrollView.invalidate();
//                scrollView.setFillViewport(false);
                }
                else{
                    log("Changes NOT interested in");
                }
//                chat = megaChatApi.getChatRoom(chatHandle);
//                participants.clear();
//                setParticipants(chat);
//                scrollView.invalidate();
//                scrollView.setFillViewport(false);
            }
            else if (e.getErrorCode() == MegaChatError.ERROR_EXIST){
                log("Error inviting ARGS: "+e.getErrorString());
                showSnackbar(getString(R.string.add_participant_error_already_exists));
            }
            else{
                log("Error inviting: "+e.getErrorString()+" "+e.getErrorCode());
                showSnackbar(getString(R.string.add_participant_error));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
            log("Create chat request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){

                log("open new chat");
                Intent intent = new Intent(this, ChatActivityLollipop.class);
                intent.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
                intent.putExtra("CHAT_ID", request.getChatHandle());
                this.startActivity(intent);
            }
            else{
                log("EEEERRRRROR WHEN CREATING CHAT " + e.getErrorString());
                showSnackbar(getString(R.string.create_chat_error));
            }
        }
        else if (request.getType() == MegaChatRequest.TYPE_CHAT_LINK_HANDLE){
            log("MegaChatRequest.TYPE_CHAT_LINK_HANDLE finished!!!");
            if(request.getFlag()==false){
                if(request.getNumRetry()==0){
//                    Query chat link
                    if(e.getErrorCode()==MegaChatError.ERROR_OK){
                        chatLink = request.getText();
                        ChatUtil.showShareChatLinkDialog(groupChatInfoActivity, chat, chatLink);
                        return;
                    }
                    else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                        log("The chatroom isn't grupal or public");
                    }
                    else if (e.getErrorCode()==MegaChatError.ERROR_NOENT){
                        log("The chatroom doesn't exist or the chatid is invalid");
                    }
                    else if(e.getErrorCode()==MegaChatError.ERROR_ACCESS){
                        log("The chatroom doesn't have a topic or the caller isn't an operator");
                    }
                    else{
                        log("Error TYPE_CHAT_LINK_HANDLE "+e.getErrorCode());
                    }
                    if (chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR){
                        if (chat.hasCustomTitle()) {
                            megaChatApi.createChatLink(chatHandle, groupChatInfoActivity);
                        }
                        else {
                            showRenameGroupDialog(true);
                        }
                    }
                    else {
                        showSnackbar(getString(R.string.no_chat_link_available));
                    }
                }
                else if(request.getNumRetry()==1){
//                    Create chat link
                    if(e.getErrorCode()==MegaChatError.ERROR_OK){
                        chatLink = request.getText();
                        ChatUtil.showShareChatLinkDialog(groupChatInfoActivity, chat, chatLink);
                    }
                    else{
                        log("Error TYPE_CHAT_LINK_HANDLE "+e.getErrorCode());
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                }
            }
            else{
                if(request.getNumRetry()==0){
                    log("Removing chat link");
                    if(e.getErrorCode()==MegaChatError.ERROR_OK){
                        chatLink = null;
                        showSnackbar(getString(R.string.chat_link_deleted));
                    }
                    else{
                        if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                            log("The chatroom isn't grupal or public");
                        }
                        else if (e.getErrorCode()==MegaChatError.ERROR_NOENT){
                            log("The chatroom doesn't exist or the chatid is invalid");
                        }
                        else if(e.getErrorCode()==MegaChatError.ERROR_ACCESS){
                            log("The chatroom doesn't have a topic or the caller isn't an operator");
                        }
                        else{
                            log("Error TYPE_CHAT_LINK_HANDLE "+e.getErrorCode());
                        }
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                }
            }

            updatePreviewers();

        }
        else if (request.getType() == MegaChatRequest.TYPE_SET_PRIVATE_MODE){
            log("MegaChatRequest.TYPE_SET_PRIVATE_MODE finished!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                chatLink = null;
                log("Chat is PRIVATE now");
                chatLinkLayout.setVisibility(View.GONE);
                chatLinkSeparator.setVisibility(View.GONE);
                privateLayout.setVisibility(View.GONE);
                privateSeparator.setVisibility(View.GONE);

                updatePreviewers();
            }
            else{
                log("Error on closeChatLink");
                if(e.getErrorCode()==MegaChatError.ERROR_ARGS){
                    log("NOT public chatroom");
                }
                else if(e.getErrorCode()==MegaChatError.ERROR_NOENT){
                    log("Chatroom not FOUND");
                }
                else if(e.getErrorCode()==MegaChatError.ERROR_ACCESS){
                    log("NOT privileges or private chatroom");
                }
                showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
            }
        }
    }

    public void showSnackbar(String s){
        showSnackbar(fragmentContainer, s);
    }


    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish "+request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
            log("MegaRequest.TYPE_INVITE_CONTACT finished: "+request.getNumber());

            if(request.getNumber()== MegaContactRequest.INVITE_ACTION_REMIND){
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    log("OK INVITE CONTACT: "+request.getEmail());
                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
                    {
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    }
                }
                else{
                    log("Code: "+e.getErrorString());
                    if(e.getErrorCode()==MegaError.API_EEXIST)
                    {
                        showSnackbar(getString(R.string.context_contact_already_invited, request.getEmail()));
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode()==MegaError.API_EARGS)
                    {
                        showSnackbar(getString(R.string.error_own_email_as_contact));
                    }
                    else{
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                    log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    public static void log(String message) {
        Util.log("GroupChatInfoActivity", message);
    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        log("onChatListItemUpdate");

        if(item.getChatId()==chatHandle){
            log("Changes in my chat");
            chat = megaChatApi.getChatRoom(chatHandle);

            if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)){
                log("Change participants");
                participants.clear();
                setParticipants();
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV)){
                log("listItemUpdate: Change status: MegaChatListItem.CHANGE_TYPE_OWN_PRIV");
                setChatPermissions();
                participants.clear();
                setParticipants();
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_TITLE)) {

                log("NEW title: "+chat.getTitle());
                infoTitleChatText.setText(chat.getTitle());

                if (chat.getTitle().length() > 0){
                    String chatTitle = chat.getTitle().trim();
                    String firstLetter = "";
                    if(!chatTitle.isEmpty()){
                        firstLetter = chatTitle.charAt(0) + "";
                        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                    }
                    initialLetter.setText(firstLetter);
                }

                createGroupChatAvatar();
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)) {
                log("CHANGE_TYPE_CLOSED");
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
                log("CHANGE_TYPE_UNREAD_COUNT");
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UPDATE_PREVIEWERS)){
                updatePreviewers();
            }
            else {
                log("Changes other: " + item.getChanges());
                log("Chat ID: " + item.getChatId());
            }
        }
        else{
            log("Changes NOT interested in");
        }

    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {
        log("onChatInitStateUpdate");
    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userHandle, int status, boolean inProgress) {
        log("onChatOnlineStatusUpdate");

        if(inProgress){
            status = -1;
        }
        if(userHandle == megaChatApi.getMyUserHandle()){
            log("My own status update");
            int position = participants.size()-1;
            if(chat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
                adapter.updateContactStatus(position+1);
            }
            else{
                adapter.updateContactStatus(position);
            }
        }
        else{
            log("Status update for the user: "+userHandle);

            int indexToReplace = -1;
            ListIterator<MegaChatParticipant> itrReplace = participants.listIterator();
            while (itrReplace.hasNext()) {
                MegaChatParticipant participant = itrReplace.next();
                if (participant != null) {
                    if (participant.getHandle() == userHandle) {
                        if(status != MegaChatApi.STATUS_ONLINE && status != MegaChatApi.STATUS_BUSY && status != MegaChatApi.STATUS_INVALID){
                            log("Request last green for user");
                            megaChatApi.requestLastGreen(userHandle, this);
                        }
                        else{
                            participant.setLastGreen("");
                        }
                        indexToReplace = itrReplace.nextIndex() - 1;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (indexToReplace != -1) {
                log("Index to replace: " + indexToReplace);
                if(chat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
                    adapter.updateContactStatus(indexToReplace+1);
                }
                else{
                    adapter.updateContactStatus(indexToReplace);
                }
            }
        }
    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
        log("onChatPresenceConfigUpdate");
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

    }


    public void showConfirmationPrivateChatDialog(){
        log("showConfirmationPrivateChatDialog");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_chat_link_options, null);
        dialogBuilder.setView(dialogView);

        Button actionButton = (Button) dialogView.findViewById(R.id.chat_link_button_action);
        actionButton.setText(getString(R.string.general_enable));

        TextView title = (TextView) dialogView.findViewById(R.id.chat_link_title);
        title.setText(getString(R.string.make_chat_private_option));

        TextView text = (TextView) dialogView.findViewById(R.id.text_chat_link);
        text.setText(getString(R.string.context_make_private_chat_warning_text));

        TextView secondText = (TextView) dialogView.findViewById(R.id.second_text_chat_link);
        secondText.setVisibility(View.GONE);

        chatLinkDialog = dialogBuilder.create();

        actionButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                chatLinkDialog.dismiss();
                megaChatApi.setPublicChatToPrivate(chatHandle, groupChatInfoActivity);
            }

        });

        chatLinkDialog.show();
    }

    public void showConfirmationCreateChatLinkDialog() {
        log("showConfirmationCreateChatLinkDialog");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_chat_link_options, null);
        dialogBuilder.setView(dialogView);

        Button actionButton = (Button) dialogView.findViewById(R.id.chat_link_button_action);
        actionButton.setText(getString(R.string.get_chat_link_option));

        TextView title = (TextView) dialogView.findViewById(R.id.chat_link_title);
        title.setText(getString(R.string.get_chat_link_option));

        TextView firstText = (TextView) dialogView.findViewById(R.id.text_chat_link);
        firstText.setText(getString(R.string.context_create_chat_link_warning_text));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) firstText.getLayoutParams();
        params.bottomMargin = Util.scaleHeightPx(12, outMetrics);
        firstText.setLayoutParams(params);

        chatLinkDialog = dialogBuilder.create();

        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chatLinkDialog.dismiss();
                createPublicGroupAndGetLink();
            }

        });

        chatLinkDialog.show();
    }


    public void startConversation(long handle){
        log("startConversation");
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if(chat==null){
            log("No chat, create it!");
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        }
        else{
            log("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
            intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
            this.startActivity(intentOpenChat);
        }
    }

    public void createPublicGroupAndGetLink(){

        long participantsCount = chat.getPeerCount();
        MegaChatPeerList peers = MegaChatPeerList.createInstance();

        for (int i=0;i<participantsCount;i++){
            log("Add participant: "+chat.getPeerHandle(i)+" privilege: "+chat.getPeerPrivilege(i));
            peers.addPeer(chat.getPeerHandle(i), chat.getPeerPrivilege(i));
        }

        CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, chat.getTitle());
        megaChatApi.createPublicChat(peers, chat.getTitle(), listener);
    }

    public void onRequestFinishCreateChat(int errorCode, long chatHandle, boolean publicLink){
        if(errorCode==MegaChatError.ERROR_OK){
            log("open new chat: " + chatHandle);
            Intent intent = new Intent(this, ChatActivityLollipop.class);
            intent.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra("CHAT_ID", chatHandle);
            if(publicLink){
                intent.putExtra("PUBLIC_LINK", errorCode);
            }
            this.startActivity(intent);
        }
        else{
            log("EEEERRRRROR WHEN CREATING CHAT " + errorCode);
            showSnackbar(getString(R.string.create_chat_error));
        }
    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
        log("onChatPresenceLastGreen");
        int state = megaChatApi.getUserOnlineStatus(userhandle);
        if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
            String formattedDate = TimeUtils.lastGreenDate(this, lastGreen);

            if(userhandle != megaChatApi.getMyUserHandle()){
                log("Status last green for the user: "+userhandle);
                int indexToReplace = -1;
                ListIterator<MegaChatParticipant> itrReplace = participants.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatParticipant participant = itrReplace.next();
                    if (participant != null) {
                        if (participant.getHandle() == userhandle) {
                            participant.setLastGreen(formattedDate);
                            indexToReplace = itrReplace.nextIndex() - 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (indexToReplace != -1) {
                    log("Index to replace: " + indexToReplace);
                    if(chat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
                        adapter.updateContactStatus(indexToReplace+1);
                    }
                    else{
                        adapter.updateContactStatus(indexToReplace);
                    }
                }
            }

            log("Date last green: "+formattedDate);
        }
    }
}
