package mega.privacy.android.app.lollipop.megachat;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.listeners.CreateGroupChatWithPublicLink;
import mega.privacy.android.app.lollipop.listeners.MultipleGroupChatRequestListener;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaParticipantsChatLollipopAdapter;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ParticipantBottomSheetDialogFragment;
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

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;

public class GroupChatInfoActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatListenerInterface, View.OnClickListener, MegaRequestListenerInterface, AdapterView.OnItemClickListener {


    private final static int MAX_LENGTH_CHAT_TITLE = 60;
    private final static int MAX_WIDTH_CHAT_TITLE_PORT = 200;
    private final static int MAX_WIDTH_CHAT_TITLE_LAND = 300;

    public long chatHandle;
    public long selectedHandleParticipant;
    private GroupChatInfoActivityLollipop groupChatInfoActivity;
    private MegaChatRoom chat;
    AlertDialog permissionsDialog;
    AlertDialog changeTitleDialog;
    AlertDialog chatLinkDialog;

    MenuItem addParticipantItem;
    MenuItem changeTitleItem;
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
    androidx.core.widget.NestedScrollView scrollView;
    LinearLayout infoLayout;
    RelativeLayout avatarLayout;
    TextView infoNumParticipantsText;
    RelativeLayout infoTextContainerLayout;
    ImageView editImageView;
    LinearLayout notificationsLayout;
    SwitchCompat notificationsSwitch;
    TextView notificationsTitle;
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
    private RoundedImageView avatarImageView;
    private EmojiTextView infoTitleChatText;
    private MegaApiAndroid megaApi = null;

    private ParticipantBottomSheetDialogFragment bottomSheetDialogFragment;

    private BroadcastReceiver nicknameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            long userHandle = intent.getLongExtra(EXTRA_USER_HANDLE, 0);
            updateAdapter(userHandle);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        logDebug("onCreate");
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
            logDebug("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        logDebug("addChatListener");
        megaChatApi.addChatListener(this);

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        scaleW = getScaleW(outMetrics, density);
        scaleH = getScaleH(outMetrics, density);

        if (scaleH < scaleW) {
            scaleText = scaleH;
        } else {
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
                logError("Chatroom NULL cannot be recovered");
                finish();
                return;
            }

            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
            chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));
            setContentView(R.layout.activity_group_chat_properties);

            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

            fragmentContainer = findViewById(R.id.fragment_container_group_chat);
            toolbar = findViewById(R.id.toolbar_group_chat_properties);
            setSupportActionBar(toolbar);
            aB = getSupportActionBar();

            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);

            aB.setTitle(getString(R.string.group_chat_info_label).toUpperCase());

            scrollView = findViewById(R.id.scroll_view_group_chat_properties);
            new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (scrollView.canScrollVertically(-1)) {
                        changeActionBarElevation(true);
                    } else {
                        changeActionBarElevation(false);
                    }
                }
            });

            //Info Layout
            avatarImageView = findViewById(R.id.chat_group_properties_thumbnail);
            createGroupChatAvatar();

            infoLayout = findViewById(R.id.chat_group_contact_properties_info_layout);
            infoLayout.setVisibility(View.VISIBLE);
            avatarLayout = findViewById(R.id.chat_group_properties_avatar_layout);

            infoTextContainerLayout = findViewById(R.id.chat_group_contact_properties_info_text_container);
            LinearLayout.LayoutParams paramsInfoText = (LinearLayout.LayoutParams) infoTextContainerLayout.getLayoutParams();
            paramsInfoText.leftMargin = scaleWidthPx(16, outMetrics);
            infoTextContainerLayout.setLayoutParams(paramsInfoText);

            infoTitleChatText = findViewById(R.id.chat_group_contact_properties_info_title);
            if(isScreenInPortrait(this)){
                infoTitleChatText.setMaxWidthEmojis(px2dp(MAX_WIDTH_CHAT_TITLE_PORT, outMetrics));
            }else{
                infoTitleChatText.setMaxWidthEmojis(px2dp(MAX_WIDTH_CHAT_TITLE_LAND, outMetrics));

            }
            infoTitleChatText.setText(getTitleChat(chat));

            editImageView = findViewById(R.id.chat_group_contact_properties_edit_icon);
            editImageView.setOnClickListener(this);

            //Notifications Layout
            notificationsLayout = findViewById(R.id.chat_group_contact_properties_notifications_layout);
            notificationsLayout.setVisibility(View.VISIBLE);

            notificationsTitle = findViewById(R.id.chat_group_contact_properties_notifications_title);
            notificationsSwitch = findViewById(R.id.chat_group_contact_properties_switch);
            notificationsSwitch.setOnClickListener(this);

            dividerNotifications = findViewById(R.id.divider_notifications_layout);

            infoNumParticipantsText = findViewById(R.id.chat_group_contact_properties_info_participants);

            //Chat links
            chatLinkLayout = findViewById(R.id.chat_group_contact_properties_chat_link_layout);
            chatLinkTitleText = findViewById(R.id.chat_group_contact_properties_chat_link);
            chatLinkSeparator = findViewById(R.id.divider_chat_link_layout);

            //Private chat
            privateLayout = findViewById(R.id.chat_group_contact_properties_private_layout);
            privateSeparator = findViewById(R.id.divider_private_layout);

            //Chat Shared Files Layout

            sharedFilesLayout = findViewById(R.id.chat_group_contact_properties_chat_files_shared_layout);
            sharedFilesLayout.setOnClickListener(this);

            dividerSharedFilesLayout = findViewById(R.id.divider_chat_files_shared_layout);

            //Clear chat Layout
            clearChatLayout = findViewById(R.id.chat_group_contact_properties_clear_layout);
            clearChatLayout.setOnClickListener(this);

            dividerClearLayout = findViewById(R.id.divider_clear_layout);

            //Archive chat Layout
            archiveChatLayout = findViewById(R.id.chat_group_contact_properties_archive_layout);
            archiveChatLayout.setOnClickListener(this);

            archiveChatSeparator = findViewById(R.id.divider_archive_layout);

            archiveChatTitle = findViewById(R.id.chat_group_contact_properties_archive);
            archiveChatIcon = findViewById(R.id.chat_group_contact_properties_archive_icon);

            //Leave chat Layout
            leaveChatLayout = findViewById(R.id.chat_group_contact_properties_leave_layout);
            leaveChatLayout.setOnClickListener(this);
            dividerLeaveLayout = findViewById(R.id.divider_leave_layout);

            //Observers layout
            observersLayout = findViewById(R.id.chat_group_observers_layout);
            observersNumberText = findViewById(R.id.chat_group_observers_number_text);
            observersSeparator = findViewById(R.id.divider_observers_layout);

            participantsTitle = findViewById(R.id.chat_group_contact_properties_title_text);

            recyclerView = findViewById(R.id.chat_group_contact_properties_list);
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setFocusable(false);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setNestedScrollingEnabled(false);

            if (chat.isPreview()) {
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
            } else {
                setChatPermissions();

                if (chat.isArchived()) {
                    archiveChatTitle.setText(getString(R.string.general_unarchive));
                    archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_unarchive));
                } else {
                    archiveChatTitle.setText(getString(R.string.general_archive));
                    archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_archive));
                }

                chatSettings = dbH.getChatSettings();

                if(chatSettings==null){
                    logDebug("Chat settings null - notifications ON");
                    setUpIndividualChatNotifications();
                }
                else {
                    logDebug("There is chat settings");
                    if (chatSettings.getNotificationsEnabled() == null) {
                        generalChatNotifications = true;

                    } else {
                        generalChatNotifications = Boolean.parseBoolean(chatSettings.getNotificationsEnabled());

                    }

                    if (generalChatNotifications) {
                        setUpIndividualChatNotifications();
                    } else {
                        logDebug("General notifications OFF");
                        notificationsSwitch.setChecked(false);
                    }
                }
            }

            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }

            LocalBroadcastManager.getInstance(this).registerReceiver(nicknameReceiver,
                    new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_NICKNAME));

            //Set participants
            participants = new ArrayList<>();

            setParticipants();

            updatePreviewers();
        }
    }


    public void updatePreviewers(){
        logDebug("updatePreviewers");
        if (chat.getNumPreviewers() < 1) {
            observersSeparator.setVisibility(View.GONE);
            observersLayout.setVisibility(View.GONE);
        } else {
            observersSeparator.setVisibility(View.VISIBLE);
            observersLayout.setVisibility(View.VISIBLE);
            observersNumberText.setText(chat.getNumPreviewers() + "");
        }
    }

    public void changeActionBarElevation(boolean whitElevation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (whitElevation) {
                aB.setElevation(px2dp(4, outMetrics));
            }
            else {
                aB.setElevation(0);
            }
        }
    }


    public void setUpIndividualChatNotifications(){
        logDebug("setUpIndividualChatNotifications");
        //SET Preferences (if exist)
        if(chatPrefs!=null){
            logDebug("There is individual chat preferences");
            notificationsSwitch.setChecked(isChatRoomEnabled(chatPrefs));
        }
        else{
            logDebug("NO individual chat preferences");
            notificationsSwitch.setChecked(true);
        }
    }

    public void setChatPermissions(){
        logDebug("setChatPermissions");
        if (chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR) {
            editImageView.setVisibility(View.VISIBLE);
            dividerClearLayout.setVisibility(View.VISIBLE);
            clearChatLayout.setVisibility(View.VISIBLE);
            dividerLeaveLayout.setVisibility(View.VISIBLE);

            if (chat.isPublic()) {
                privateLayout.setVisibility(View.VISIBLE);
                privateLayout.setOnClickListener(this);
                privateSeparator.setVisibility(View.VISIBLE);
            }
            else{
                logDebug("Private chat");
                privateLayout.setVisibility(View.GONE);
                privateSeparator.setVisibility(View.GONE);
            }
        } else {
            editImageView.setVisibility(View.GONE);
            dividerClearLayout.setVisibility(View.GONE);
            clearChatLayout.setVisibility(View.GONE);
            privateLayout.setVisibility(View.GONE);
            privateSeparator.setVisibility(View.GONE);

            if (chat.getOwnPrivilege() < MegaChatRoom.PRIV_RO) {
                leaveChatLayout.setVisibility(View.GONE);
                dividerLeaveLayout.setVisibility(View.GONE);
            }
        }

        if (chat.isPublic() && chat.getOwnPrivilege() >= MegaChatRoom.PRIV_RO) {
            chatLinkLayout.setVisibility(View.VISIBLE);
            chatLinkLayout.setOnClickListener(this);
            chatLinkSeparator.setVisibility(View.VISIBLE);
        } else {
            chatLinkLayout.setVisibility(View.GONE);
            chatLinkSeparator.setVisibility(View.GONE);
            chatLink = null;
        }
    }

    @Override
    protected void onDestroy(){
        logDebug("onDestroy()");
        super.onDestroy();
        if (megaChatApi != null) {
            megaChatApi.removeChatListener(this);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(nicknameReceiver);

    }

    private String checkParticipantName(long handle, int position) {
        String fullName = getNicknameContact(handle);

        if (fullName == null) fullName = getParticipantFullName(position);

        return fullName;
    }

    public void setParticipants(){
        logDebug("Participants size: " + participants.size());
        //Set the first element = me
        participantsCount = chat.getPeerCount();
        logDebug("Participants count: " + participantsCount);
        if (chat.isPreview()) {
            infoNumParticipantsText.setText(getString(R.string.number_of_participants, participantsCount));
        } else {
            long participantsLabel = participantsCount + 1; //Add one to include me
            infoNumParticipantsText.setText(getString(R.string.number_of_participants, participantsLabel));
        }

        for (int i = 0; i < participantsCount; i++) {
            int peerPrivilege = chat.getPeerPrivilege(i);
            if(peerPrivilege==MegaChatRoom.PRIV_RM){
                logDebug("Continue");
                continue;
            }

            long peerHandle = chat.getPeerHandle(i);
            String fullName = checkParticipantName(peerHandle, i);

            String participantEmail = chat.getPeerEmail(i);

            logDebug(i + " - Handle of the peer: "+ peerHandle + ", Pprivilege: " + peerPrivilege);
            MegaChatParticipant participant = new MegaChatParticipant(peerHandle, "", "", fullName, participantEmail, peerPrivilege);

            participants.add(participant);

            int userStatus = megaChatApi.getUserOnlineStatus(participant.getHandle());
            if(userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID){
                logDebug("Request last green for user");
                megaChatApi.requestLastGreen(participant.getHandle(), null);
            }
        }

        if(!chat.isPreview()){
            logDebug("Is not preview - add me as participant");
            String myFullName =  megaChatApi.getMyFullname();
            if(myFullName!=null){
                if(myFullName.trim().isEmpty()){
                    myFullName =  megaChatApi.getMyEmail();
                }
            } else {
                myFullName = megaChatApi.getMyEmail();
            }

            MegaChatParticipant me = new MegaChatParticipant(megaChatApi.getMyUserHandle(), null, null, getString(R.string.chat_me_text_bracket, myFullName), megaChatApi.getMyEmail(), chat.getOwnPrivilege());

            participants.add(me);
        }

        logDebug("Number of participants with me: " + participants.size());
        if (adapter == null){
            if(chat.isPreview()){
                adapter = new MegaParticipantsChatLollipopAdapter(this, participants, recyclerView, true);
            } else {
                adapter = new MegaParticipantsChatLollipopAdapter(this, participants, recyclerView, false);
            }

            adapter.setHasStableIds(true);
            adapter.setPositionClicked(-1);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.setParticipants(participants);
        }
    }

    private void updateAdapter(long contactHandle) {
        for (MegaChatParticipant participant : participants) {
            if (participant.getHandle() == contactHandle) {
                int pos = participants.indexOf(participant);
                String fullName = checkParticipantName(contactHandle, pos);
                participants.get(pos).setFullName(fullName);
                adapter.updateParticipant(pos, participants);
                break;
            }
        }
    }

    private String getParticipantFullName(long contact) {
        String nickname = getNicknameContact(contact);
        if (nickname != null) {
            return nickname;
        }

        String fullName = chat.getPeerFullname(contact);
        if (isTextEmpty(fullName)){
            return chat.getPeerEmail(contact);
        }

        return fullName;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenuLollipop");

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_group_chat_info, menu);

        addParticipantItem = menu.findItem(R.id.action_add_participants);
        changeTitleItem = menu.findItem(R.id.action_rename);

        int permission = chat.getOwnPrivilege();
        logDebug("Permision: " + permission);
        if (permission == MegaChatRoom.PRIV_MODERATOR) {
            addParticipantItem.setVisible(true);
            changeTitleItem.setVisible(true);
        } else {
            addParticipantItem.setVisible(false);
            changeTitleItem.setVisible(false);
        }

        logDebug("Call to super onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.action_add_participants: {
                //
                chooseAddParticipantDialog();
                break;
            }
            case R.id.action_rename: {
                showRenameGroupDialog(false);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    public void chooseAddParticipantDialog(){
        logDebug("chooseAddContactDialog");

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        if (megaApi != null && megaApi.getRootNode() != null) {
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if (contacts == null) {
                showSnackbar(getString(R.string.no_contacts_invite));
            } else {
                if (contacts.isEmpty()) {
                    showSnackbar(getString(R.string.no_contacts_invite));
                } else {
                    Intent in = new Intent(this, AddContactActivityLollipop.class);
                    in.putExtra("contactType", CONTACT_TYPE_MEGA);
                    in.putExtra("chat", true);
                    in.putExtra("chatId", chatHandle);
                    in.putExtra("aBtitle", getString(R.string.add_participants_menu_item));
                    startActivityForResult(in, REQUEST_ADD_PARTICIPANTS);
                }
            }
        }
        else {
            logWarning("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void showRemoveParticipantConfirmation (long handle, MegaChatRoom chatToChange){
        logDebug("Participant Handle: " + handle + ", Chat ID: " + chatToChange.getChatId());

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE: {
                        removeParticipant();
                        break;
                    }

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(groupChatInfoActivity, R.style.AppCompatAlertDialogStyle);
        String name = chatToChange.getPeerFullnameByHandle(handle);
        String message = getResources().getString(R.string.confirmation_remove_chat_contact, name);
        builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();

    }

    public void removeParticipant(){
        logDebug("selectedHandleParticipant: " + selectedHandleParticipant);
        logDebug("before remove, participants: " + chat.getPeerCount());
        ChatController cC = new ChatController(groupChatInfoActivity);
        cC.removeParticipant(chatHandle, selectedHandleParticipant);
    }

    public void changeTitle(String title){
        logDebug("changeTitle");
        ChatController cC = new ChatController(groupChatInfoActivity);
        cC.changeTitle(chatHandle, title);
    }

    public void showChangePermissionsDialog(long handle, MegaChatRoom chatToChange) {
        //Change permissions

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        final long handleToChange = handle;

        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.change_permissions_dialog, null);

        final LinearLayout administratorLayout = (LinearLayout) dialoglayout.findViewById(R.id.change_permissions_dialog_administrator_layout);
        final CheckedTextView administratorCheck = (CheckedTextView) dialoglayout.findViewById(R.id.change_permissions_dialog_administrator);
        administratorCheck.setCompoundDrawablePadding(scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams administratorMLP = (ViewGroup.MarginLayoutParams) administratorCheck.getLayoutParams();
        administratorMLP.setMargins(scaleWidthPx(17, outMetrics), 0, 0, 0);

        final TextView administratorTitle = (TextView) dialoglayout.findViewById(R.id.administrator_title);
        administratorTitle.setText(getString(R.string.administrator_permission_label_participants_panel));
        administratorTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView administratorSubtitle = (TextView) dialoglayout.findViewById(R.id.administrator_subtitle);
        administratorSubtitle.setText(getString(R.string.file_properties_shared_folder_full_access));
        administratorSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout administratorTextLayout = (LinearLayout) dialoglayout.findViewById(R.id.administrator_text_layout);
        ViewGroup.MarginLayoutParams administratorSubtitleMLP = (ViewGroup.MarginLayoutParams) administratorTextLayout.getLayoutParams();
        administratorSubtitleMLP.setMargins(scaleHeightPx(10, outMetrics), scaleHeightPx(15, outMetrics), 0, scaleHeightPx(15, outMetrics));

        final LinearLayout memberLayout = (LinearLayout) dialoglayout.findViewById(R.id.change_permissions_dialog_member_layout);
        final CheckedTextView memberCheck = (CheckedTextView) dialoglayout.findViewById(R.id.change_permissions_dialog_member);
        memberCheck.setCompoundDrawablePadding(scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams memberMLP = (ViewGroup.MarginLayoutParams) memberCheck.getLayoutParams();
        memberMLP.setMargins(scaleWidthPx(17, outMetrics), 0, 0, 0);

        final TextView memberTitle = (TextView) dialoglayout.findViewById(R.id.member_title);
        memberTitle.setText(getString(R.string.standard_permission_label_participants_panel));
        memberTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView memberSubtitle = (TextView) dialoglayout.findViewById(R.id.member_subtitle);
        memberSubtitle.setText(getString(R.string.file_properties_shared_folder_read_write));
        memberSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout memberTextLayout = (LinearLayout) dialoglayout.findViewById(R.id.member_text_layout);
        ViewGroup.MarginLayoutParams memberSubtitleMLP = (ViewGroup.MarginLayoutParams) memberTextLayout.getLayoutParams();
        memberSubtitleMLP.setMargins(scaleHeightPx(10, outMetrics), scaleHeightPx(15, outMetrics), 0, scaleHeightPx(15, outMetrics));

        final LinearLayout observerLayout = (LinearLayout) dialoglayout.findViewById(R.id.change_permissions_dialog_observer_layout);
        final CheckedTextView observerCheck = (CheckedTextView) dialoglayout.findViewById(R.id.change_permissions_dialog_observer);
        observerCheck.setCompoundDrawablePadding(scaleWidthPx(10, outMetrics));
        ViewGroup.MarginLayoutParams observerMLP = (ViewGroup.MarginLayoutParams) observerCheck.getLayoutParams();
        observerMLP.setMargins(scaleWidthPx(17, outMetrics), 0, 0, 0);

        final TextView observerTitle = (TextView) dialoglayout.findViewById(R.id.observer_title);
        observerTitle.setText(getString(R.string.observer_permission_label_participants_panel));
        observerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView observerSubtitle = (TextView) dialoglayout.findViewById(R.id.observer_subtitle);
        observerSubtitle.setText(getString(R.string.subtitle_read_only_permissions));
        observerSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout observerTextLayout = (LinearLayout) dialoglayout.findViewById(R.id.observer_text_layout);
        ViewGroup.MarginLayoutParams observerSubtitleMLP = (ViewGroup.MarginLayoutParams) observerTextLayout.getLayoutParams();
        observerSubtitleMLP.setMargins(scaleHeightPx(10, outMetrics), scaleHeightPx(15, outMetrics), 0, scaleHeightPx(15, outMetrics));

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setView(dialoglayout);

        builder.setTitle(getString(R.string.file_properties_shared_folder_permissions));
        permissionsDialog = builder.create();

        permissionsDialog.show();

        int permission = chatToChange.getPeerPrivilegeByHandle(handleToChange);

        if (permission == MegaChatRoom.PRIV_STANDARD) {
            administratorCheck.setChecked(false);
            memberCheck.setChecked(true);
            observerCheck.setChecked(false);
        } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
            administratorCheck.setChecked(true);
            memberCheck.setChecked(false);
            observerCheck.setChecked(false);
        } else {
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
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        };
        administratorLayout.setOnClickListener(clickListener);
        memberLayout.setOnClickListener(clickListener);
        observerLayout.setOnClickListener(clickListener);

        logDebug("Change permissions");
    }


    public void changePermissions(int newPermissions){
        logDebug("New permissions: " + newPermissions);
        ChatController cC = new ChatController(groupChatInfoActivity);
        cC.alterParticipantsPermissions(chatHandle, selectedHandleParticipant, newPermissions);
    }

    public void createGroupChatAvatar(){
        logDebug("createGroupChatAvatar()");
        avatarImageView.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR), getTitleChat(chat), AVATAR_SIZE, true));
    }

    public void showParticipantsPanel(MegaChatParticipant participant){
        logDebug("Participant Handle: " + participant.getHandle());

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
        logDebug("onClick");

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
                logDebug("Clear chat option");
                showConfirmationClearChat();
                break;
            }
            case R.id.chat_group_contact_properties_archive_layout: {
                ChatController chatC = new ChatController(this);
                chatC.archiveChat(chat);
                break;
            }
            case R.id.chat_group_contact_properties_switch:{
                logDebug("Click on switch notifications");
                if(!generalChatNotifications){
                    notificationsSwitch.setChecked(false);
                    showSnackbar("The chat notifications are disabled, go to settings to set up them");
                } else {
                    boolean enabled = notificationsSwitch.isChecked();

                    ChatController chatC = new ChatController(this);
                    String typeMute =  enabled ? NOTIFICATIONS_ENABLED: NOTIFICATIONS_DISABLED;
                    chatC.muteChat(chatHandle, typeMute);
                }

                break;

            }
            case R.id.chat_group_contact_properties_chat_link_layout: {
                megaChatApi.queryChatLink(chatHandle, groupChatInfoActivity);
                break;
            }
            case R.id.chat_group_contact_properties_private_layout: {
                logDebug("Make chat private");
                showConfirmationPrivateChatDialog();
                break;
            }
            case R.id.chat_group_contact_properties_chat_files_shared_layout: {
                Intent nodeHistoryIntent = new Intent(this, NodeAttachmentHistoryActivity.class);
                if (chat != null) {
                    nodeHistoryIntent.putExtra("chatId", chat.getChatId());
                }
                startActivity(nodeHistoryIntent);
                break;
            }
        }

    }


    public void copyLink(){
        logDebug("copyLink");
        if(chatLink!=null) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", chatLink);
            clipboard.setPrimaryClip(clip);
            showSnackbar(getString(R.string.chat_link_copied_clipboard));
        } else {
            showSnackbar(getString(R.string.general_text_error));
        }
    }


    public void removeChatLink(){
        logDebug("removeChatLink");
        megaChatApi.removeChatLink(chatHandle, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("Result Code: " + resultCode);
       if (requestCode == REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
           logDebug("REQUEST_ADD_PARTICIPANTS OK");
            if (intent == null) {
                logWarning("Return.....");
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
                    logDebug("Add multiple participants " + contactsData.size());
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
           logError("Error REQUEST_ADD_PARTICIPANTS");
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }


    public void showRenameGroupDialog(boolean fromGetLink){
        logDebug("fromGetLink: " + fromGetLink);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        if (fromGetLink) {
            final TextView alertRename = new TextView(this);
            alertRename.setText(getString(R.string.message_error_set_title_get_link));

            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsText.setMargins(scaleWidthPx(24, outMetrics), scaleHeightPx(8, outMetrics), scaleWidthPx(12, outMetrics), 0);
            alertRename.setLayoutParams(paramsText);
            layout.addView(alertRename);
            params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(8, outMetrics), scaleWidthPx(17, outMetrics), 0);

        }
        else{
            params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(16, outMetrics), scaleWidthPx(17, outMetrics), 0);
        }

        final EmojiEditText input = new EmojiEditText(this);

        layout.addView(input, params);

        input.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
        input.setSingleLine();
        input.setSelectAllOnFocus(true);
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setEmojiSize(px2dp(EMOJI_SIZE, outMetrics));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        int maxAllowed = getMaxAllowed(getTitleChat(chat));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxAllowed)});
        input.setText(getTitleChat(chat));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    changeTitle(input);
                }
                else{
                    logDebug("Other IME" + actionId);
                }
                return false;
            }
        });
        input.setImeActionLabel(getString(R.string.context_rename), EditorInfo.IME_ACTION_DONE);
        builder.setTitle(getString(R.string.context_rename));
        builder.setPositiveButton(getString(R.string.context_rename),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        changeTitle(input);
                    }
                });


        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                hideKeyboard(groupChatInfoActivity, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

        builder.setNegativeButton(getString(android.R.string.cancel), null);
        builder.setView(layout);
        changeTitleDialog = builder.create();
        changeTitleDialog.show();

        changeTitleDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logDebug("OK BTTN CHANGE");
                changeTitle(input);
            }
        });

    }

    private void changeTitle(EmojiEditText input){
        String title = input.getText().toString();
        if (title.equals("") || title.isEmpty() || title.trim().isEmpty()) {
            logWarning("Input is empty");
            input.setError(getString(R.string.invalid_string));
            input.requestFocus();
        }
        else if(!isAllowedTitle(title)){
            logWarning("Title is too long");
            input.setError(getString(R.string.title_long));
            input.requestFocus();
        }
        else {
            logDebug("Positive button pressed - change title");
            changeTitle(title);
            changeTitleDialog.dismiss();
        }
    }


    public void showConfirmationClearChat(){
        logDebug("showConfirmationClearChat");
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        logDebug("Clear chat!");
//						megaChatApi.truncateChat(chatHandle, MegaChatHandle.MEGACHAT_INVALID_HANDLE);
                        logDebug("Clear history selected!");
                        ChatController chatC = new ChatController(groupChatInfoActivity);
                        chatC.clearHistory(chat);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message = getResources().getString(R.string.confirmation_clear_group_chat);
        builder.setTitle(R.string.title_confirmation_clear_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_clear, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationLeaveChat (final MegaChatRoom c){
        logDebug("Chat ID: " + c.getChatId());
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
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

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
        String message = getResources().getString(R.string.confirmation_leave_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void inviteContact (String email){
        logDebug("inviteContact");
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
        logDebug("onRequestFinish CHAT: " + request.getType()+ " " + e.getErrorCode());

        if(request.getType() == MegaChatRequest.TYPE_UPDATE_PEER_PERMISSIONS){
            logDebug("Permissions changed");
            int index = -1;
            MegaChatParticipant participantToUpdate = null;

            logDebug("Participants count: " + participantsCount);
            for (int i = 0; i < participantsCount; i++) {
                if (request.getUserHandle() == participants.get(i).getHandle()) {
                    participantToUpdate = participants.get(i);

                    participantToUpdate.setPrivilege(request.getPrivilege());
                    index = i;
                    break;
                }
            }

            if (index != -1 && participantToUpdate != null) {
                participants.set(index, participantToUpdate);
//                adapter.setParticipants(participants);
                adapter.updateParticipant(index + 1, participants);
            }

        } else if (request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM) {
            long chatHandle = request.getChatHandle();
            MegaChatRoom chat = megaChatApi.getChatRoom(chatHandle);
            String chatTitle = getTitleChat(chat);

            if (chatTitle == null) {
                chatTitle = "";
            } else if (!chatTitle.isEmpty() && chatTitle.length() > MAX_LENGTH_CHAT_TITLE) {
                chatTitle = chatTitle.substring(0, 59) + "...";
            }

            if (!chatTitle.isEmpty() && chat.isGroup() && !chat.hasCustomTitle()) {
                chatTitle = "\"" + chatTitle + "\"";
            }

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getFlag()){
                    logDebug("Chat archived");
                    Intent intent = new Intent(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED_GROUP);
                    intent.putExtra(CHAT_TITLE, chatTitle);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    finish();
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

            if (chat.isArchived()) {
                archiveChatTitle.setText(getString(R.string.general_unarchive));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_unarchive));
            } else {
                archiveChatTitle.setText(getString(R.string.general_archive));
                archiveChatIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_b_archive));
            }

        }
        else if(request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM){
            logDebug("Remove participant: " + request.getUserHandle());
            logDebug("My user handle: " + megaChatApi.getMyUserHandle());

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getUserHandle()==-1){
                    logDebug("I left the chatroom");
                    finish();
                }
                else{
                    logDebug("Removed from chat");

                    chat = megaChatApi.getChatRoom(chatHandle);
                    logDebug("Peers after onChatListItemUpdate: " + chat.getPeerCount());
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
            } else {

                if(request.getUserHandle()==-1){
                    logError("ERROR WHEN LEAVING CHAT" + e.getErrorString());
                    showSnackbar("Error.Chat not left");
                }
                else{
                    logError("ERROR WHEN TYPE_REMOVE_FROM_CHATROOM " + e.getErrorString());
                    showSnackbar(getString(R.string.remove_participant_error));
                }
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_EDIT_CHATROOM_NAME) {
            logDebug("Change title");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getText()!=null) {
                    infoTitleChatText.setText(request.getText());
                }
            } else {
                logError("ERROR WHEN TYPE_EDIT_CHATROOM_NAME " + e.getErrorString());
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
            logDebug("Truncate history request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Ok. Clear history done");
                showSnackbar(getString(R.string.clear_history_success));
            }
            else{
                logError("Error clearing history: " + e.getErrorString());
                showSnackbar(getString(R.string.clear_history_error));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_INVITE_TO_CHATROOM) {
            logDebug("Invite to chatroom request finish!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Ok. Invited");
                showSnackbar(getString(R.string.add_participant_success));

                if(request.getChatHandle()==chatHandle){
                    logDebug("Changes in my chat");
                    chat = megaChatApi.getChatRoom(chatHandle);
                    logDebug("Peers after onChatListItemUpdate: " + chat.getPeerCount());
//                chat = megaChatApi.getChatRoom(chatHandle);
                    participants.clear();
                    setParticipants();
//                scrollView.invalidate();
//                scrollView.setFillViewport(false);
                }
                else{
                    logWarning("Changes NOT interested in");
                }
//                chat = megaChatApi.getChatRoom(chatHandle);
//                participants.clear();
//                setParticipants(chat);
//                scrollView.invalidate();
//                scrollView.setFillViewport(false);
            }
            else if (e.getErrorCode() == MegaChatError.ERROR_EXIST){
                logError("Error inviting ARGS: " + e.getErrorString());
                showSnackbar(getString(R.string.add_participant_error_already_exists));
            }
            else{
                logError("Error inviting: " + e.getErrorString() + " " + e.getErrorCode());
                showSnackbar(getString(R.string.add_participant_error));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
            logDebug("Create chat request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Open new chat");
                Intent intent = new Intent(this, ChatActivityLollipop.class);
                intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
                intent.putExtra("CHAT_ID", request.getChatHandle());
                this.startActivity(intent);
            }
            else{
                logError("ERROR WHEN CREATING CHAT " + e.getErrorString());
                showSnackbar(getString(R.string.create_chat_error));
            }
        }
        else if (request.getType() == MegaChatRequest.TYPE_CHAT_LINK_HANDLE){
            logDebug("MegaChatRequest.TYPE_CHAT_LINK_HANDLE finished!!!");
            if(request.getFlag()==false){
                if(request.getNumRetry()==0){
//                    Query chat link
                    if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                        chatLink = request.getText();
                        showShareChatLinkDialog(groupChatInfoActivity, chat, chatLink);
                        return;
                    }
                    else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                        logError("The chatroom isn't grupal or public");
                    }
                    else if (e.getErrorCode()==MegaChatError.ERROR_NOENT){
                        logError("The chatroom doesn't exist or the chatid is invalid");
                    }
                    else if(e.getErrorCode()==MegaChatError.ERROR_ACCESS){
                        logError("The chatroom doesn't have a topic or the caller isn't an operator");
                    }
                    else{
                        logError("Error TYPE_CHAT_LINK_HANDLE " + e.getErrorCode());
                    }
                    if (chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR){
                        if (chat.hasCustomTitle()) {
                            megaChatApi.createChatLink(chatHandle, groupChatInfoActivity);
                        } else {
                            showRenameGroupDialog(true);
                        }
                    } else {
                        showSnackbar(getString(R.string.no_chat_link_available));
                    }
                } else if (request.getNumRetry() == 1) {
//                    Create chat link
                    if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                        chatLink = request.getText();
                        showShareChatLinkDialog(groupChatInfoActivity, chat, chatLink);
                    }
                    else{
                        logError("Error TYPE_CHAT_LINK_HANDLE " + e.getErrorCode());
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                }
            }
            else{
                if(request.getNumRetry()==0){
                    logDebug("Removing chat link");
                    if(e.getErrorCode()==MegaChatError.ERROR_OK){
                        chatLink = null;
                        showSnackbar(getString(R.string.chat_link_deleted));
                    } else {
                        if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                            logError("The chatroom isn't grupal or public");
                        }
                        else if (e.getErrorCode()==MegaChatError.ERROR_NOENT){
                            logError("The chatroom doesn't exist or the chatid is invalid");
                        }
                        else if(e.getErrorCode()==MegaChatError.ERROR_ACCESS){
                            logError("The chatroom doesn't have a topic or the caller isn't an operator");
                        }
                        else{
                            logError("Error TYPE_CHAT_LINK_HANDLE " + e.getErrorCode());
                        }
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                }
            }
            updatePreviewers();
        }
        else if (request.getType() == MegaChatRequest.TYPE_SET_PRIVATE_MODE){
            logDebug("MegaChatRequest.TYPE_SET_PRIVATE_MODE finished!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                chatLink = null;
                logDebug("Chat is PRIVATE now");
                chatLinkLayout.setVisibility(View.GONE);
                chatLinkSeparator.setVisibility(View.GONE);
                privateLayout.setVisibility(View.GONE);
                privateSeparator.setVisibility(View.GONE);
                updatePreviewers();
            }
            else{
                logError("Error on closeChatLink");
                if(e.getErrorCode()==MegaChatError.ERROR_ARGS){
                    logError("NOT public chatroom");
                }
                else if(e.getErrorCode()==MegaChatError.ERROR_NOENT){
                    logError("Chatroom not FOUND");
                }
                else if(e.getErrorCode()==MegaChatError.ERROR_ACCESS){
                    logError("NOT privileges or private chatroom");
                }
                showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
            }
        }
    }

    public void showSnackbar(String s) {
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
        logDebug("onRequestFinish " + request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());
            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    logDebug("OK INVITE CONTACT: " + request.getEmail());
                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
                    {
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    }
                }
                else{
                    logError("Error - Code: " + e.getErrorString());
                    if(e.getErrorCode()==MegaError.API_EEXIST)
                    {
                        showSnackbar(getString(R.string.context_contact_already_invited, request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode() == MegaError.API_EARGS) {
                        showSnackbar(getString(R.string.error_own_email_as_contact));
                    } else {
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                    logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        logDebug("Chat ID: " + item.getChatId());

        if(item.getChatId()==chatHandle){
            logDebug("Changes in my chat");
            chat = megaChatApi.getChatRoom(chatHandle);

            if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)){
                logDebug("Change participants");
                participants.clear();
                setParticipants();
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV)){
                logDebug("Change status: CHANGE_TYPE_OWN_PRIV");
                setChatPermissions();
                participants.clear();
                setParticipants();
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_TITLE)) {
                logDebug("Change status: CHANGE_TYPE_TITLE");
                infoTitleChatText.setText(getTitleChat(chat));
                createGroupChatAvatar();
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)) {
                logDebug("CHANGE_TYPE_CLOSED");
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
                logDebug("CHANGE_TYPE_UNREAD_COUNT");
            }
            else if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UPDATE_PREVIEWERS)){
                updatePreviewers();
            }
            else {
                logDebug("Changes other: " + item.getChanges());
                logDebug("Chat ID: " + item.getChatId());
            }
        }
        else{
            logDebug("Changes NOT interested in");
        }
    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {
        logDebug("New state: " + newState);
    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userHandle, int status, boolean inProgress) {
        logDebug("User Handle: " + userHandle + ", Status: " + status + ", inProgress: " + inProgress);

        if (inProgress) {
            status = -1;
        }
        if(userHandle == megaChatApi.getMyUserHandle()){
            logDebug("My own status update");
            int position = participants.size()-1;
            if(chat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
                adapter.updateContactStatus(position+1);
            }
            else{
                adapter.updateContactStatus(position);
            }
        }
        else{
            logDebug("Status update for the user: " + userHandle);
            int indexToReplace = -1;
            ListIterator<MegaChatParticipant> itrReplace = participants.listIterator();
            while (itrReplace.hasNext()) {
                MegaChatParticipant participant = itrReplace.next();
                if (participant != null) {
                    if (participant.getHandle() == userHandle) {
                        if(status != MegaChatApi.STATUS_ONLINE && status != MegaChatApi.STATUS_BUSY && status != MegaChatApi.STATUS_INVALID){
                            logDebug("Request last green for user");
                            megaChatApi.requestLastGreen(userHandle, this);
                        } else {
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
                logDebug("Index to replace: " + indexToReplace);
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
        logDebug("onChatPresenceConfigUpdate");
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

    }

    public void showConfirmationPrivateChatDialog(){
        logDebug("showConfirmationPrivateChatDialog");
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

        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chatLinkDialog.dismiss();
                megaChatApi.setPublicChatToPrivate(chatHandle, groupChatInfoActivity);
            }

        });

        chatLinkDialog.show();
    }

    public void showConfirmationCreateChatLinkDialog() {
        logDebug("showConfirmationCreateChatLinkDialog");

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
        params.bottomMargin = scaleHeightPx(12, outMetrics);
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
        logDebug("Handle: " + handle);
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if(chat==null){
            logDebug("No chat, create it!");
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        }
        else{
            logDebug("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
            this.startActivity(intentOpenChat);
        }
    }

    public void createPublicGroupAndGetLink() {

        long participantsCount = chat.getPeerCount();
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        for (int i = 0; i < participantsCount; i++) {
            logDebug("Add participant: " + chat.getPeerHandle(i) + ", privilege: " + chat.getPeerPrivilege(i));
            peers.addPeer(chat.getPeerHandle(i), chat.getPeerPrivilege(i));
        }

        CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, getTitleChat(chat));
        megaChatApi.createPublicChat(peers, getTitleChat(chat), listener);
    }


    public void onRequestFinishCreateChat(int errorCode, long chatHandle, boolean publicLink){
        if(errorCode==MegaChatError.ERROR_OK){
            logDebug("Open new chat: " + chatHandle);
            Intent intent = new Intent(this, ChatActivityLollipop.class);
            intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra("CHAT_ID", chatHandle);
            if (publicLink) {
                intent.putExtra("PUBLIC_LINK", errorCode);
            }
            this.startActivity(intent);
        }
        else{
            logError("ERROR WHEN CREATING CHAT " + errorCode);
            showSnackbar(getString(R.string.create_chat_error));
        }
    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
        logDebug("User Handle: " + userhandle + ", Last green: " + lastGreen);
        int state = megaChatApi.getUserOnlineStatus(userhandle);
        if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
            String formattedDate = lastGreenDate(this, lastGreen);

            if(userhandle != megaChatApi.getMyUserHandle()){
                logDebug("Status last green for the user: " + userhandle);
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
                    logDebug("Index to replace: " + indexToReplace);
                    if(chat.getOwnPrivilege()==MegaChatRoom.PRIV_MODERATOR){
                        adapter.updateContactStatus(indexToReplace+1);
                    }
                    else{
                        adapter.updateContactStatus(indexToReplace);
                    }
                }
            }

            logDebug("Date last green: " + formattedDate);
        }
    }
}
