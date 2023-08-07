package mega.privacy.android.app.main.megachat;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_CREDENTIALS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_SHOW_MESSAGES;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static mega.privacy.android.app.utils.ContactUtil.getNicknameContact;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaContactsAttachedAdapter;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ContactAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage;
import mega.privacy.android.domain.entity.Contact;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

public class ContactAttachmentActivity extends PasscodeActivity implements MegaRequestListenerInterface, MegaChatRequestListenerInterface, OnClickListener {

    ActionBar aB;
    Toolbar tB;
    public String selectedEmail;

    RelativeLayout container;
    RecyclerView listView;
    View separator;
    Button actionButton;
    Button cancelButton;
    LinearLayout optionsBar;
    LinearLayoutManager mLayoutManager;

    boolean inviteAction = false;

    ChatController cC;

    AndroidMegaChatMessage message = null;
    public long chatId;
    public long messageId;

    ArrayList<Contact> contacts;

    MegaContactsAttachedAdapter adapter;

    DisplayMetrics outMetrics;

    private ContactAttachmentBottomSheetDialogFragment bottomSheetDialogFragment;

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
        Timber.d("onCreate");
        super.onCreate(savedInstanceState);

        if (shouldRefreshSessionDueToSDK() || shouldRefreshSessionDueToKarere()) {
            return;
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        cC = new ChatController(this);

        Intent intent = getIntent();
        if (intent != null) {
            chatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
            messageId = intent.getLongExtra(MESSAGE_ID, MEGACHAT_INVALID_HANDLE);
            Timber.d("Chat ID: %d, Message ID: %d", chatId, messageId);
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        }

        if (message != null) {
            contacts = new ArrayList<>();

            for (int i = 0; i < message.getMessage().getUsersCount(); i++) {
                String email = message.getMessage().getUserEmail(i);
                Contact contactDB = dbH.findContactByEmail(email);
                if (contactDB != null) {
                    contacts.add(contactDB);
                } else {
                    long handle = message.getMessage().getUserHandle(i);
                    Contact newContactDB = new Contact(handle,
                            email,
                            "",
                            message.getMessage().getUserName(i),
                            "");
                    contacts.add(newContactDB);
                }
            }
        } else {
            finish();
        }

        setContentView(R.layout.activity_contact_attachment_chat);

        //Set toolbar
        tB = (Toolbar) findViewById(R.id.toolbar_contact_attachment_chat);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(getString(R.string.activity_title_contacts_attached));

        aB.setSubtitle(message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle() ? megaChatApi.getMyFullname()
                : cC.getParticipantFullName(message.getMessage().getUserHandle()));

        container = (RelativeLayout) findViewById(R.id.contact_attachment_chat);

        optionsBar = (LinearLayout) findViewById(R.id.options_contact_attachment_chat_layout);
        separator = (View) findViewById(R.id.contact_attachment_chat_separator_3);

        actionButton = (Button) findViewById(R.id.contact_attachment_chat_option_button);
        actionButton.setOnClickListener(this);

        for (Contact contactDB : contacts) {
            if (contactDB.getEmail() == null) continue;
            MegaUser checkContact = megaApi.getContact(contactDB.getEmail());

            if (!contactDB.getEmail().equals(megaApi.getMyEmail()) &&
                    (checkContact == null || checkContact.getVisibility() != MegaUser.VISIBILITY_VISIBLE)) {
                inviteAction = true;
                break;
            }
        }

        actionButton.setText(inviteAction ? R.string.menu_add_contact : R.string.group_chat_start_conversation_label);

        cancelButton = (Button) findViewById(R.id.contact_attachment_chat_cancel_button);
        cancelButton.setOnClickListener(this);

        listView = (RecyclerView) findViewById(R.id.contact_attachment_chat_view_browser);
        listView.setClipToPadding(false);
        listView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(mLayoutManager);
        listView.setItemAnimator(noChangeRecyclerViewItemAnimator());

        if (adapter == null) {
            adapter = new MegaContactsAttachedAdapter(this, contacts, listView);
        }

        adapter.setPositionClicked(-1);
        adapter.setMultipleSelect(false);

        listView.setAdapter(adapter);

        IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_CREDENTIALS);
        registerReceiver(contactUpdateReceiver, contactUpdateFilter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                finish();
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (megaApi != null) {
            megaApi.removeRequestListener(this);
        }

        unregisterReceiver(contactUpdateReceiver);
    }

    public void showOptionsPanel(String email) {
        Timber.d("showOptionsPanel");

        if (email == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedEmail = email;
        bottomSheetDialogFragment = new ContactAttachmentBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        if (request.getType() == MegaRequest.TYPE_SHARE) {
            Timber.d("Share");
        }
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("onRequestFinish: %d__%s", request.getType(), request.getRequestString());

        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.getNumber());

            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("OK INVITE CONTACT: %s", request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    }
                } else {
                    Timber.e("Code: %s", e.getErrorString());
                    if (e.getErrorCode() == MegaError.API_EEXIST) {
                        showSnackbar(getString(R.string.context_contact_already_invited, request.getEmail()));
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode() == MegaError.API_EARGS) {
                        showSnackbar(getString(R.string.error_own_email_as_contact));
                    } else {
                        showSnackbar(getString(R.string.general_error));
                    }
                    Timber.e("ERROR: %s___%s", e.getErrorCode(), e.getErrorString());
                }
            }
        }
    }


    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
                                        MegaError e) {
        Timber.w("onRequestTemporaryError");
    }

    public void itemClick(int position) {
        Timber.d("Position: %s", position);

        Contact c = contacts.get(position);
        if (c != null) {
            MegaUser contact = megaApi.getContact(c.getEmail());

            if (contact != null) {
                if (contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
                    long contactHandle = c.getUserId();
                    boolean isChatRoomOpen = chat != null && !chat.isGroup() && contactHandle == chat.getPeerHandle(0);
                    ContactUtil.openContactInfoActivity(this, c.getEmail(), isChatRoomOpen);
                } else {
                    Timber.d("The user is not contact");
                    showSnackbar(getString(R.string.alert_user_is_not_contact));
                }
            } else {
                Timber.e("The contact is null");
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.contact_attachment_chat_option_button) {
            Timber.d("Click on ACTION button");

            if (inviteAction) {
                ArrayList<String> contactEmails = new ArrayList<>();
                ContactController contactControllerC = new ContactController(this);
                for (int i = 0; i < contacts.size(); i++) {
                    Contact contact = contacts.get(i);
                    MegaUser checkContact = megaApi.getContact(contact.getEmail());
                    if (!contact.getEmail().equals(megaApi.getMyEmail()) && (checkContact == null || checkContact.getVisibility() != MegaUser.VISIBILITY_VISIBLE)) {
                        contactEmails.add(contact.getEmail());
                    }
                }

                if (!contactEmails.isEmpty()) {
                    contactControllerC.inviteMultipleContacts(contactEmails);
                }
            } else {
                ArrayList<Long> contactHandles = new ArrayList<>();

                for (int i = 0; i < contacts.size(); i++) {
                    contactHandles.add(contacts.get(i).getUserId());
                }

                startGroupConversation(contactHandles);
            }
        } else if (id == R.id.contact_attachment_chat_cancel_button) {
            Timber.d("Click on Cancel button");
            finish();
        }
    }

    public void setPositionClicked(int positionClicked) {
        if (adapter != null) {
            adapter.setPositionClicked(positionClicked);
        }
    }

    public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void showSnackbar(String s) {
        showSnackbar(container, s);
    }

    public void startConversation(long handle) {
        Timber.d("Handle: %s", handle);
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if (chat == null) {
            Timber.d("No chat, create it!");
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        } else {
            Timber.d("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivity.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra(CHAT_ID, chat.getChatId());
            finish();
            this.startActivity(intentOpenChat);
        }
    }

    private void startGroupConversation(ArrayList<Long> userHandles) {
        MegaChatPeerList peers = MegaChatPeerList.createInstance();

        for (long handle : userHandles) {
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
        }

        megaChatApi.createChat(true, peers, this);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        Timber.d("onRequestStart: %s", request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.d("onRequestFinish: %s", request.getRequestString());

        if (request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            Timber.d("Create chat request finish!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                Timber.d("Open new chat");
                Intent intent = new Intent(this, ChatActivity.class);
                intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
                intent.putExtra(CHAT_ID, request.getChatHandle());
                finish();
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                this.startActivity(intent);

            } else {
                Timber.e("ERROR WHEN CREATING CHAT %s", e.getErrorString());
                showSnackbar(getString(R.string.create_chat_error));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    private void updateAdapter(long handleReceived) {
        if (contacts == null || contacts.isEmpty()) return;

        for (int i = 0; i < contacts.size(); i++) {
            String email = contacts.get(i).getEmail();
            MegaUser user = megaApi.getContact(email);
            long handleUser = user.getHandle();
            if (handleUser == handleReceived) {
                Contact contact = contacts.get(i);
                adapter.updateContact(new Contact(
                        contact.getUserId(),
                        email,
                        getNicknameContact(email),
                        contact.getFirstName(),
                        contact.getLastName()
                ), i);
                break;
            }
        }
    }
}

