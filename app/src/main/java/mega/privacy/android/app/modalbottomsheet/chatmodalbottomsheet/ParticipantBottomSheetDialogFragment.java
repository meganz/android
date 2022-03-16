package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.lollipop.ManagerActivity;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.utils.ContactUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.inject.Inject;

@AndroidEntryPoint
public class ParticipantBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Inject
    PasscodeManagement passcodeManagement;

    private MegaChatRoom selectedChat;
    private long chatId = INVALID_HANDLE;
    private long participantHandle = INVALID_HANDLE;
    private ChatController chatC;

    private EmojiTextView titleNameContactChatPanel;
    private RoundedImageView contactImageView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_group_participant, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);
        titleNameContactChatPanel = contentView.findViewById(R.id.group_participants_chat_name_text);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            participantHandle = savedInstanceState.getLong(CONTACT_HANDLE, INVALID_HANDLE);
        } else {
            chatId = ((GroupChatInfoActivity) requireActivity()).getChatHandle();
            participantHandle = ((GroupChatInfoActivity) requireActivity()).getSelectedHandleParticipant();
        }

        selectedChat = megaChatApi.getChatRoom(chatId);

        chatC = new ChatController(requireActivity());

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (selectedChat == null || participantHandle == INVALID_HANDLE) {
            logWarning("Error. Selected chat is NULL or participant handle is -1");
            return;
        }

        ImageView stateIcon = contentView.findViewById(R.id.group_participants_state_circle);

        stateIcon.setVisibility(View.VISIBLE);

        stateIcon.setMaxWidth(scaleWidthPx(6, getResources().getDisplayMetrics()));
        stateIcon.setMaxHeight(scaleHeightPx(6, getResources().getDisplayMetrics()));

        ImageView permissionsIcon = contentView.findViewById(R.id.group_participant_list_permissions);

        TextView titleMailContactChatPanel = contentView.findViewById(R.id.group_participants_chat_mail_text);
        contactImageView = contentView.findViewById(R.id.sliding_group_participants_chat_list_thumbnail);

        LinearLayout optionContactInfoChat = contentView.findViewById(R.id.contact_info_group_participants_chat_layout);
        LinearLayout optionEditProfileChat = contentView.findViewById(R.id.edit_profile_group_participants_chat_layout);

        LinearLayout optionStartConversationChat = contentView.findViewById(R.id.start_chat_group_participants_chat_layout);
        LinearLayout optionStartCall = contentView.findViewById(R.id.contact_list_option_call_layout);
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
        optionStartCall.setOnClickListener(this);
        optionStartCall.setVisibility(View.GONE);
        View separatorInfo = contentView.findViewById(R.id.separator_info);
        View separatorChat = contentView.findViewById(R.id.separator_chat);
        View separatorOptions = contentView.findViewById(R.id.separator_options);
        View separatorLeave = contentView.findViewById(R.id.separator_leave);

        if (isScreenInPortrait(requireContext())) {
            titleNameContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT));
            titleMailContactChatPanel.setMaxWidth(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT));
        } else {
            titleNameContactChatPanel.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND));
            titleMailContactChatPanel.setMaxWidth(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND));
        }

        setContactStatus(getUserStatus(participantHandle), stateIcon, StatusIconLocation.DRAWER);

        if (participantHandle == megaApi.getMyUser().getHandle()) {
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

            setImageAvatar(megaApi.getMyUser().getHandle(), megaChatApi.getMyEmail(), myFullName, contactImageView);
        } else {
            String fullName = chatC.getParticipantFullName(participantHandle);
            titleNameContactChatPanel.setText(fullName);
            String email = chatC.getParticipantEmail(participantHandle);

            int permission = selectedChat.getPeerPrivilegeByHandle(participantHandle);

            if (permission == MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write);
            } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access);
            } else {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only);
            }

            MegaUser contact = megaApi.getContact(email);

            if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                optionContactInfoChat.setVisibility(View.VISIBLE);
                optionStartConversationChat.setVisibility(View.VISIBLE);
                optionStartCall.setVisibility(View.VISIBLE);
                optionInvite.setVisibility(View.GONE);

                titleMailContactChatPanel.setText(email);
            } else {
                optionContactInfoChat.setVisibility(View.GONE);
                optionStartConversationChat.setVisibility(View.GONE);
                optionInvite.setVisibility(chatC.getParticipantEmail(participantHandle) == null ? View.GONE : View.VISIBLE);
                titleMailContactChatPanel.setVisibility(View.GONE);
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

            setImageAvatar(participantHandle, isTextEmpty(email) ? MegaApiAndroid.userHandleToBase64(participantHandle) : email, fullName, contactImageView);
        }

        separatorInfo.setVisibility((optionContactInfoChat.getVisibility() == View.VISIBLE ||
                optionEditProfileChat.getVisibility() == View.VISIBLE) &&
                (optionStartCall.getVisibility() == View.VISIBLE ||
                        optionStartConversationChat.getVisibility() == View.VISIBLE)
                ? View.VISIBLE : View.GONE);

        separatorChat.setVisibility((optionStartCall.getVisibility() == View.VISIBLE ||
                optionStartConversationChat.getVisibility() == View.VISIBLE) &&
                (optionChangePermissionsChat.getVisibility() == View.VISIBLE ||
                        optionInvite.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);

        separatorOptions.setVisibility((optionChangePermissionsChat.getVisibility() == View.VISIBLE ||
                optionInvite.getVisibility() == View.VISIBLE) && optionLeaveChat.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        separatorLeave.setVisibility(optionLeaveChat.getVisibility() == View.VISIBLE &&
                optionRemoveParticipantChat.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.contact_info_group_participants_chat_layout:
                ContactUtil.openContactInfoActivity(requireActivity(), chatC.getParticipantEmail(participantHandle));
                dismissAllowingStateLoss();
                break;

            case R.id.start_chat_group_participants_chat_layout:
                ((GroupChatInfoActivity) requireActivity()).startConversation(participantHandle);
                break;

            case R.id.contact_list_option_call_layout:
                if (canCallBeStartedFromContactOption((GroupChatInfoActivity) requireActivity(), passcodeManagement)) {
                    startNewCall((GroupChatInfoActivity) requireActivity(),
                            (SnackbarShower) requireActivity(),
                            megaApi.getContact(chatC.getParticipantEmail(participantHandle)), passcodeManagement);
                }
                break;

            case R.id.change_permissions_group_participants_chat_layout:
                ((GroupChatInfoActivity) requireActivity()).showChangePermissionsDialog(participantHandle, selectedChat);
                break;

            case R.id.remove_group_participants_chat_layout:
                ((GroupChatInfoActivity) requireActivity()).showRemoveParticipantConfirmation(participantHandle);
                break;

            case R.id.edit_profile_group_participants_chat_layout:
                Intent editProfile = new Intent(requireActivity(), ManagerActivity.class);
                editProfile.setAction(ACTION_SHOW_MY_ACCOUNT);
                startActivity(editProfile);
                dismissAllowingStateLoss();
                break;

            case R.id.leave_group_participants_chat_layout:
                ((GroupChatInfoActivity) requireActivity()).showConfirmationLeaveChat();
                break;

            case R.id.invite_group_participants_chat_layout:
                ((GroupChatInfoActivity) requireActivity()).inviteContact(chatC.getParticipantEmail(participantHandle));
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

    public void updateContactData() {
        if (participantHandle == megaApi.getMyUser().getHandle()) {
            String myFullName = chatC.getMyFullName();
            if (isTextEmpty(myFullName)) {
                myFullName = megaChatApi.getMyEmail();
            }

            titleNameContactChatPanel.setText(myFullName);
            setImageAvatar(megaApi.getMyUser().getHandle(), megaChatApi.getMyEmail(), myFullName, contactImageView);
        } else {
            String fullName = chatC.getParticipantFullName(participantHandle);
            titleNameContactChatPanel.setText(fullName);
            String email = chatC.getParticipantEmail(participantHandle);

            setImageAvatar(participantHandle, isTextEmpty(email) ? MegaApiAndroid.userHandleToBase64(participantHandle) : email, fullName, contactImageView);
        }
    }
}
