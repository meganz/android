package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import static mega.privacy.android.app.utils.AvatarUtil.setImageAvatar;
import static mega.privacy.android.app.utils.CallUtil.canCallBeStartedFromContactOption;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.getUserStatus;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.CONTACT_HANDLE;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import mega.privacy.android.app.myAccount.MyAccountActivity;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity;
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel;
import mega.privacy.android.app.utils.ContactUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@AndroidEntryPoint
public class ParticipantBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    @Inject
    PasscodeManagement passcodeManagement;

    private ScheduledMeetingInfoViewModel viewModel = null;

    private MegaChatRoom selectedChat;
    private long chatId = INVALID_HANDLE;
    private long participantHandle = INVALID_HANDLE;
    private ChatController chatC;

    private EmojiTextView titleNameContactChatPanel;
    private RoundedImageView contactImageView;

    public static ParticipantBottomSheetDialogFragment newInstance(long chatId, long participantHandle) {
        ParticipantBottomSheetDialogFragment fragment = new ParticipantBottomSheetDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(CHAT_ID, chatId);
        arguments.putLong(CONTACT_HANDLE, participantHandle);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_group_participant, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);
        titleNameContactChatPanel = contentView.findViewById(R.id.group_participants_chat_name_text);

        Bundle arguments = getArguments();
        if (arguments != null) {
            chatId = arguments.getLong(CHAT_ID, INVALID_HANDLE);
            participantHandle = arguments.getLong(CONTACT_HANDLE, INVALID_HANDLE);
        } else if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            participantHandle = savedInstanceState.getLong(CONTACT_HANDLE, INVALID_HANDLE);
        } else {
            chatId = ((GroupChatInfoActivity) requireActivity()).getChatHandle();
            participantHandle = ((GroupChatInfoActivity) requireActivity()).getSelectedHandleParticipant();
        }

        if (requireActivity() instanceof ScheduledMeetingInfoActivity) {
            viewModel = new ViewModelProvider(requireActivity()).get(ScheduledMeetingInfoViewModel.class);
        }

        selectedChat = megaChatApi.getChatRoom(chatId);

        chatC = new ChatController(requireActivity());

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (selectedChat == null || participantHandle == INVALID_HANDLE) {
            Timber.w("Error. Selected chat is NULL or participant handle is -1");
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
        TextView optionLeaveChatText = contentView.findViewById(R.id.leave_group_participants_chat_text);
        if (megaChatApi.getChatRoom(chatId) != null && megaChatApi.getChatRoom(chatId).isMeeting()) {
            optionLeaveChatText.setText(R.string.meetings_info_leave_option);
        } else {
            optionLeaveChatText.setText(R.string.title_properties_chat_leave_chat);
        }
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

        int userStatus = participantHandle == megaChatApi.getMyUserHandle() ? megaChatApi.getOnlineStatus() : getUserStatus(participantHandle);
        setContactStatus(userStatus, stateIcon, StatusIconLocation.DRAWER);

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
        int id = v.getId();
        if (id == R.id.contact_info_group_participants_chat_layout) {
            ContactUtil.openContactInfoActivity(requireActivity(), chatC.getParticipantEmail(participantHandle));
        } else if (id == R.id.start_chat_group_participants_chat_layout) {
            if (requireActivity() instanceof ScheduledMeetingInfoActivity) {
                if (viewModel != null) {
                    viewModel.onSendMsgTap();
                }
            } else if (requireActivity() instanceof GroupChatInfoActivity) {
                ((GroupChatInfoActivity) requireActivity()).startConversation(participantHandle);
            }
        } else if (id == R.id.contact_list_option_call_layout) {
            MegaApplication.setUserWaitingForCall(participantHandle);
            if (canCallBeStartedFromContactOption(requireActivity(), passcodeManagement)) {
                if (requireActivity() instanceof ScheduledMeetingInfoActivity) {
                    if (viewModel != null) {
                        viewModel.onStartCallTap();
                    }
                } else if (requireActivity() instanceof GroupChatInfoActivity) {
                    ((GroupChatInfoActivity) requireActivity()).startCall();
                }
            }
        } else if (id == R.id.change_permissions_group_participants_chat_layout) {
            if (requireActivity() instanceof ScheduledMeetingInfoActivity) {
                if (viewModel != null) {
                    viewModel.onChangePermissionsTap();
                }
            } else if (requireActivity() instanceof GroupChatInfoActivity) {
                ((GroupChatInfoActivity) requireActivity()).showChangePermissionsDialog(participantHandle, selectedChat);
            }
        } else if (id == R.id.remove_group_participants_chat_layout) {
            if (requireActivity() instanceof ScheduledMeetingInfoActivity) {
                if (viewModel != null) {
                    viewModel.onRemoveParticipantTap(true);
                }
            } else if (requireActivity() instanceof GroupChatInfoActivity) {
                ((GroupChatInfoActivity) requireActivity()).showRemoveParticipantConfirmation(participantHandle);
            }
        } else if (id == R.id.edit_profile_group_participants_chat_layout) {
            Intent editProfile = new Intent(requireActivity(), MyAccountActivity.class);
            startActivity(editProfile);
        } else if (id == R.id.leave_group_participants_chat_layout) {
            if (requireActivity() instanceof ScheduledMeetingInfoActivity) {
                if (viewModel != null) {
                    viewModel.onLeaveGroupTap();
                }
            } else if (requireActivity() instanceof GroupChatInfoActivity) {
                ((GroupChatInfoActivity) requireActivity()).showConfirmationLeaveChat();
            }
        } else if (id == R.id.invite_group_participants_chat_layout) {
            if (requireActivity() instanceof ScheduledMeetingInfoActivity) {
                if (viewModel != null) {
                    viewModel.onInviteContactTap();
                }
            } else if (requireActivity() instanceof GroupChatInfoActivity) {
                ((GroupChatInfoActivity) requireActivity()).inviteContact(chatC.getParticipantEmail(participantHandle));
            }
        }

        dismissAllowingStateLoss();
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
