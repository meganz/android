package mega.privacy.android.app.lollipop.megachat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.os.CountDownTimer;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.GroupParticipantsDividerItemDecoration;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.GetPeerAttributesListener;
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
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.getAvatarBitmap;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class GroupChatInfoActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatListenerInterface, MegaRequestListenerInterface {

    private static final int TIMEOUT = 300;
    private static final int MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE = 100;
    private static final int MAX_LENGTH_CHAT_TITLE = 60;

    private ChatController chatC;
    private long chatHandle;
    private long selectedHandleParticipant;
    private long participantsCount;

    private GroupChatInfoActivityLollipop groupChatInfoActivity;
    private MegaChatRoom chat;
    private AlertDialog permissionsDialog;
    private AlertDialog changeTitleDialog;
    private AlertDialog chatLinkDialog;

    private ChatItemPreferences chatPrefs = null;
    private ChatSettings chatSettings = null;

    private LinearLayout containerLayout;
    private Toolbar toolbar;
    private ActionBar aB;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;

    private MegaParticipantsChatLollipopAdapter adapter;
    private ArrayList<MegaChatParticipant> participants = new ArrayList<>();

    private String chatLink;
    private boolean chatMuted;

    private HashMap<Integer, MegaChatParticipant> pendingParticipantRequests = new HashMap<>();

    private ParticipantBottomSheetDialogFragment bottomSheetDialogFragment;

    private CountDownTimer countDownTimer;

    /**
     * Broadcast to update a contact in adapter due to a change.
     * Currently the changes contemplated are: nickname and credentials.
     */
    private BroadcastReceiver contactUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            if (intent.getAction().equals(ACTION_UPDATE_NICKNAME)
                || intent.getAction().equals(ACTION_UPDATE_FIRST_NAME)
                || intent.getAction().equals(ACTION_UPDATE_LAST_NAME)
                || intent.getAction().equals(ACTION_UPDATE_CREDENTIALS)) {
                updateAdapter(intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");
        groupChatInfoActivity = this;
        chatC = new ChatController(this);

        if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
            logDebug("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        logDebug("addChatListener");
        megaChatApi.addChatListener(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            chatHandle = extras.getLong("handle", -1);
            if (chatHandle == INVALID_HANDLE) {
                finish();
                return;
            }

            chat = megaChatApi.getChatRoom(chatHandle);
            if (chat == null) {
                logError("Chatroom NULL cannot be recovered");
                finish();
                return;
            }

            chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));
            setContentView(R.layout.activity_group_chat_properties);

            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

            containerLayout = findViewById(R.id.fragment_container_group_chat);

            toolbar = findViewById(R.id.toolbar_group_chat_properties);
            setSupportActionBar(toolbar);
            aB = getSupportActionBar();
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
            aB.setTitle(getString(R.string.group_chat_info_label).toUpperCase());

            recyclerView = findViewById(R.id.chat_group_contact_properties_list);
            recyclerView.addItemDecoration(new GroupParticipantsDividerItemDecoration(this, getOutMetrics()));
            recyclerView.setHasFixedSize(true);
            linearLayoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setFocusable(false);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setNestedScrollingEnabled(false);
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    changeViewElevation(aB, recyclerView.canScrollVertically(-1), getOutMetrics());
                    if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                        checkIfShouldAskForUsersAttributes(RecyclerView.SCROLL_STATE_IDLE);
                    }
                }

                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    checkIfShouldAskForUsersAttributes(newState);
                }
            });

            if (!chat.isPreview()) {
                chatSettings = dbH.getChatSettings();

                if (chatSettings == null || areGeneralChatNotificationsAvailable()) {
                    if (chatPrefs != null) {
                        boolean notificationsEnabled = true;

                        if (chatPrefs.getNotificationsEnabled() != null) {
                            notificationsEnabled = Boolean.parseBoolean(chatPrefs.getNotificationsEnabled());
                        }

                        chatMuted = !notificationsEnabled;
                    } else {
                        chatMuted = false;
                    }
                } else {
                    chatMuted = true;
                }
            }

            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }

            IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
            contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
            contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
            contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
            contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS);
            registerReceiver(contactUpdateReceiver, contactUpdateFilter);

            setParticipants();
            updateAdapterHeader();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (megaChatApi != null) {
            megaChatApi.removeChatListener(this);
        }

        unregisterReceiver(contactUpdateReceiver);
    }

    private void setParticipants() {
        //Set the first element = me
        participantsCount = chat.getPeerCount();
        logDebug("Participants count: " + participantsCount);

        for (int i = 0; i < participantsCount; i++) {
            int peerPrivilege = chat.getPeerPrivilege(i);
            if (peerPrivilege == MegaChatRoom.PRIV_RM) {
                continue;
            }

            long peerHandle = chat.getPeerHandle(i);
            MegaChatParticipant participant = new MegaChatParticipant(peerHandle, peerPrivilege);
            participants.add(participant);

            int userStatus = getUserStatus(peerHandle);
            if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                megaChatApi.requestLastGreen(participant.getHandle(), null);
            }
        }

        if (!chat.isPreview() && chat.isActive()) {
            String myFullName = megaChatApi.getMyFullname();
            if (isTextEmpty(myFullName)) {
                myFullName = megaChatApi.getMyEmail();
            }

            MegaChatParticipant me = new MegaChatParticipant(megaChatApi.getMyUserHandle(), null, null, getString(R.string.chat_me_text_bracket, myFullName), megaChatApi.getMyEmail(), chat.getOwnPrivilege());
            participants.add(me);
        }

        logDebug("Number of participants with me: " + participants.size());
        if (adapter == null) {
            adapter = new MegaParticipantsChatLollipopAdapter(this, recyclerView);
            adapter.setHasStableIds(true);
            recyclerView.setAdapter(adapter);
        }

        adapter.setParticipants(participants);
    }

    private void updateAdapter(long contactHandle) {
        chat = megaChatApi.getChatRoom(chatHandle);
        for (MegaChatParticipant participant : participants) {
            if (participant.getHandle() == contactHandle) {
                int pos = participants.indexOf(participant);
                participants.get(pos).setFullName(chatC.getParticipantFullName(contactHandle));
                adapter.updateParticipant(pos, participants);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_group_chat_info, menu);

        MenuItem addParticipantItem = menu.findItem(R.id.action_add_participants);
        MenuItem changeTitleItem = menu.findItem(R.id.action_rename);

        int permission = chat.getOwnPrivilege();
        if (permission == MegaChatRoom.PRIV_MODERATOR) {
            addParticipantItem.setVisible(true);
            changeTitleItem.setVisible(true);
        } else {
            addParticipantItem.setVisible(false);
            changeTitleItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            case R.id.action_add_participants:
                chooseAddParticipantDialog();
                break;

            case R.id.action_rename:
                showRenameGroupDialog(false);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void chooseAddParticipantDialog() {
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
        } else {
            logWarning("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    /**
     * Shows an alert dialog to confirm the deletion of a participant.
     *
     * @param handle        participant's handle
     */
    public void showRemoveParticipantConfirmation(long handle) {
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(groupChatInfoActivity, R.style.AppCompatAlertDialogStyle);
        String name = chatC.getParticipantFullName(handle);
        builder.setMessage(getResources().getString(R.string.confirmation_remove_chat_contact, name))
                .setPositiveButton(R.string.general_remove, (dialog, which) -> removeParticipant())
                .setNegativeButton(R.string.general_cancel, null)
                .show();
    }

    private void removeParticipant() {
        logDebug("selectedHandleParticipant: " + selectedHandleParticipant);
        logDebug("before remove, participants: " + chat.getPeerCount());
        chatC.removeParticipant(chatHandle, selectedHandleParticipant);
    }

    public void showChangePermissionsDialog(long handle, MegaChatRoom chatToChange) {
        //Change permissions

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }

        final long handleToChange = handle;

        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.change_permissions_dialog, null);

        final LinearLayout administratorLayout = dialoglayout.findViewById(R.id.change_permissions_dialog_administrator_layout);
        final CheckedTextView administratorCheck = dialoglayout.findViewById(R.id.change_permissions_dialog_administrator);
        administratorCheck.setCompoundDrawablePadding(scaleWidthPx(10, getOutMetrics()));
        ViewGroup.MarginLayoutParams administratorMLP = (ViewGroup.MarginLayoutParams) administratorCheck.getLayoutParams();
        administratorMLP.setMargins(scaleWidthPx(17, getOutMetrics()), 0, 0, 0);

        final TextView administratorTitle = dialoglayout.findViewById(R.id.administrator_title);
        administratorTitle.setText(getString(R.string.administrator_permission_label_participants_panel));
        administratorTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView administratorSubtitle = dialoglayout.findViewById(R.id.administrator_subtitle);
        administratorSubtitle.setText(getString(R.string.file_properties_shared_folder_full_access));
        administratorSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout administratorTextLayout = dialoglayout.findViewById(R.id.administrator_text_layout);
        ViewGroup.MarginLayoutParams administratorSubtitleMLP = (ViewGroup.MarginLayoutParams) administratorTextLayout.getLayoutParams();
        administratorSubtitleMLP.setMargins(scaleHeightPx(10, getOutMetrics()), scaleHeightPx(15, getOutMetrics()), 0, scaleHeightPx(15, getOutMetrics()));

        final LinearLayout memberLayout = dialoglayout.findViewById(R.id.change_permissions_dialog_member_layout);
        final CheckedTextView memberCheck = dialoglayout.findViewById(R.id.change_permissions_dialog_member);
        memberCheck.setCompoundDrawablePadding(scaleWidthPx(10, getOutMetrics()));
        ViewGroup.MarginLayoutParams memberMLP = (ViewGroup.MarginLayoutParams) memberCheck.getLayoutParams();
        memberMLP.setMargins(scaleWidthPx(17, getOutMetrics()), 0, 0, 0);

        final TextView memberTitle = dialoglayout.findViewById(R.id.member_title);
        memberTitle.setText(getString(R.string.standard_permission_label_participants_panel));
        memberTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView memberSubtitle = dialoglayout.findViewById(R.id.member_subtitle);
        memberSubtitle.setText(getString(R.string.file_properties_shared_folder_read_write));
        memberSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout memberTextLayout = dialoglayout.findViewById(R.id.member_text_layout);
        ViewGroup.MarginLayoutParams memberSubtitleMLP = (ViewGroup.MarginLayoutParams) memberTextLayout.getLayoutParams();
        memberSubtitleMLP.setMargins(scaleHeightPx(10, getOutMetrics()), scaleHeightPx(15, getOutMetrics()), 0, scaleHeightPx(15, getOutMetrics()));

        final LinearLayout observerLayout = dialoglayout.findViewById(R.id.change_permissions_dialog_observer_layout);
        final CheckedTextView observerCheck = dialoglayout.findViewById(R.id.change_permissions_dialog_observer);
        observerCheck.setCompoundDrawablePadding(scaleWidthPx(10, getOutMetrics()));
        ViewGroup.MarginLayoutParams observerMLP = (ViewGroup.MarginLayoutParams) observerCheck.getLayoutParams();
        observerMLP.setMargins(scaleWidthPx(17, getOutMetrics()), 0, 0, 0);

        final TextView observerTitle = dialoglayout.findViewById(R.id.observer_title);
        observerTitle.setText(getString(R.string.observer_permission_label_participants_panel));
        observerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (16));
        final TextView observerSubtitle = dialoglayout.findViewById(R.id.observer_subtitle);
        observerSubtitle.setText(getString(R.string.subtitle_read_only_permissions));
        observerSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14));
        final LinearLayout observerTextLayout = dialoglayout.findViewById(R.id.observer_text_layout);
        ViewGroup.MarginLayoutParams observerSubtitleMLP = (ViewGroup.MarginLayoutParams) observerTextLayout.getLayoutParams();
        observerSubtitleMLP.setMargins(scaleHeightPx(10, getOutMetrics()), scaleHeightPx(15, getOutMetrics()), 0, scaleHeightPx(15, getOutMetrics()));

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

        View.OnClickListener clickListener = v -> {
            switch (v.getId()) {
                case R.id.change_permissions_dialog_administrator_layout:
                    changePermissions(MegaChatRoom.PRIV_MODERATOR);
                    break;

                case R.id.change_permissions_dialog_member_layout:
                    changePermissions(MegaChatRoom.PRIV_STANDARD);
                    break;

                case R.id.change_permissions_dialog_observer_layout:
                    changePermissions(MegaChatRoom.PRIV_RO);
                    break;
            }

            if (dialog != null) {
                dialog.dismiss();
            }
        };
        administratorLayout.setOnClickListener(clickListener);
        memberLayout.setOnClickListener(clickListener);
        observerLayout.setOnClickListener(clickListener);

        logDebug("Change permissions");
    }


    private void changePermissions(int newPermissions) {
        logDebug("New permissions: " + newPermissions);
        chatC.alterParticipantsPermissions(chatHandle, selectedHandleParticipant, newPermissions);
    }

    public void showParticipantsPanel(MegaChatParticipant participant) {
        logDebug("Participant Handle: " + participant.getHandle());

        if (participant == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedHandleParticipant = participant.getHandle();
        bottomSheetDialogFragment = new ParticipantBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public MegaChatRoom getChat() {
        return chat;
    }

    public void copyLink() {
        logDebug("copyLink");
        if (chatLink != null) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", chatLink);
            clipboard.setPrimaryClip(clip);
            showSnackbar(getString(R.string.chat_link_copied_clipboard));
        } else {
            showSnackbar(getString(R.string.general_text_error));
        }
    }

    public void removeChatLink() {
        logDebug("removeChatLink");
        megaChatApi.removeChatLink(chatHandle, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("Result Code: " + resultCode);
        if (requestCode == REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            logDebug("Participants successfully added");
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
        } else {
            logError("Error adding participants");
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    public void showRenameGroupDialog(boolean fromGetLink) {
        logDebug("fromGetLink: " + fromGetLink);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        if (fromGetLink) {
            final TextView alertRename = new TextView(this);
            alertRename.setText(getString(R.string.message_error_set_title_get_link));

            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsText.setMargins(scaleWidthPx(24, getOutMetrics()), scaleHeightPx(8, getOutMetrics()), scaleWidthPx(12, getOutMetrics()), 0);
            alertRename.setLayoutParams(paramsText);
            layout.addView(alertRename);
            params.setMargins(scaleWidthPx(20, getOutMetrics()), scaleHeightPx(8, getOutMetrics()), scaleWidthPx(17, getOutMetrics()), 0);

        } else {
            params.setMargins(scaleWidthPx(20, getOutMetrics()), scaleHeightPx(16, getOutMetrics()), scaleWidthPx(17, getOutMetrics()), 0);
        }

        final EmojiEditText input = new EmojiEditText(this);

        layout.addView(input, params);

        input.setOnLongClickListener(v -> false);
        input.setSingleLine();
        input.setSelectAllOnFocus(true);
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setEmojiSize(px2dp(EMOJI_SIZE, getOutMetrics()));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        int maxAllowed = getMaxAllowed(getTitleChat(chat));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxAllowed)});
        input.setText(getTitleChat(chat));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                changeTitle(input);
            } else {
                logDebug("Other IME" + actionId);
            }
            return false;
        });
        input.setImeActionLabel(getString(R.string.context_rename), EditorInfo.IME_ACTION_DONE);

        builder.setTitle(R.string.context_rename)
                .setPositiveButton(getString(R.string.context_rename), null)
                .setNegativeButton(android.R.string.cancel, null)
                .setView(layout)
                .setOnDismissListener(dialog -> hideKeyboard(groupChatInfoActivity, InputMethodManager.HIDE_NOT_ALWAYS));

        changeTitleDialog = builder.create();
        changeTitleDialog.show();
        changeTitleDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> changeTitle(input));
    }

    private void changeTitle(EmojiEditText input) {
        String title = input.getText().toString();
        if (title.equals("") || title.isEmpty() || title.trim().isEmpty()) {
            logWarning("Input is empty");
            input.setError(getString(R.string.invalid_string));
            input.requestFocus();
        } else if (!isAllowedTitle(title)) {
            logWarning("Title is too long");
            input.setError(getString(R.string.title_long));
            input.requestFocus();
        } else {
            logDebug("Positive button pressed - change title");
            chatC.changeTitle(chatHandle, title);
            changeTitleDialog.dismiss();
        }
    }

    public void showConfirmationClearChat() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message = getResources().getString(R.string.confirmation_clear_group_chat);
        builder.setTitle(R.string.title_confirmation_clear_group_chat);
        builder.setMessage(message)
                .setPositiveButton(R.string.general_clear, (dialog, which) -> chatC.clearHistory(chat))
                .setNegativeButton(R.string.general_cancel, null)
                .show();
    }

    public void showConfirmationLeaveChat() {
        logDebug("Chat ID: " + chat.getChatId());

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
        String message = getResources().getString(R.string.confirmation_leave_group_chat);
        builder.setMessage(message)
                .setPositiveButton(R.string.general_leave, (dialog, which) -> chatC.leaveChat(chat))
                .setNegativeButton(R.string.general_cancel, null)
                .show();
    }

    public void inviteContact(String email) {
        new ContactController(this).inviteContact(email);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish CHAT: " + request.getType() + " " + e.getErrorCode());

        if (request.getType() == MegaChatRequest.TYPE_UPDATE_PEER_PERMISSIONS) {
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
                adapter.updateParticipant(index, participants);
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

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getFlag()) {
                    logDebug("Chat archived");
                    Intent intent = new Intent(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED_GROUP);
                    intent.putExtra(CHAT_TITLE, chatTitle);
                    sendBroadcast(intent);
                    finish();
                } else {
                    logDebug("Chat unarchived");
                    showSnackbar(getString(R.string.success_unarchive_chat, chatTitle));
                }
            } else if (request.getFlag()) {
                logError("ERROR WHEN ARCHIVING CHAT " + e.getErrorString());
                showSnackbar(getString(R.string.error_archive_chat, chatTitle));
            } else {
                logError("ERROR WHEN UNARCHIVING CHAT " + e.getErrorString());
                showSnackbar(getString(R.string.error_unarchive_chat, chatTitle));
            }

            updateAdapterHeader();
        } else if (request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM) {
            logDebug("Remove participant: " + request.getUserHandle());
            logDebug("My user handle: " + megaChatApi.getMyUserHandle());

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getUserHandle() == INVALID_HANDLE) {
                    logDebug("I left the chatroom");
                    finish();
                } else {
                    logDebug("Removed from chat");

                    chat = megaChatApi.getChatRoom(chatHandle);
                    logDebug("Peers after onChatListItemUpdate: " + chat.getPeerCount());
                    updateParticipants();
                    showSnackbar(getString(R.string.remove_participant_success));
                }
            } else if (request.getUserHandle() == -1) {
                logError("ERROR WHEN LEAVING CHAT" + e.getErrorString());
                showSnackbar("Error.Chat not left");
            } else {
                logError("ERROR WHEN TYPE_REMOVE_FROM_CHATROOM " + e.getErrorString());
                showSnackbar(getString(R.string.remove_participant_error));
            }
        } else if (request.getType() == MegaChatRequest.TYPE_EDIT_CHATROOM_NAME) {
            logDebug("Change title");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getText() != null) {
                    updateAdapterHeader();
                }
            } else {
                logError("ERROR WHEN TYPE_EDIT_CHATROOM_NAME " + e.getErrorString());
            }
        } else if (request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY) {
            logDebug("Truncate history request finish!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Ok. Clear history done");
                showSnackbar(getString(R.string.clear_history_success));
            } else {
                logError("Error clearing history: " + e.getErrorString());
                showSnackbar(getString(R.string.clear_history_error));
            }
        } else if (request.getType() == MegaChatRequest.TYPE_INVITE_TO_CHATROOM) {
            logDebug("Invite to chatroom request finish!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Ok. Invited");
                showSnackbar(getString(R.string.add_participant_success));

                if (request.getChatHandle() == chatHandle) {
                    logDebug("Changes in my chat");
                    chat = megaChatApi.getChatRoom(chatHandle);
                    logDebug("Peers after onChatListItemUpdate: " + chat.getPeerCount());
                    updateParticipants();
                } else {
                    logWarning("Changes NOT interested in");
                }
            } else if (e.getErrorCode() == MegaChatError.ERROR_EXIST) {
                logError("Error inviting ARGS: " + e.getErrorString());
                showSnackbar(getString(R.string.add_participant_error_already_exists));
            } else {
                logError("Error inviting: " + e.getErrorString() + " " + e.getErrorCode());
                showSnackbar(getString(R.string.add_participant_error));
            }
        } else if (request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            logDebug("Create chat request finish!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("Open new chat");
                Intent intent = new Intent(this, ChatActivityLollipop.class);
                intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
                intent.putExtra("CHAT_ID", request.getChatHandle());
                this.startActivity(intent);
            } else {
                logError("ERROR WHEN CREATING CHAT " + e.getErrorString());
                showSnackbar(getString(R.string.create_chat_error));
            }
        } else if (request.getType() == MegaChatRequest.TYPE_CHAT_LINK_HANDLE) {
            logDebug("MegaChatRequest.TYPE_CHAT_LINK_HANDLE finished!!!");
            if (request.getFlag() == false) {
                if (request.getNumRetry() == 0) {
//                    Query chat link
                    if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                        chatLink = request.getText();
                        showShareChatLinkDialog(groupChatInfoActivity, chat, chatLink);
                        return;
                    } else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                        logError("The chatroom isn't grupal or public");
                    } else if (e.getErrorCode() == MegaChatError.ERROR_NOENT) {
                        logError("The chatroom doesn't exist or the chatid is invalid");
                    } else if (e.getErrorCode() == MegaChatError.ERROR_ACCESS) {
                        logError("The chatroom doesn't have a topic or the caller isn't an operator");
                    } else {
                        logError("Error TYPE_CHAT_LINK_HANDLE " + e.getErrorCode());
                    }
                    if (chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR) {
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
                    } else {
                        logError("Error TYPE_CHAT_LINK_HANDLE " + e.getErrorCode());
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                }
            } else {
                if (request.getNumRetry() == 0) {
                    logDebug("Removing chat link");
                    if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                        chatLink = null;
                        showSnackbar(getString(R.string.chat_link_deleted));
                    } else {
                        if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                            logError("The chatroom isn't grupal or public");
                        } else if (e.getErrorCode() == MegaChatError.ERROR_NOENT) {
                            logError("The chatroom doesn't exist or the chatid is invalid");
                        } else if (e.getErrorCode() == MegaChatError.ERROR_ACCESS) {
                            logError("The chatroom doesn't have a topic or the caller isn't an operator");
                        } else {
                            logError("Error TYPE_CHAT_LINK_HANDLE " + e.getErrorCode());
                        }
                        showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
                    }
                }
            }
            updateAdapterHeader();
        } else if (request.getType() == MegaChatRequest.TYPE_SET_PRIVATE_MODE) {
            logDebug("MegaChatRequest.TYPE_SET_PRIVATE_MODE finished!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                chatLink = null;
                logDebug("Chat is PRIVATE now");
                updateAdapterHeader();
                return;
            } else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                logError("NOT public chatroom");
            } else if (e.getErrorCode() == MegaChatError.ERROR_NOENT) {
                logError("Chatroom not FOUND");
            } else if (e.getErrorCode() == MegaChatError.ERROR_ACCESS) {
                logError("NOT privileges or private chatroom");
            }

            showSnackbar(getString(R.string.general_error) + ": " + e.getErrorString());
        }
    }

    public void showSnackbar(String s) {
        showSnackbar(containerLayout, s);
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

        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());
            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    logDebug("OK INVITE CONTACT: " + request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    }
                    return;
                } else if (e.getErrorCode() == MegaError.API_EEXIST) {
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

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        if (item.getChatId() != chatHandle) {
            return;
        }

        logDebug("Chat ID: " + item.getChatId());
        chat = megaChatApi.getChatRoom(chatHandle);

        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)) {
            logDebug("Change participants");
            updateParticipants();
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV)) {
            logDebug("Change status: CHANGE_TYPE_OWN_PRIV");
            updateAdapterHeader();
            updateParticipants();
            supportInvalidateOptionsMenu();
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_TITLE)) {
            logDebug("Change status: CHANGE_TYPE_TITLE");
            updateAdapterHeader();
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)) {
            logDebug("CHANGE_TYPE_CLOSED");
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
            logDebug("CHANGE_TYPE_UNREAD_COUNT");
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UPDATE_PREVIEWERS)) {
            updateAdapterHeader();
        } else {
            logDebug("Changes other: " + item.getChanges());
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
        if (userHandle == megaChatApi.getMyUserHandle()) {
            logDebug("My own status update");
            int position = participants.size() - 1;
            adapter.updateContactStatus(position);
        } else {
            logDebug("Status update for the user: " + userHandle);
            int indexToReplace = -1;
            ListIterator<MegaChatParticipant> itrReplace = participants.listIterator();
            while (itrReplace.hasNext()) {
                MegaChatParticipant participant = itrReplace.next();
                if (participant != null) {
                    if (participant.getHandle() == userHandle) {
                        if (status != MegaChatApi.STATUS_ONLINE && status != MegaChatApi.STATUS_BUSY && status != MegaChatApi.STATUS_INVALID) {
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
                adapter.updateContactStatus(indexToReplace);
            }
        }
    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {
        logDebug("onChatPresenceConfigUpdate");
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {
        logDebug("Chat ID: " + chatid + ", New state: " + newState);

        MegaChatRoom chatRoom = api.getChatRoom(chatid);
        if (MegaApplication.isWaitingForCall() && newState == MegaChatApi.CHAT_CONNECTION_ONLINE
                && chatRoom != null && chatRoom.getPeerHandle(0) == MegaApplication.getUserWaitingForCall()) {
            startCallWithChatOnline(this, api.getChatRoom(chatid));
        }
    }

    public void showConfirmationPrivateChatDialog() {
        logDebug("showConfirmationPrivateChatDialog");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_chat_link_options, null);
        dialogBuilder.setView(dialogView);

        Button actionButton = dialogView.findViewById(R.id.chat_link_button_action);
        actionButton.setText(getString(R.string.general_enable));

        TextView title = dialogView.findViewById(R.id.chat_link_title);
        title.setText(getString(R.string.make_chat_private_option));

        TextView text = dialogView.findViewById(R.id.text_chat_link);
        text.setText(getString(R.string.context_make_private_chat_warning_text));

        TextView secondText = dialogView.findViewById(R.id.second_text_chat_link);
        secondText.setVisibility(View.GONE);

        chatLinkDialog = dialogBuilder.create();

        actionButton.setOnClickListener(v -> {
            chatLinkDialog.dismiss();
            if (chat.getPeerCount() + 1 > MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE) {
                showSnackbar(getString(R.string.warning_make_chat_private));
            } else {
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

        Button actionButton = dialogView.findViewById(R.id.chat_link_button_action);
        actionButton.setText(getString(R.string.get_chat_link_option));

        TextView title = dialogView.findViewById(R.id.chat_link_title);
        title.setText(getString(R.string.get_chat_link_option));

        TextView firstText = dialogView.findViewById(R.id.text_chat_link);
        firstText.setText(getString(R.string.context_create_chat_link_warning_text));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) firstText.getLayoutParams();
        params.bottomMargin = scaleHeightPx(12, getOutMetrics());
        firstText.setLayoutParams(params);

        chatLinkDialog = dialogBuilder.create();

        actionButton.setOnClickListener(v -> {
            chatLinkDialog.dismiss();
            createPublicGroupAndGetLink();
        });

        chatLinkDialog.show();
    }


    public void startConversation(long handle) {
        logDebug("Handle: " + handle);
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
        MegaChatPeerList peers = MegaChatPeerList.createInstance();

        if (chat == null) {
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        } else {
            Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
            this.startActivity(intentOpenChat);
        }
    }

    private void createPublicGroupAndGetLink() {
        long participantsCount = chat.getPeerCount();
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        for (int i = 0; i < participantsCount; i++) {
            logDebug("Add participant: " + chat.getPeerHandle(i) + ", privilege: " + chat.getPeerPrivilege(i));
            peers.addPeer(chat.getPeerHandle(i), chat.getPeerPrivilege(i));
        }

        CreateGroupChatWithPublicLink listener = new CreateGroupChatWithPublicLink(this, getTitleChat(chat));
        megaChatApi.createPublicChat(peers, getTitleChat(chat), listener);
    }


    public void onRequestFinishCreateChat(int errorCode, long chatHandle, boolean publicLink) {
        if (errorCode == MegaChatError.ERROR_OK) {
            Intent intent = new Intent(this, ChatActivityLollipop.class);
            intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra("CHAT_ID", chatHandle);
            if (publicLink) {
                intent.putExtra("PUBLIC_LINK", errorCode);
            }
            this.startActivity(intent);
        } else {
            logError("ERROR WHEN CREATING CHAT " + errorCode);
            showSnackbar(getString(R.string.create_chat_error));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RECORD_AUDIO:
            case REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    controlCallPermissions();
                }
                break;
        }
    }

    /**
     * Method for checking the necessary actions when you have permission to start a call.
     */
    private void controlCallPermissions() {
        if (checkPermissionsCall(this, INVALID_TYPE_PERMISSIONS)) {
            MegaChatRoom chat = megaChatApi.getChatRoomByUser(MegaApplication.getUserWaitingForCall());
            if (chat != null) {
                startCallWithChatOnline(this, chat);
            }
        }
    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
        logDebug("User Handle: " + userhandle + ", Last green: " + lastGreen);
        int state = megaChatApi.getUserOnlineStatus(userhandle);
        if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
            String formattedDate = lastGreenDate(this, lastGreen);

            if (userhandle != megaChatApi.getMyUserHandle()) {
                logDebug("Status last green for the user: " + userhandle);
                ListIterator<MegaChatParticipant> itrReplace = participants.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatParticipant participant = itrReplace.next();
                    if (participant == null) break;

                    if (participant.getHandle() == userhandle) {
                        participant.setLastGreen(formattedDate);
                        adapter.updateContactStatus(itrReplace.nextIndex() - 1);
                        break;
                    }
                }
            }

            logDebug("Date last green: " + formattedDate);
        }
    }

    /**
     * Stores a MegaChatParticipant with their position in the adapter.
     *
     * @param position      position of the participant in the adapter.
     * @param participant   MegaChatParticipant to store.
     */
    public void addParticipantRequest(int position, MegaChatParticipant participant) {
        pendingParticipantRequests.put(position, participant);
    }

    /**
     * If there are participants visibles in the UI without attributes,
     * it launches a request to ask for them.
     *
     * @param scrollState   current scroll state of the RecyclerView
     */
    private void checkIfShouldAskForUsersAttributes(int scrollState) {
        if (scrollState != RecyclerView.SCROLL_STATE_IDLE) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }
            return;
        }

        if (pendingParticipantRequests.isEmpty()) return;

        countDownTimer = new CountDownTimer(TIMEOUT, TIMEOUT) {

            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                HashMap<Integer, MegaChatParticipant> copyOfPendingParticipantRequests = new HashMap<>(pendingParticipantRequests);

                MegaHandleList handleList = MegaHandleList.createInstance();
                HashMap<Integer, MegaChatParticipant> participantRequests = new HashMap<>();
                HashMap<Integer, String> participantAvatars = new HashMap<>();
                int firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
                int lastPosition = linearLayoutManager.findLastVisibleItemPosition();

                for (int i = firstPosition; i <= lastPosition; i++) {
                    MegaChatParticipant participant = copyOfPendingParticipantRequests.get(i);
                    if (participant != null) {
                        participantRequests.put(i, participant);
                        long handle = participant.getHandle();
                        if (participant.isEmpty()) {
                            handleList.addMegaHandle(handle);
                        }

                        if (!participant.hasAvatar()) {
                            participantAvatars.put(i, MegaApiAndroid.userHandleToBase64(handle));
                        }
                    }

                    pendingParticipantRequests.remove(i);
                }

                copyOfPendingParticipantRequests.clear();
                requestUserAttributes(handleList, participantRequests, participantAvatars);
            }
        }.start();
    }

    /**
     * Requests the attributes of some MegaChatParticipants
     *
     * @param handleList            MegaHandleList in which the participant's handles are stored to ask for their attributes
     * @param participantRequests   HashMap in which the participants and their positions in the adapter are stored
     * @param participantAvatars    HastMap in which the participants' handles and their positions in the adapter are stored
     */
    private void requestUserAttributes(MegaHandleList handleList, HashMap<Integer, MegaChatParticipant> participantRequests, HashMap<Integer, String> participantAvatars) {
        if (handleList.size() > 0) {
            megaChatApi.loadUserAttributes(chatHandle, handleList, new GetPeerAttributesListener(this, participantRequests));
        }

        for (Integer positionInAdapter : participantAvatars.keySet()) {
            String handle = participantAvatars.get(positionInAdapter);
            if (!isTextEmpty(handle)) {
                megaApi.getUserAvatar(handle, buildAvatarFile(this, handle + JPG_EXTENSION).getAbsolutePath(), new GetAttrUserListener(this, positionInAdapter));
            }
        }
    }

    /**
     * Updates in the adapter the requested participants in loadUserAttributes().
     *
     * @param chatHandle            identifier of the current MegaChatRoom
     * @param participantUpdates    list of requested participants
     * @param handleList            list of the participants' identifiers
     */
    public void updateParticipants(long chatHandle, final HashMap<Integer, MegaChatParticipant> participantUpdates, MegaHandleList handleList) {
        if (chatHandle != chat.getChatId() || megaChatApi.getChatRoom(chatHandle) == null || adapter == null)
            return;

        chat = megaChatApi.getChatRoom(chatHandle);

        for (int i = 0; i < handleList.size(); i++) {
            long handle = handleList.get(i);
            chatC.setNonContactAttributesInDB(handle);

            for (Integer positionInAdapter : participantUpdates.keySet()) {
                if (participantUpdates.get(positionInAdapter).getHandle() == handle) {
                    int positionInArray = adapter.getParticipantPositionInArray(positionInAdapter);
                    if (positionInArray >= 0 && positionInArray < participants.size() && participants.get(positionInArray).getHandle() == handle) {
                        MegaChatParticipant participant = participantUpdates.get(positionInAdapter);
                        participant.setEmail(chatC.getParticipantEmail(handle));
                        participant.setFullName(chatC.getParticipantFullName(handle));
                        participant.setPrivilege(chat.getPeerPrivilegeByHandle(handle));

                        if (hasParticipantAttributes(participant)) {
                            participants.set(positionInArray, participant);
                            adapter.updateParticipant(positionInArray, participants);
                        }
                    }
                }
            }
        }
    }

    /**
     * Updates the participant's avatar in the adapter.
     *
     * @param positionInAdapter participant's position in the adapter
     * @param emailOrHandle     participant's email or handle in Base64
     */
    public void updateParticipantAvatar (int positionInAdapter, String emailOrHandle) {
        int positionInArray = adapter.getParticipantPositionInArray(positionInAdapter);
        boolean isEmail = EMAIL_ADDRESS.matcher(emailOrHandle).matches();

        if (positionInArray >= 0 && positionInArray < participants.size()
                && ((isEmail && participants.get(positionInArray).getEmail().equals(emailOrHandle)
                || (participants.get(positionInArray).getHandle() == MegaApiJava.base64ToUserHandle(emailOrHandle))))) {
            Bitmap avatar = getAvatarBitmap(emailOrHandle);
            if (avatar != null) {
                adapter.notifyItemChanged(positionInAdapter);
            }
        }
    }

    /**
     * Updates a participant in the participants' list.
     *
     * @param position      position of the participant in the list
     * @param participant   MegaChatParticipant to update
     */
    public void updateParticipant(int position, MegaChatParticipant participant) {
        participants.set(position, participant);
    }

    /**
     * Checks if a participant has attributes.
     * If so, the mail and full name do not have to be empty.
     *
     * @param participant   MegaChatParticipant to check.
     * @return True if the participant was correctly updated, false otherwise.
     */
    public boolean hasParticipantAttributes(MegaChatParticipant participant) {
        return !isTextEmpty(participant.getEmail()) || !isTextEmpty(participant.getFullName());
    }

    private void updateAdapterHeader() {
        if (adapter != null) {
            adapter.notifyItemChanged(0);
        }
    }

    public long getChatHandle() {
        return chatHandle;
    }

    public long getSelectedHandleParticipant() {
        return selectedHandleParticipant;
    }

    /**
     * Checks if the notifications of the chat are enabled.
     *
     * @return true if the notifications of the chat are enabled, false otherwise
     */
    public boolean areGeneralChatNotificationsAvailable() {
        return chatSettings != null || chatSettings.getNotificationsEnabled() == null ? true : Boolean.parseBoolean(chatSettings.getNotificationsEnabled());
    }

    public void setChatLink(String chatLink) {
        this.chatLink = chatLink;
    }

    public void setChatMuted() {
        chatMuted = !chatMuted;
    }

    public boolean isChatMuted() {
        return chatMuted;
    }

    public ChatController getChatC() {
        return chatC;
    }

    public void updateParticipants() {
        MegaChatRoom chatRoomUpdated = megaChatApi.getChatRoom(chatHandle);
        if (chatRoomUpdated == null) {
            logWarning("The chatRoom updated is null");
            return;
        }

        chat = chatRoomUpdated;
        participants.clear();
        setParticipants();
    }
}
