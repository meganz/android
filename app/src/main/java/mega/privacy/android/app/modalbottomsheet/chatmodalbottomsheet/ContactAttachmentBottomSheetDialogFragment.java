package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class ContactAttachmentBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private AndroidMegaChatMessage message;
    private long chatId;
    private long messageId;
    private String email;
    private ChatController chatC;

    private int position;
    private int positionMessage;
    private MegaChatRoom chatRoom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_HANDLE);
            email = savedInstanceState.getString(EMAIL);
            positionMessage = savedInstanceState.getInt(POSITION_SELECTED_MESSAGE, INVALID_POSITION);

            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        } else {
            if (context instanceof ChatActivityLollipop) {
                chatId = ((ChatActivityLollipop) context).idChat;
                messageId = ((ChatActivityLollipop) context).selectedMessageId;
                positionMessage = ((ChatActivityLollipop) context).selectedPosition;
            } else {
                chatId = ((ContactAttachmentActivityLollipop) context).chatId;
                messageId = ((ContactAttachmentActivityLollipop) context).messageId;
                email = ((ContactAttachmentActivityLollipop) context).selectedEmail;
            }

            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        }

        logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
        chatRoom = megaChatApi.getChatRoom(chatId);
        chatC = new ChatController(context);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        if (message == null) {
            logError("Message is null");
            return;
        }

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_attachment_item, null);
        mainLinearLayout = contentView.findViewById(R.id.contact_attachment_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        EmojiTextView titleNameContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_name_text);
        ImageView stateIcon = contentView.findViewById(R.id.contact_attachment_state_circle);
        stateIcon.setMaxWidth(scaleWidthPx(6, outMetrics));
        stateIcon.setMaxHeight(scaleHeightPx(6, outMetrics));

        EmojiTextView titleMailContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_mail_text);
        RoundedImageView contactImageView = contentView.findViewById(R.id.contact_attachment_thumbnail);

        LinearLayout optionView = contentView.findViewById(R.id.option_view_layout);
        LinearLayout optionInfo = contentView.findViewById(R.id.option_info_layout);
        LinearLayout optionStartConversation = contentView.findViewById(R.id.option_start_conversation_layout);
        LinearLayout optionInvite = contentView.findViewById(R.id.option_invite_layout);
        LinearLayout optionForward = contentView.findViewById(R.id.forward_layout);
        LinearLayout optionSelect = contentView.findViewById(R.id.select_layout);
        LinearLayout optionDeleteMessage = contentView.findViewById(R.id.delete_layout);

        optionView.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionStartConversation.setOnClickListener(this);
        optionInvite.setOnClickListener(this);
        optionSelect.setOnClickListener(this);
        optionForward.setOnClickListener(this);
        optionDeleteMessage.setOnClickListener(this);

        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);
        LinearLayout viewSeparator = contentView.findViewById(R.id.view_separator);
        LinearLayout selectSeparator = contentView.findViewById(R.id.select_separator);
        LinearLayout deleteMessageSeparator = contentView.findViewById(R.id.delete_separator);

        if (isScreenInPortrait(context)) {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        } else {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        if (context instanceof ChatActivityLollipop && chatRoom != null) {
            optionSelect.setVisibility(View.VISIBLE);
            if (chatC.isInAnonymousMode() || ((chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO) && !chatRoom.isPreview())) {
                optionForward.setVisibility(View.GONE);
                optionDeleteMessage.setVisibility(View.GONE);
            } else {
                if (!isOnline(context)) {
                    optionForward.setVisibility(View.GONE);
                } else {
                    optionForward.setVisibility(View.VISIBLE);
                }
                if (message.getMessage().getUserHandle() != megaChatApi.getMyUserHandle() || !message.getMessage().isDeletable()) {
                    optionDeleteMessage.setVisibility(View.GONE);
                } else {
                    optionDeleteMessage.setVisibility(View.VISIBLE);
                }
            }
        } else {
            optionSelect.setVisibility(View.GONE);
            optionForward.setVisibility(View.GONE);
            optionDeleteMessage.setVisibility(View.GONE);
        }

        long userCount = message.getMessage().getUsersCount();
        if (userCount == 1) {
            logDebug("One contact attached");
            optionView.setVisibility(View.GONE);
            optionInfo.setVisibility(View.VISIBLE);

            long userHandle = message.getMessage().getUserHandle(0);
            setContactStatus(megaChatApi.getUserOnlineStatus(userHandle), stateIcon);

            if (userHandle != megaChatApi.getMyUserHandle()) {

                String userName = message.getMessage().getUserName(0);
                titleNameContactChatPanel.setText(userName);

                String userEmail = message.getMessage().getUserEmail(0);
                titleMailContactChatPanel.setText(userEmail);
                MegaUser contact = megaApi.getContact(userEmail);

                if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    optionInfo.setVisibility(View.VISIBLE);

                    //Check if the contact is the same that the one is chatting
                    MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                    if (!chatRoom.isGroup()) {
                        long contactHandle = message.getMessage().getUserHandle(0);
                        long messageContactHandle = chatRoom.getPeerHandle(0);
                        if (contactHandle == messageContactHandle) {
                            optionStartConversation.setVisibility(View.GONE);
                        } else {
                            optionStartConversation.setVisibility(View.VISIBLE);
                        }
                    } else {
                        optionStartConversation.setVisibility(View.VISIBLE);
                    }
                    optionInvite.setVisibility(View.GONE);
                } else {
                    optionInfo.setVisibility(View.GONE);
                    optionStartConversation.setVisibility(View.GONE);
                    optionInvite.setVisibility(View.VISIBLE);
                }

                setImageAvatar(userEmail, userName, contactImageView);
            }
        } else {
            MegaUser contact;
            if (email == null) {
                optionView.setVisibility(View.VISIBLE);
                optionInfo.setVisibility(View.GONE);

                stateIcon.setVisibility(View.GONE);

                StringBuilder name = new StringBuilder();
                name.append(message.getMessage().getUserName(0));
                for (int i = 1; i < userCount; i++) {
                    name.append(", ").append(message.getMessage().getUserName(i));
                }

                optionInvite.setVisibility(View.GONE);

                for (int i = 1; i < userCount; i++) {
                    String userEmail = message.getMessage().getUserEmail(i);
                    contact = megaApi.getContact(userEmail);

                    if (contact != null && contact.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
                        optionStartConversation.setVisibility(View.GONE);
                        optionInvite.setVisibility(View.VISIBLE);
                        break;
                    }
                }

                titleMailContactChatPanel.setText(name);

                String email = context.getResources().getQuantityString(R.plurals.general_selection_num_contacts, (int) userCount, userCount);
                titleNameContactChatPanel.setText(email);

                setImageAvatar(null, userCount + "", contactImageView);
            } else {
                optionView.setVisibility(View.GONE);

                stateIcon.setVisibility(View.VISIBLE);

                position = getPositionByMail(email);
                if (position == -1) {
                    logWarning("Error - position -1");
                    return;
                }

                optionStartConversation.setVisibility(View.VISIBLE);
                optionInvite.setVisibility(View.GONE);

                String email = message.getMessage().getUserEmail(position);
                titleMailContactChatPanel.setText(email);

                long userHandle = message.getMessage().getUserHandle(position);
                String name = message.getMessage().getUserName(position);
                if (isTextEmpty(name)) {
                    name = chatC.getFullName(userHandle, chatId);
                    if (name.trim().isEmpty()) {
                        name = email;
                    }
                }

                titleNameContactChatPanel.setText(name);

                contact = megaApi.getContact(email);

                if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                    optionInfo.setVisibility(View.VISIBLE);
                    //Check if the contact is the same that the one is chatting
                    MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                    if (!chatRoom.isGroup() && chatRoom.getPeerHandle(0) == contact.getHandle()) {
                        optionStartConversation.setVisibility(View.GONE);
                    } else {
                        optionStartConversation.setVisibility(View.VISIBLE);
                    }

                    optionInvite.setVisibility(View.GONE);
                } else {
                    optionInfo.setVisibility(View.GONE);
                    optionStartConversation.setVisibility(View.GONE);
                    optionInvite.setVisibility(View.VISIBLE);
                }

                setImageAvatar(email, name, contactImageView);
            }
        }

        selectSeparator.setVisibility((optionSelect.getVisibility() == View.VISIBLE && optionForward.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);

        viewSeparator.setVisibility(((optionSelect.getVisibility() == View.GONE &&
                optionForward.getVisibility() == View.GONE) ||
                (optionView.getVisibility() == View.GONE &&
                        optionInfo.getVisibility() == View.GONE)) ? View.GONE : View.VISIBLE);

        separatorInfo.setVisibility((optionView.getVisibility() == View.GONE &&
                optionInfo.getVisibility() == View.GONE) ||
                (optionStartConversation.getVisibility() == View.GONE &&
                        optionInvite.getVisibility() == View.GONE) ? View.GONE : View.VISIBLE);

        deleteMessageSeparator.setVisibility(optionDeleteMessage.getVisibility());

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    @Override
    public void onClick(View v) {
        if (message == null) {
            logWarning("Error. The message is NULL");
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        Intent i;
        long numUsers = message.getMessage().getUsersCount();

        switch (v.getId()) {
            case R.id.option_info_layout:
                if (!isOnline(context)) {
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                i = new Intent(context, ContactInfoActivityLollipop.class);
                if (context instanceof ChatActivityLollipop) {
                    i.putExtra(NAME, message.getMessage().getUserEmail(0));
                } else if (position != -1) {
                    i.putExtra(NAME, message.getMessage().getUserEmail(position));
                } else {
                    logWarning("Error - position -1");
                }

                context.startActivity(i);
                break;

            case R.id.option_view_layout:
                logDebug("View option");
                i = new Intent(context, ContactAttachmentActivityLollipop.class);
                i.putExtra("chatId", chatId);
                i.putExtra(MESSAGE_ID, messageId);
                context.startActivity(i);
                break;

            case R.id.select_layout:
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).activateActionModeWithItem(positionMessage);
                }
                break;

            case R.id.forward_layout:
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).forwardMessages(messagesSelected);
                }
                break;

            case R.id.delete_layout:
                if (context instanceof ChatActivityLollipop) {
                    ((ChatActivityLollipop) context).showConfirmationDeleteMessages(messagesSelected, chatRoom);
                }
                break;

            case R.id.option_invite_layout:
                if (!isOnline(context)) {
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                ContactController cC = new ContactController(context);
                ArrayList<String> contactEmails;

                if (context instanceof ChatActivityLollipop) {
                    if (numUsers == 1) {
                        cC.inviteContact(message.getMessage().getUserEmail(0));
                    } else {
                        logDebug("Num users to invite: " + numUsers);
                        contactEmails = new ArrayList<>();

                        for (int j = 0; j < numUsers; j++) {
                            String userMail = message.getMessage().getUserEmail(j);
                            contactEmails.add(userMail);
                        }
                        cC.inviteMultipleContacts(contactEmails);
                    }
                } else if (email != null) {
                    cC.inviteContact(email);
                }
                break;

            case R.id.option_start_conversation_layout:
                if (context instanceof ChatActivityLollipop) {
                    if (numUsers == 1) {
                        ((ChatActivityLollipop) context).startConversation(message.getMessage().getUserHandle(0));
                        dismissAllowingStateLoss();
                    } else {
                        logDebug("Num users to invite: " + numUsers);
                        ArrayList<Long> contactHandles = new ArrayList<>();

                        for (int j = 0; j < numUsers; j++) {
                            long userHandle = message.getMessage().getUserHandle(j);
                            contactHandles.add(userHandle);
                        }
                        ((ChatActivityLollipop) context).startGroupConversation(contactHandles);
                    }
                } else {
                    logDebug("Instance of ContactAttachmentActivityLollipop");
                    logDebug("position: " + position);
                    long userHandle = message.getMessage().getUserHandle(position);
                    ((ContactAttachmentActivityLollipop) context).startConversation(userHandle);
                }
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    private int getPositionByMail(String email) {
        long userCount = message.getMessage().getUsersCount();
        for (int i = 0; i < userCount; i++) {
            if (message.getMessage().getUserEmail(i).equals(email)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
        outState.putString(EMAIL, email);
        if (context instanceof ChatActivityLollipop) {
            outState.putLong(POSITION_SELECTED_MESSAGE, positionMessage);
        }
    }
}
