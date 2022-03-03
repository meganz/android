package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivity;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.app.utils.StringResourcesUtils;
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
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ContactAttachmentBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private AndroidMegaChatMessage message;
    private long chatId;
    private long messageId;
    private String email;
    private ChatController chatC;
    private int position;

    public EmojiTextView titleNameContactChatPanel;
    public ImageView stateIcon;
    public EmojiTextView titleMailContactChatPanel;
    public RoundedImageView contactImageView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_attachment_item, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_HANDLE);
            email = savedInstanceState.getString(EMAIL);

            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        } else {
            chatId = ((ContactAttachmentActivity) requireActivity()).chatId;
            messageId = ((ContactAttachmentActivity) requireActivity()).messageId;
            email = ((ContactAttachmentActivity) requireActivity()).selectedEmail;
        }

        MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
        if (messageMega != null) {
            message = new AndroidMegaChatMessage(messageMega);
        }

        logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
        chatC = new ChatController(requireActivity());

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (message == null) {
            logError("Message is null");
            return;
        }

        LinearLayout titleContact = contentView.findViewById(R.id.contact_attachment_chat_title_layout);
        View separatorTitleContact = contentView.findViewById(R.id.contact_title_separator);

        titleNameContactChatPanel = contentView.findViewById(R.id.contact_attachment_chat_name_text);
        stateIcon = contentView.findViewById(R.id.contact_attachment_state_circle);
        stateIcon.setMaxWidth(scaleWidthPx(6, getResources().getDisplayMetrics()));
        stateIcon.setMaxHeight(scaleHeightPx(6, getResources().getDisplayMetrics()));

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

        View separatorInfo = contentView.findViewById(R.id.separator_info);

        if (isScreenInPortrait(requireContext())) {
            titleNameContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT));
            titleMailContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT));
        } else {
            titleNameContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND));
            titleMailContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND));
        }

        titleContact.setVisibility(View.VISIBLE);
        separatorTitleContact.setVisibility(View.VISIBLE);

        long userCount = message.getMessage().getUsersCount();
        if (userCount == 1) {
            logDebug("One contact attached");
            optionView.setVisibility(View.GONE);
            optionInfo.setVisibility(View.VISIBLE);

            long userHandle = message.getMessage().getUserHandle(0);
            setContactStatus(getUserStatus(userHandle), stateIcon, StatusIconLocation.DRAWER);

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
                    userHandle = contact.getHandle();
                } else {
                    optionInfo.setVisibility(View.GONE);
                    optionStartConversation.setVisibility(View.GONE);
                    optionInvite.setVisibility(View.VISIBLE);
                }
                setImageAvatar(userHandle, userEmail, userName, contactImageView);
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

                String email = StringResourcesUtils.getQuantityString(R.plurals.general_selection_num_contacts, (int) userCount, userCount);
                titleNameContactChatPanel.setText(email);

                setImageAvatar(INVALID_HANDLE,null, userCount + "", contactImageView);
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
                    name = chatC.getParticipantFullName(userHandle);
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

                setImageAvatar(userHandle, email, name, contactImageView);
            }
        }

        separatorInfo.setVisibility((optionView.getVisibility() == View.VISIBLE ||
                optionInfo.getVisibility() == View.VISIBLE) && (optionStartConversation.getVisibility() == View.VISIBLE ||
                optionInvite.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if (message == null) {
            logWarning("Error. The message is NULL");
            return;
        }

        ArrayList<AndroidMegaChatMessage> messagesSelected = new ArrayList<>();
        messagesSelected.add(message);
        long numUsers = message.getMessage().getUsersCount();

        switch (v.getId()) {
            case R.id.option_info_layout:
                if (!isOnline(requireContext())) {
                    ((ChatActivity) requireActivity()).showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                long contactHandle = MEGACHAT_INVALID_HANDLE;
                String contactEmail = null;
                if (requireActivity() instanceof ChatActivity) {
                    contactEmail = message.getMessage().getUserEmail(0);
                    contactHandle = message.getMessage().getUserHandle(0);
                } else if (position != -1) {
                    contactEmail = message.getMessage().getUserEmail(position);
                    contactHandle = message.getMessage().getUserHandle(position);
                } else {
                    logWarning("Error - position -1");
                }

                if (contactHandle != MEGACHAT_INVALID_HANDLE) {
                    MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatId);
                    boolean isChatRoomOpen = chatRoom != null && !chatRoom.isGroup() && contactHandle == chatRoom.getPeerHandle(0);
                    ContactUtil.openContactInfoActivity(requireActivity(), contactEmail, isChatRoomOpen);
                }
                break;

            case R.id.option_view_layout:
                logDebug("View option");
                ContactUtil.openContactAttachmentActivity(requireActivity(), chatId, messageId);
                break;

            case R.id.option_invite_layout:
                if (!isOnline(requireContext())) {
                    ((ChatActivity) requireActivity()).showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.error_server_connection_problem), INVALID_HANDLE);
                    return;
                }

                ContactController cC = new ContactController(requireActivity());
                ArrayList<String> contactEmails;

                if (requireActivity() instanceof ChatActivity) {
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
                if (requireActivity() instanceof ChatActivity) {
                    if (numUsers == 1) {
                        ((ChatActivity) requireActivity()).startConversation(message.getMessage().getUserHandle(0));
                        dismissAllowingStateLoss();
                    } else {
                        logDebug("Num users to invite: " + numUsers);
                        ArrayList<Long> contactHandles = new ArrayList<>();

                        for (int j = 0; j < numUsers; j++) {
                            long userHandle = message.getMessage().getUserHandle(j);
                            contactHandles.add(userHandle);
                        }
                        ((ChatActivity) requireActivity()).startGroupConversation(contactHandles);
                    }
                } else {
                    logDebug("Instance of ContactAttachmentActivity");
                    logDebug("position: " + position);
                    long userHandle = message.getMessage().getUserHandle(position);
                    ((ContactAttachmentActivity) requireActivity()).startConversation(userHandle);
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
