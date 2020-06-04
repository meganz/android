package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class ParticipantBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaChatRoom selectedChat;
    private long chatId = INVALID_HANDLE;
    private long participantHandle = INVALID_HANDLE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            participantHandle = savedInstanceState.getLong(CONTACT_HANDLE, INVALID_HANDLE);
        } else {
            chatId = ((GroupChatInfoActivityLollipop) context).getChatHandle();
            participantHandle = ((GroupChatInfoActivityLollipop) context).getSelectedHandleParticipant();
        }

        selectedChat = megaChatApi.getChatRoom(chatId);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        if (selectedChat == null || participantHandle == INVALID_HANDLE) {
            logWarning("Error. Selected chat is NULL or participant handle is -1");
            return;
        }

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_group_participant, null);
        mainLinearLayout = contentView.findViewById(R.id.participant_item_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        EmojiTextView titleNameContactChatPanel = contentView.findViewById(R.id.group_participants_chat_name_text);
        ImageView stateIcon = contentView.findViewById(R.id.group_participants_state_circle);

        stateIcon.setVisibility(View.VISIBLE);

        stateIcon.setMaxWidth(scaleWidthPx(6, outMetrics));
        stateIcon.setMaxHeight(scaleHeightPx(6, outMetrics));

        ImageView permissionsIcon = contentView.findViewById(R.id.group_participant_list_permissions);

        TextView titleMailContactChatPanel = contentView.findViewById(R.id.group_participants_chat_mail_text);
        RoundedImageView contactImageView = contentView.findViewById(R.id.sliding_group_participants_chat_list_thumbnail);

        LinearLayout optionContactInfoChat = contentView.findViewById(R.id.contact_info_group_participants_chat_layout);
        LinearLayout optionStartConversationChat = contentView.findViewById(R.id.start_chat_group_participants_chat_layout);
        LinearLayout optionEditProfileChat = contentView.findViewById(R.id.edit_profile_group_participants_chat_layout);
        LinearLayout optionLeaveChat = contentView.findViewById(R.id.leave_group_participants_chat_layout);
        LinearLayout optionChangePermissionsChat = contentView.findViewById(R.id.change_permissions_group_participants_chat_layout);
        LinearLayout optionRemoveParticipantChat = contentView.findViewById(R.id.remove_group_participants_chat_layout);
        LinearLayout optionInvite = contentView.findViewById(R.id.invite_group_participants_chat_layout);

        optionChangePermissionsChat.setOnClickListener(this);
        optionRemoveParticipantChat.setOnClickListener(this);
        optionContactInfoChat.setOnClickListener(this);
        optionStartConversationChat.setOnClickListener(this);
        optionEditProfileChat.setOnClickListener(this);
        optionLeaveChat.setOnClickListener(this);
        optionInvite.setOnClickListener(this);

        LinearLayout separatorInfo = contentView.findViewById(R.id.separator_info);
        LinearLayout separatorOptions = contentView.findViewById(R.id.separator_options);
        LinearLayout separatorLeave = contentView.findViewById(R.id.separator_leave);

        if (isScreenInPortrait(context)) {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT, outMetrics));
        } else {
            titleNameContactChatPanel.setMaxWidthEmojis(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
            titleMailContactChatPanel.setMaxWidth(px2dp(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND, outMetrics));
        }

        setContactStatus(getUserStatus(participantHandle), stateIcon);

        if (participantHandle == megaApi.getMyUser().getHandle()) {
            ChatController chatC = new ChatController(context);
            String myFullName = chatC.getMyFullName();
            if (isTextEmpty(myFullName)) {
                myFullName = megaChatApi.getMyEmail();
            }

            titleNameContactChatPanel.setText(myFullName);

            titleMailContactChatPanel.setText(megaChatApi.getMyEmail());

            int permission = selectedChat.getOwnPrivilege();

            if (permission == MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
            } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
            } else {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
            }

            optionEditProfileChat.setVisibility(View.VISIBLE);
            if (permission < MegaChatRoom.PRIV_RO) {
                optionLeaveChat.setVisibility(View.GONE);
            } else {
                optionLeaveChat.setVisibility(View.VISIBLE);
            }

            optionContactInfoChat.setVisibility(View.GONE);
            optionStartConversationChat.setVisibility(View.GONE);
            optionChangePermissionsChat.setVisibility(View.GONE);
            optionRemoveParticipantChat.setVisibility(View.GONE);

            optionInvite.setVisibility(View.GONE);

            setImageAvatar(megaApi.getMyUser(), megaChatApi.getMyEmail(), myFullName, contactImageView);
        } else {
            String fullName = getNicknameContact(participantHandle);
            if (fullName == null) {
                fullName = selectedChat.getPeerFullnameByHandle(participantHandle);
                if (isTextEmpty(fullName)) {
                    fullName = selectedChat.getPeerEmailByHandle(participantHandle);
                }
            }
            titleNameContactChatPanel.setText(fullName);
            titleMailContactChatPanel.setText(selectedChat.getPeerEmailByHandle(participantHandle));

            int permission = selectedChat.getPeerPrivilegeByHandle(participantHandle);

            if (permission == MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
            } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
            } else {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
            }

            MegaUser contact = megaApi.getContact(selectedChat.getPeerEmailByHandle(participantHandle));

            if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                optionContactInfoChat.setVisibility(View.VISIBLE);
                optionStartConversationChat.setVisibility(View.VISIBLE);
                optionInvite.setVisibility(View.GONE);
            } else {
                optionContactInfoChat.setVisibility(View.GONE);
                optionStartConversationChat.setVisibility(View.GONE);
                optionInvite.setVisibility(View.VISIBLE);
            }

            optionEditProfileChat.setVisibility(View.GONE);
            optionLeaveChat.setVisibility(View.GONE);

            if (selectedChat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR) {
                optionChangePermissionsChat.setVisibility(View.VISIBLE);
                optionRemoveParticipantChat.setVisibility(View.VISIBLE);
            } else {
                optionChangePermissionsChat.setVisibility(View.GONE);
                optionRemoveParticipantChat.setVisibility(View.GONE);
            }

            String email = selectedChat.getPeerEmailByHandle(participantHandle);
            setImageAvatar(!isTextEmpty(email) ? email : MegaApiAndroid.userHandleToBase64(participantHandle), fullName, contactImageView);
        }

        if ((optionContactInfoChat.getVisibility() == View.GONE && optionEditProfileChat.getVisibility() == View.GONE)
                || (optionChangePermissionsChat.getVisibility() == View.GONE && optionStartConversationChat.getVisibility() == View.GONE && optionInvite.getVisibility() == View.GONE
                && optionLeaveChat.getVisibility() == View.GONE && optionRemoveParticipantChat.getVisibility() == View.GONE)) {
            separatorInfo.setVisibility(View.GONE);
        } else {
            separatorInfo.setVisibility(View.VISIBLE);
        }
        if ((optionChangePermissionsChat.getVisibility() == View.GONE && optionStartConversationChat.getVisibility() == View.GONE && optionInvite.getVisibility() == View.GONE)
                || (optionLeaveChat.getVisibility() == View.GONE && optionRemoveParticipantChat.getVisibility() == View.GONE)) {
            separatorOptions.setVisibility(View.GONE);
        } else {
            separatorOptions.setVisibility(View.VISIBLE);
        }
        if (optionLeaveChat.getVisibility() == View.GONE || optionRemoveParticipantChat.getVisibility() == View.GONE) {
            separatorLeave.setVisibility(View.GONE);
        } else {
            separatorLeave.setVisibility(View.VISIBLE);
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.contact_info_group_participants_chat_layout:
                Intent i = new Intent(context, ContactInfoActivityLollipop.class);
                i.putExtra(NAME, selectedChat.getPeerEmailByHandle(participantHandle));
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;

            case R.id.start_chat_group_participants_chat_layout:
                ((GroupChatInfoActivityLollipop) context).startConversation(participantHandle);
                break;

            case R.id.change_permissions_group_participants_chat_layout:
                ((GroupChatInfoActivityLollipop) context).showChangePermissionsDialog(participantHandle, selectedChat);
                break;

            case R.id.remove_group_participants_chat_layout:
                ((GroupChatInfoActivityLollipop) context).showRemoveParticipantConfirmation(participantHandle, selectedChat);
                break;

            case R.id.edit_profile_group_participants_chat_layout:
                Intent editProfile = new Intent(context, ManagerActivityLollipop.class);
                editProfile.setAction(ACTION_SHOW_MY_ACCOUNT);
                startActivity(editProfile);
                dismissAllowingStateLoss();
                break;

            case R.id.leave_group_participants_chat_layout:
                ((GroupChatInfoActivityLollipop) context).showConfirmationLeaveChat();
                break;

            case R.id.invite_group_participants_chat_layout:
                ((GroupChatInfoActivityLollipop) context).inviteContact(selectedChat.getPeerEmailByHandle(participantHandle));
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(CONTACT_HANDLE, participantHandle);
    }
}
