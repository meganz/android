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

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatReactionsFragment;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class GeneralChatMessageBottomSheet extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private AndroidMegaChatMessage message = null;
    private long chatId;
    private long messageId;
    private int positionMessage;
    private ChatController chatC;
    private MegaChatRoom chatRoom;

    private LinearLayout reactionsLayout;
    private ChatReactionsFragment reactionsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!(context instanceof ChatActivityLollipop))
            return;

        if (savedInstanceState != null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_ID);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_ID);
            positionMessage = savedInstanceState.getInt(POSITION_SELECTED_MESSAGE, INVALID_POSITION);
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
        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_general_chat_messages, null);
        reactionsLayout = contentView.findViewById(R.id.reactions_layout);
        reactionsFragment = contentView.findViewById(R.id.fragment_container_reactions);
        items_layout = contentView.findViewById(R.id.items_layout);

        RelativeLayout optionForward = contentView.findViewById(R.id.forward_layout);

        LinearLayout editSeparator = contentView.findViewById(R.id.edit_separator);
        RelativeLayout optionEdit = contentView.findViewById(R.id.edit_layout);

        LinearLayout copySeparator = contentView.findViewById(R.id.copy_separator);
        RelativeLayout optionCopy = contentView.findViewById(R.id.copy_layout);

        LinearLayout selectSeparator = contentView.findViewById(R.id.select_separator);
        RelativeLayout optionSelect = contentView.findViewById(R.id.select_layout);

        LinearLayout infoSeparator = contentView.findViewById(R.id.info_separator);
        RelativeLayout viewOption = contentView.findViewById(R.id.option_view_layout);
        RelativeLayout infoOption = contentView.findViewById(R.id.option_info_layout);

        LinearLayout inviteSeparator = contentView.findViewById(R.id.invite_separator);
        RelativeLayout startConversationOption = contentView.findViewById(R.id.option_start_conversation_layout);
        RelativeLayout inviteOption = contentView.findViewById(R.id.option_invite_layout);

        LinearLayout deleteSeparator = contentView.findViewById(R.id.delete_separator);
        RelativeLayout optionDelete = contentView.findViewById(R.id.delete_layout);
        TextView textDelete = contentView.findViewById(R.id.delete_text);

        reactionsFragment.init(context, chatId, messageId, positionMessage);

        optionForward.setOnClickListener(this);
        optionEdit.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionSelect.setOnClickListener(this);
        viewOption.setOnClickListener(this);
        infoOption.setOnClickListener(this);
        startConversationOption.setOnClickListener(this);
        inviteOption.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        if (message == null || chatRoom == null || ((ChatActivityLollipop) context).hasMessagesRemoved(message.getMessage()) || message.isUploading()) {
            optionForward.setVisibility(View.GONE);
            editSeparator.setVisibility(View.GONE);
            optionEdit.setVisibility(View.GONE);
            copySeparator.setVisibility(View.GONE);
            optionCopy.setVisibility(View.GONE);
            selectSeparator.setVisibility(View.GONE);
            optionSelect.setVisibility(View.GONE);
            infoSeparator.setVisibility(View.GONE);
            viewOption.setVisibility(View.GONE);
            infoOption.setVisibility(View.GONE);
            inviteSeparator.setVisibility(View.GONE);
            startConversationOption.setVisibility(View.GONE);
            inviteOption.setVisibility(View.GONE);
            deleteSeparator.setVisibility(View.GONE);
            optionDelete.setVisibility(View.GONE);

        } else {
            int typeMessage = message.getMessage().getType();

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
            } else {

                optionForward.setVisibility((!isOnline(context) || chatC.isInAnonymousMode()) ? View.GONE : View.VISIBLE);

                if (message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle() || !message.getMessage().isEditable() || typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                    optionEdit.setVisibility(View.GONE);
                } else {
                    optionEdit.setVisibility((typeMessage == MegaChatMessage.TYPE_NORMAL || typeMessage == MegaChatMessage.TYPE_CONTAINS_META) ? View.VISIBLE : View.GONE);
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

            if (typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                long userCount = message.getMessage().getUsersCount();

                infoOption.setVisibility((message.getMessage().getUsersCount() == 1 &&
                        message.getMessage().getUserHandle(0) != megaChatApi.getMyUserHandle() &&
                        megaApi.getContact(message.getMessage().getUserEmail(0)) != null &&
                        megaApi.getContact(message.getMessage().getUserEmail(0)).getVisibility() == MegaUser.VISIBILITY_VISIBLE) ? View.VISIBLE : View.GONE);

                viewOption.setVisibility(message.getMessage().getUsersCount() > 1 ? View.VISIBLE : View.GONE);


                if(userCount == 1){
                    inviteOption.setVisibility(message.getMessage().getUserHandle(0) != megaChatApi.getMyUserHandle() &&
                            (megaApi.getContact(message.getMessage().getUserEmail(0)) == null ||
                                    megaApi.getContact(message.getMessage().getUserEmail(0)).getVisibility() != MegaUser.VISIBILITY_VISIBLE) ? View.VISIBLE : View.GONE);

                    startConversationOption.setVisibility(message.getMessage().getUserHandle(0) != megaChatApi.getMyUserHandle() &&
                            megaApi.getContact(message.getMessage().getUserEmail(0)) != null &&
                            megaApi.getContact(message.getMessage().getUserEmail(0)).getVisibility() == MegaUser.VISIBILITY_VISIBLE &&
                            (chatRoom.isGroup() || message.getMessage().getUserHandle(0) != chatRoom.getPeerHandle(0)) ? View.VISIBLE : View.GONE);

                }else{
                    startConversationOption.setVisibility(View.VISIBLE);
                    inviteOption.setVisibility(View.GONE);
                    boolean isContact = false;
                    for (int i = 0; i < userCount; i++) {
                        String userEmail = message.getMessage().getUserEmail(i);
                        MegaUser contact = megaApi.getContact(userEmail);
                        if (contact == null || contact.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
                            startConversationOption.setVisibility(View.GONE);
                            break;
                        }
                    }

                }
            }
        }

        reactionsLayout.setVisibility((shouldReactionOptionsBeVisible(context, chatRoom, message)) ? View.VISIBLE : View.GONE);
        editSeparator.setVisibility(optionForward.getVisibility() == View.VISIBLE && optionEdit.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        copySeparator.setVisibility(optionEdit.getVisibility() == View.VISIBLE && optionCopy.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        selectSeparator.setVisibility((optionSelect.getVisibility() == View.VISIBLE && (optionForward.getVisibility() == View.VISIBLE || optionCopy.getVisibility() == View.VISIBLE)) ? View.VISIBLE : View.GONE);
        infoSeparator.setVisibility((viewOption.getVisibility() == View.VISIBLE || infoOption.getVisibility() == View.VISIBLE) && optionSelect.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        inviteSeparator.setVisibility((startConversationOption.getVisibility() == View.VISIBLE || inviteOption.getVisibility() == View.VISIBLE) &&
                (viewOption.getVisibility() == View.VISIBLE || infoOption.getVisibility() == View.VISIBLE || selectSeparator.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);
        deleteSeparator.setVisibility(optionDelete.getVisibility());

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    private boolean shouldStartConversationOptionBeVisible(MegaUser contact) {
        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
        return contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE && (chatRoom.isGroup() || message.getMessage().getUserHandle(0) != chatRoom.getPeerHandle(0));
    }

    @Override
    public void onClick(View view) {
        if (message == null) {
            logWarning("The message is NULL");
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        Intent i;
        switch (view.getId()) {
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

            case R.id.delete_layout:
                ((ChatActivityLollipop) context).showConfirmationDeleteMessages(messagesSelected, chatRoom);
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putLong(POSITION_SELECTED_MESSAGE, positionMessage);
    }
}
