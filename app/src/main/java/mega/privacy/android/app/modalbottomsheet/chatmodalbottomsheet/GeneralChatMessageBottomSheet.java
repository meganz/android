package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatReactionsView;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class GeneralChatMessageBottomSheet extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaNode node;
    private MegaNodeList nodeList;
    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private int positionMessage;
    private long handle = INVALID_HANDLE;
    private ChatController chatC;
    private MegaChatRoom chatRoom;
    private LinearLayout reactionsLayout;
    private ChatReactionsView reactionsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_ID);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_ID);
            positionMessage = savedInstanceState.getInt(POSITION_SELECTED_MESSAGE, INVALID_POSITION);
            handle = savedInstanceState.getLong(HANDLE, INVALID_HANDLE);
        } else {
            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;
            positionMessage = ((ChatActivityLollipop) context).selectedPosition;
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        chatRoom = megaChatApi.getChatRoom(chatId);
        chatC = new ChatController(context);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_general_chat_messages, null);
        reactionsLayout = contentView.findViewById(R.id.reactions_layout);
        reactionsFragment = contentView.findViewById(R.id.fragment_container_reactions);
        items_layout = contentView.findViewById(R.id.items_layout);

        RelativeLayout optionOpenWith = contentView.findViewById(R.id.open_with_layout);
        LinearLayout forwardSeparator = contentView.findViewById(R.id.forward_separator);
        RelativeLayout optionForward = contentView.findViewById(R.id.forward_layout);
        LinearLayout editSeparator = contentView.findViewById(R.id.edit_separator);
        RelativeLayout optionEdit = contentView.findViewById(R.id.edit_layout);
        LinearLayout copySeparator = contentView.findViewById(R.id.copy_separator);
        RelativeLayout optionCopy = contentView.findViewById(R.id.copy_layout);
        LinearLayout shareSeparator = contentView.findViewById(R.id.share_separator);
        RelativeLayout optionShare = contentView.findViewById(R.id.share_layout);
        LinearLayout selectSeparator = contentView.findViewById(R.id.select_separator);
        RelativeLayout optionSelect = contentView.findViewById(R.id.select_layout);
        LinearLayout infoSeparator = contentView.findViewById(R.id.info_separator);
        RelativeLayout optionViewContacts = contentView.findViewById(R.id.option_view_layout);
        RelativeLayout optionInfoContacts = contentView.findViewById(R.id.option_info_layout);
        LinearLayout inviteSeparator = contentView.findViewById(R.id.invite_separator);
        RelativeLayout optionStartConversation = contentView.findViewById(R.id.option_start_conversation_layout);
        RelativeLayout optionInviteContact = contentView.findViewById(R.id.option_invite_layout);
        LinearLayout infoFileSeparator = contentView.findViewById(R.id.info_file_separator);
        RelativeLayout optionImport = contentView.findViewById(R.id.option_import_layout);
        RelativeLayout optionDownload = contentView.findViewById(R.id.option_download_layout);
        RelativeLayout optionSaveOffline = contentView.findViewById(R.id.option_save_offline_layout);
        LinearLayout deleteSeparator = contentView.findViewById(R.id.delete_separator);
        RelativeLayout optionDelete = contentView.findViewById(R.id.delete_layout);
        TextView textDelete = contentView.findViewById(R.id.delete_text);

        optionOpenWith.setOnClickListener(this);
        optionForward.setOnClickListener(this);
        optionEdit.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionShare.setOnClickListener(this);
        optionSelect.setOnClickListener(this);
        optionViewContacts.setOnClickListener(this);
        optionInfoContacts.setOnClickListener(this);
        optionStartConversation.setOnClickListener(this);
        optionInviteContact.setOnClickListener(this);
        optionImport.setOnClickListener(this);
        optionDownload.setOnClickListener(this);
        optionSaveOffline.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        boolean shouldReactionOptionBeVisible = chatRoom != null && message != null &&
                context instanceof ChatActivityLollipop && shouldReactionBeClicked(chatRoom) &&
                !((ChatActivityLollipop) context).hasMessagesRemoved(message.getMessage()) &&
                !message.isUploading();

        if (shouldReactionOptionBeVisible) {
            reactionsFragment.init(context, this, chatId, messageId, positionMessage);
            reactionsLayout.setVisibility(View.VISIBLE);
        } else {
            reactionsLayout.setVisibility(View.GONE);
        }

        if (message == null || message.getMessage() == null || chatRoom == null || ((ChatActivityLollipop) context).hasMessagesRemoved(message.getMessage()) || message.isUploading()) {
            optionOpenWith.setVisibility(View.GONE);
            forwardSeparator.setVisibility(View.GONE);
            optionForward.setVisibility(View.GONE);
            editSeparator.setVisibility(View.GONE);
            optionEdit.setVisibility(View.GONE);
            copySeparator.setVisibility(View.GONE);
            optionCopy.setVisibility(View.GONE);
            shareSeparator.setVisibility(View.GONE);
            optionShare.setVisibility(View.GONE);
            selectSeparator.setVisibility(View.GONE);
            optionSelect.setVisibility(View.GONE);
            infoSeparator.setVisibility(View.GONE);
            optionViewContacts.setVisibility(View.GONE);
            optionInfoContacts.setVisibility(View.GONE);
            inviteSeparator.setVisibility(View.GONE);
            optionStartConversation.setVisibility(View.GONE);
            optionInviteContact.setVisibility(View.GONE);
            infoFileSeparator.setVisibility(View.GONE);
            optionImport.setVisibility(View.GONE);
            optionDownload.setVisibility(View.GONE);
            optionSaveOffline.setVisibility(View.GONE);
            deleteSeparator.setVisibility(View.GONE);
            optionDelete.setVisibility(View.GONE);
            return;

        } else {
            int typeMessage = message.getMessage().getType();

            if (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                nodeList = message.getMessage().getMegaNodeList();

                if (nodeList == null || nodeList.size() == 0) {
                    logWarning("Error: nodeList is NULL or empty");
                    return;
                }

                node = handle == INVALID_HANDLE ? nodeList.get(0) : getNodeByHandle(handle);
                if (node == null) {
                    logWarning("Node is NULL");
                    return;
                }
            }

            optionSelect.setVisibility(View.VISIBLE);

            optionCopy.setVisibility((typeMessage == MegaChatMessage.TYPE_NORMAL ||
                    (typeMessage == MegaChatMessage.TYPE_CONTAINS_META &&
                            message.getMessage().getContainsMeta() != null &&
                            message.getMessage().getContainsMeta().getType() != MegaChatContainsMeta.CONTAINS_META_INVALID &&
                            message.getMessage().getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW)) ? View.VISIBLE : View.GONE);


            if (((chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO) && !chatRoom.isPreview())) {
                optionForward.setVisibility(View.GONE);
                optionEdit.setVisibility(View.GONE);
                optionDelete.setVisibility(View.GONE);
                optionShare.setVisibility(View.GONE);

            } else {
                optionShare.setVisibility(typeMessage != MegaChatMessage.TYPE_NODE_ATTACHMENT || !isOnline(context) || chatC.isInAnonymousMode() || message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle() ? View.GONE : View.VISIBLE);
                optionForward.setVisibility(!isOnline(context) || chatC.isInAnonymousMode() ? View.GONE : View.VISIBLE);

                if (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()
                        && message.getMessage().isEditable()
                        && (typeMessage == MegaChatMessage.TYPE_NORMAL || typeMessage == MegaChatMessage.TYPE_CONTAINS_META)) {
                    optionEdit.setVisibility(View.VISIBLE);
                } else {
                    optionEdit.setVisibility(View.GONE);
                }

                if (message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle() || !message.getMessage().isDeletable()) {
                    optionDelete.setVisibility(View.GONE);
                } else {
                    if (message.getMessage().getType() == MegaChatMessage.TYPE_NORMAL ||
                            (message.getMessage().getType() == MegaChatMessage.TYPE_CONTAINS_META &&
                                    message.getMessage().getContainsMeta() != null && message.getMessage().getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION)) {
                        textDelete.setText(getString(R.string.delete_button));
                    } else {
                        textDelete.setText(getString(R.string.context_remove));
                    }
                    optionDelete.setVisibility(View.VISIBLE);
                }
            }

            optionOpenWith.setVisibility(typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT &&
                    (MimeTypeList.typeForName(node.getName()).isVideoReproducible() ||
                            MimeTypeList.typeForName(node.getName()).isVideo() ||
                            MimeTypeList.typeForName(node.getName()).isAudio()
                            || MimeTypeList.typeForName(node.getName()).isImage() ||
                            MimeTypeList.typeForName(node.getName()).isPdf()) ? View.VISIBLE : View.GONE);

            optionDownload.setVisibility(typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT ? View.VISIBLE : View.GONE);
            optionImport.setVisibility(typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT && !chatC.isInAnonymousMode() ? View.VISIBLE : View.GONE);
            optionSaveOffline.setVisibility(typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT && !chatC.isInAnonymousMode() ? View.VISIBLE : View.GONE);

            if (typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                long userCount = message.getMessage().getUsersCount();
                long userHandle = message.getMessage().getUserHandle(0);
                String userEmail = message.getMessage().getUserEmail(0);

                optionInfoContacts.setVisibility((userCount == 1 &&
                        userHandle != megaChatApi.getMyUserHandle() &&
                        megaApi.getContact(userEmail) != null &&
                        megaApi.getContact(userEmail).getVisibility() == MegaUser.VISIBILITY_VISIBLE) ? View.VISIBLE : View.GONE);

                optionViewContacts.setVisibility(userCount > 1 ? View.VISIBLE : View.GONE);

                if(userCount == 1){
                    optionInviteContact.setVisibility(userHandle != megaChatApi.getMyUserHandle() &&
                            (megaApi.getContact(userEmail) == null ||
                                    megaApi.getContact(userEmail).getVisibility() != MegaUser.VISIBILITY_VISIBLE) ? View.VISIBLE : View.GONE);

                    optionStartConversation.setVisibility(userHandle != megaChatApi.getMyUserHandle() &&
                            megaApi.getContact(userEmail) != null &&
                            megaApi.getContact(userEmail).getVisibility() == MegaUser.VISIBILITY_VISIBLE &&
                            (chatRoom.isGroup() || userHandle != chatRoom.getPeerHandle(0)) ? View.VISIBLE : View.GONE);
                } else {
                    optionStartConversation.setVisibility(View.VISIBLE);
                    optionInviteContact.setVisibility(View.GONE);

                    for (int i = 0; i < userCount; i++) {
                        String email = message.getMessage().getUserEmail(i);
                        MegaUser contact = megaApi.getContact(email);
                        if (contact == null || contact.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
                            optionStartConversation.setVisibility(View.GONE);
                            break;
                        }
                    }

                }
            }
        }


        forwardSeparator.setVisibility(optionOpenWith.getVisibility() == View.VISIBLE &&
                optionForward.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        editSeparator.setVisibility(optionForward.getVisibility() == View.VISIBLE &&
                optionEdit.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        copySeparator.setVisibility((optionEdit.getVisibility() == View.VISIBLE ||
                optionForward.getVisibility() == View.VISIBLE) &&
                optionCopy.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        shareSeparator.setVisibility(optionForward.getVisibility() == View.VISIBLE &&
                optionShare.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        selectSeparator.setVisibility((optionSelect.getVisibility() == View.VISIBLE &&
                (optionForward.getVisibility() == View.VISIBLE ||
                        optionCopy.getVisibility() == View.VISIBLE)) ? View.VISIBLE : View.GONE);

        infoSeparator.setVisibility((optionViewContacts.getVisibility() == View.VISIBLE ||
                optionInfoContacts.getVisibility() == View.VISIBLE) &&
                optionSelect.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        inviteSeparator.setVisibility((optionStartConversation.getVisibility() == View.VISIBLE ||
                optionInviteContact.getVisibility() == View.VISIBLE) &&
                (optionViewContacts.getVisibility() == View.VISIBLE ||
                        optionInfoContacts.getVisibility() == View.VISIBLE ||
                        selectSeparator.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);

        infoFileSeparator.setVisibility((optionImport.getVisibility() == View.VISIBLE ||
                optionDownload.getVisibility() == View.VISIBLE ||
                optionSaveOffline.getVisibility() == View.VISIBLE) &&
                optionSelect.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        deleteSeparator.setVisibility(optionDelete.getVisibility());

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    public MegaNode getNodeByHandle(long handle) {
        for (int i = 0; i < nodeList.size(); i++) {
            MegaNode node = nodeList.get(i);
            if (node.getHandle() == handle) {
                return node;
            }
        }
        return null;
    }

    @Override
    public void onClick(View view) {
        if (message == null) {
            logWarning("The message is NULL");
            return;
        }

        if (!isOnline(context)) {
            ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), INVALID_HANDLE);
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        Intent i;
        switch (view.getId()) {
            case R.id.open_with_layout:
                if (node == null) {
                    logWarning("The selected node is NULL");
                    return;
                }
                openWith(node);
                break;

            case R.id.forward_layout:
                ((ChatActivityLollipop) context).forwardMessages(messagesSelected);
                break;

            case R.id.edit_layout:
                ((ChatActivityLollipop) context).editMessage(messagesSelected);
                break;

            case R.id.copy_layout:
                String text = ((ChatActivityLollipop) context).copyMessage(message);
                ((ChatActivityLollipop) context).copyToClipboard(text);
                break;

            case R.id.share_layout:
                if (node == null) {
                    logWarning("The selected node is NULL");
                    return;
                }
                shareNode(context, node);
                break;

            case R.id.select_layout:
                ((ChatActivityLollipop) context).activateActionModeWithItem(positionMessage);
                break;

            case R.id.option_view_layout:
                logDebug("View option");
                i = new Intent(context, ContactAttachmentActivityLollipop.class);
                i.putExtra("chatId", chatId);
                i.putExtra(MESSAGE_ID, messageId);
                context.startActivity(i);
                break;

            case R.id.option_info_layout:
                if (!isOnline(context)) {
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra(NAME, message.getMessage().getUserEmail(0));
                context.startActivity(i);
                break;

            case R.id.option_invite_layout:
                if (!isOnline(context)) {
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                ContactController cC = new ContactController(context);
                ArrayList<String> contactEmails;
                long usersCount = message.getMessage().getUsersCount();

                if (usersCount == 1) {
                    cC.inviteContact(message.getMessage().getUserEmail(0));
                } else {
                    logDebug("Num users to invite: " + usersCount);
                    contactEmails = new ArrayList<>();

                    for (int j = 0; j < usersCount; j++) {
                        String userMail = message.getMessage().getUserEmail(j);
                        contactEmails.add(userMail);
                    }
                    cC.inviteMultipleContacts(contactEmails);
                }
                break;

            case R.id.option_start_conversation_layout:
                long numUsers = message.getMessage().getUsersCount();

                if (numUsers == 1) {
                    ((ChatActivityLollipop) context).startConversation(message.getMessage().getUserHandle(0));
                } else {
                    logDebug("Num users to invite: " + numUsers);
                    ArrayList<Long> contactHandles = new ArrayList<>();

                    for (int j = 0; j < numUsers; j++) {
                        long userHandle = message.getMessage().getUserHandle(j);
                        contactHandles.add(userHandle);
                    }
                    ((ChatActivityLollipop) context).startGroupConversation(contactHandles);
                }
                break;

            case R.id.option_download_layout:
                if (node == null) {
                    logWarning("The selected node is NULL");
                    return;
                }
                chatC.prepareForChatDownload(nodeList);
                break;

            case R.id.option_import_layout:
                if (node == null) {
                    logWarning("The selected node is NULL");
                    return;
                }
                chatC.importNode(messageId, chatId);
                break;

            case R.id.option_save_offline_layout:
                if (message == null) {
                    logWarning("Message is NULL");
                    return;
                }
                ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
                messages.add(message);
                chatC.saveForOfflineWithAndroidMessages(messages, megaChatApi.getChatRoom(chatId));
                break;

            case R.id.delete_layout:
                ((ChatActivityLollipop) context).showConfirmationDeleteMessages(messagesSelected, chatRoom);
                break;
        }
        closeDialog();
    }

    public void closeDialog() {
        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putLong(POSITION_SELECTED_MESSAGE, positionMessage);
        outState.putLong(HANDLE, handle);
    }
}
