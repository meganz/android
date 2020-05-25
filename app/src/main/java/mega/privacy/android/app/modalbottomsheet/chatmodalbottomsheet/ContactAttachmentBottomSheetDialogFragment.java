package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
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

    private LinearLayout titleContact;
    private LinearLayout separatorTitleContact;
    public EmojiTextView titleNameContactChatPanel;
    public ImageView stateIcon;
    public EmojiTextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_HANDLE);
            email = savedInstanceState.getString(EMAIL);

            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        } else {

            chatId = ((ContactAttachmentActivityLollipop) context).chatId;
            messageId = ((ContactAttachmentActivityLollipop) context).messageId;
            email = ((ContactAttachmentActivityLollipop) context).selectedEmail;
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
        chatC = new ChatController(context);
        dbH = DatabaseHandler.getDbHandler(getActivity());
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
        titleContact = contentView.findViewById(R.id.contact_attachment_chat_title_layout);
        separatorTitleContact = contentView.findViewById(R.id.contact_title_separator);
        items_layout = contentView.findViewById(R.id.items_layout);

        titleNameContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_name_text);
        stateIcon = contentView.findViewById(R.id.contact_attachment_state_circle);
        stateIcon.setMaxWidth(scaleWidthPx(6, outMetrics));
        stateIcon.setMaxHeight(scaleHeightPx(6, outMetrics));

        titleMailContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_mail_text);
        contactImageView = contentView.findViewById(R.id.contact_attachment_thumbnail);

        LinearLayout optionView = contentView.findViewById(R.id.option_view_layout);
        LinearLayout optionInfo = contentView.findViewById(R.id.option_info_layout);
        LinearLayout optionStartConversation = contentView.findViewById(R.id.option_start_conversation_layout);
        LinearLayout optionInvite = contentView.findViewById(R.id.option_invite_layout);

        optionView.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionStartConversation.setOnClickListener(this);
        optionInvite.setOnClickListener(this);

        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);

        if (isScreenInPortrait(context)) {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        } else {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        titleContact.setVisibility(View.VISIBLE);
        separatorTitleContact.setVisibility(View.VISIBLE);

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

        separatorInfo.setVisibility((optionView.getVisibility() == View.VISIBLE ||
                optionInfo.getVisibility() == View.VISIBLE) && (optionStartConversation.getVisibility() == View.VISIBLE ||
                optionInvite.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);


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
    }
}
