package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication.Companion.userWaitingForCall
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.meeting.ChatInfoViewModel
import mega.privacy.android.app.utils.AvatarUtil.setImageAvatar
import mega.privacy.android.app.utils.CallUtil.canCallBeStartedFromContactOption
import mega.privacy.android.app.utils.ChatUtil.StatusIconLocation
import mega.privacy.android.app.utils.ChatUtil.getUserStatus
import mega.privacy.android.app.utils.ChatUtil.setContactStatus
import mega.privacy.android.app.utils.Constants.CONTACT_HANDLE
import mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND
import mega.privacy.android.app.utils.Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT
import mega.privacy.android.app.utils.Constants.NAME
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.isScreenInPortrait
import mega.privacy.android.app.utils.Util.scaleHeightPx
import mega.privacy.android.app.utils.Util.scaleWidthPx
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.thirdpartylib.twemoji.EmojiTextView
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ScheduledMeetingParticipantBottomSheetDialogFragment : BaseBottomSheetDialogFragment(),
    View.OnClickListener {

    private val viewModel by activityViewModels<ChatInfoViewModel>()

    @Inject
    lateinit var chatController: ChatController

    private var selectedChat: MegaChatRoom? = null
    private var chatId = MegaApiJava.INVALID_HANDLE
    private var participantHandle = MegaApiJava.INVALID_HANDLE

    private var titleNameContactChatPanel: EmojiTextView? = null
    private var contactImageView: RoundedImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_group_participant, null)
        itemsLayout = contentView.findViewById(R.id.items_layout)
        titleNameContactChatPanel =
            contentView.findViewById(R.id.group_participants_chat_name_text)

        val arguments = getArguments()
        if (arguments != null) {
            chatId =
                arguments.getLong(ChatNavKey.LEGACY_CHAT_ID, MegaApiJava.INVALID_HANDLE)
            participantHandle = arguments.getLong(CONTACT_HANDLE, MegaApiJava.INVALID_HANDLE)
        } else if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(
                ChatNavKey.LEGACY_CHAT_ID,
                MegaApiJava.INVALID_HANDLE
            )
            participantHandle =
                savedInstanceState.getLong(CONTACT_HANDLE, MegaApiJava.INVALID_HANDLE)
        }

        selectedChat = megaChatApi.getChatRoom(chatId)


        return contentView
    }

    public override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (selectedChat == null || participantHandle == MegaApiJava.INVALID_HANDLE) {
            Timber.w("Error. Selected chat is NULL or participant handle is -1")
            return
        }

        val stateIcon = contentView.findViewById<ImageView>(R.id.group_participants_state_circle)

        stateIcon.setVisibility(View.VISIBLE)

        stateIcon.setMaxWidth(scaleWidthPx(6, getResources().getDisplayMetrics()))
        stateIcon.setMaxHeight(scaleHeightPx(6, getResources().getDisplayMetrics()))

        val permissionsIcon =
            contentView.findViewById<ImageView>(R.id.group_participant_list_permissions)

        val titleMailContactChatPanel =
            contentView.findViewById<TextView>(R.id.group_participants_chat_mail_text)
        contactImageView =
            contentView.findViewById<RoundedImageView?>(R.id.sliding_group_participants_chat_list_thumbnail)

        val optionContactInfoChat =
            contentView.findViewById<TextView>(R.id.contact_info_group_participants_chat)
        val optionEditProfileChat =
            contentView.findViewById<TextView>(R.id.edit_profile_group_participants_chat)

        val optionStartConversationChat =
            contentView.findViewById<TextView>(R.id.start_chat_group_participants_chat)
        val optionStartCall =
            contentView.findViewById<TextView>(R.id.contact_list_option_call_layout)
        val optionLeaveChat = contentView.findViewById<TextView>(R.id.leave_group_participants_chat)
        if (megaChatApi.getChatRoom(chatId) != null && megaChatApi.getChatRoom(chatId)
                .isMeeting()
        ) {
            optionLeaveChat.setText(R.string.meetings_info_leave_option)
        } else {
            optionLeaveChat.setText(R.string.title_properties_chat_leave_chat)
        }
        val optionChangePermissionsChat =
            contentView.findViewById<TextView>(R.id.change_permissions_group_participants_chat)
        val optionRemoveParticipantChat =
            contentView.findViewById<TextView>(R.id.remove_group_participants_chat)
        val optionInvite = contentView.findViewById<TextView>(R.id.invite_group_participants_chat)

        optionChangePermissionsChat.setOnClickListener(this)
        optionRemoveParticipantChat.setOnClickListener(this)
        optionContactInfoChat.setOnClickListener(this)
        optionStartConversationChat.setOnClickListener(this)
        optionEditProfileChat.setOnClickListener(this)
        optionLeaveChat.setOnClickListener(this)
        optionInvite.setOnClickListener(this)
        optionStartCall.setOnClickListener(this)
        optionStartCall.setVisibility(View.GONE)
        val separatorInfo = contentView.findViewById<View>(R.id.separator_info)
        val separatorChat = contentView.findViewById<View>(R.id.separator_chat)
        val separatorOptions = contentView.findViewById<View>(R.id.separator_options)
        val separatorLeave = contentView.findViewById<View>(R.id.separator_leave)

        if (isScreenInPortrait(requireContext())) {
            titleNameContactChatPanel?.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT.toFloat()))
            titleMailContactChatPanel.setMaxWidth(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT.toFloat()))
        } else {
            titleNameContactChatPanel?.setMaxWidthEmojis(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND.toFloat()))
            titleMailContactChatPanel.setMaxWidth(dp2px(MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND.toFloat()))
        }

        val userStatus =
            if (participantHandle == megaChatApi.myUserHandle) megaChatApi.onlineStatus else getUserStatus(
                participantHandle,
                megaApi,
                megaChatApi
            )
        setContactStatus(userStatus, stateIcon, StatusIconLocation.DRAWER)

        if (participantHandle == megaApi.myUser?.handle) {
            var myFullName = chatController.myFullName
            if (isTextEmpty(myFullName)) {
                myFullName = megaChatApi.myEmail
            }

            titleNameContactChatPanel?.text = myFullName

            titleMailContactChatPanel.text = megaChatApi.myEmail

            val permission = selectedChat?.ownPrivilege

            if (permission == MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write)
            } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access)
            } else {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only)
            }

            optionEditProfileChat.visibility = View.VISIBLE
            val privateRoom = permission?.let { it < MegaChatRoom.PRIV_RO }
            if (privateRoom == true) {
                optionLeaveChat.visibility = View.GONE
            } else {
                optionLeaveChat.visibility = View.VISIBLE
            }

            optionContactInfoChat.visibility = View.GONE
            optionStartConversationChat.visibility = View.GONE
            optionChangePermissionsChat.visibility = View.GONE
            optionRemoveParticipantChat.visibility = View.GONE

            optionInvite.visibility = View.GONE

            megaApi.myUser?.let {
                setImageAvatar(
                    it.handle,
                    megaChatApi.myEmail,
                    myFullName,
                    contactImageView
                )
            }
        } else {
            val fullName = chatController.getParticipantFullName(participantHandle)
            titleNameContactChatPanel?.text = fullName
            val email = chatController.getParticipantEmail(participantHandle)

            val permission = selectedChat?.getPeerPrivilegeByHandle(participantHandle)

            if (permission == MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write)
            } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access)
            } else {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only)
            }

            val contact = megaApi.getContact(email)

            if (contact != null && contact.visibility == MegaUser.VISIBILITY_VISIBLE) {
                optionContactInfoChat.visibility = View.VISIBLE
                optionStartConversationChat.visibility = View.VISIBLE
                optionStartCall.visibility = View.VISIBLE
                optionInvite.visibility = View.GONE

                titleMailContactChatPanel.text = email
            } else {
                optionContactInfoChat.visibility = View.GONE
                optionStartConversationChat.visibility = View.GONE
                optionInvite.visibility = if (chatController.getParticipantEmail(
                        participantHandle
                    ) == null
                ) View.GONE else View.VISIBLE
                titleMailContactChatPanel.visibility = View.GONE
            }

            optionEditProfileChat.visibility = View.GONE
            optionLeaveChat.visibility = View.GONE

            if (selectedChat?.ownPrivilege == MegaChatRoom.PRIV_MODERATOR) {
                optionChangePermissionsChat.visibility = View.VISIBLE
                optionRemoveParticipantChat.visibility = View.VISIBLE
            } else {
                optionChangePermissionsChat.visibility = View.GONE
                optionRemoveParticipantChat.visibility = View.GONE
            }

            setImageAvatar(
                participantHandle,
                if (isTextEmpty(email)) MegaApiAndroid.userHandleToBase64(participantHandle) else email,
                fullName,
                contactImageView
            )
        }

        separatorInfo.setVisibility(
            if ((optionContactInfoChat.getVisibility() == View.VISIBLE ||
                        optionEditProfileChat.getVisibility() == View.VISIBLE) &&
                (optionStartCall.getVisibility() == View.VISIBLE ||
                        optionStartConversationChat.getVisibility() == View.VISIBLE)
            )
                View.VISIBLE
            else
                View.GONE
        )

        separatorChat.visibility = if ((optionStartCall.visibility == View.VISIBLE ||
                    optionStartConversationChat.visibility == View.VISIBLE) &&
            (optionChangePermissionsChat.visibility == View.VISIBLE ||
                    optionInvite.visibility == View.VISIBLE)
        ) View.VISIBLE else View.GONE

        separatorOptions.visibility = if ((optionChangePermissionsChat.visibility == View.VISIBLE ||
                    optionInvite.visibility == View.VISIBLE) && optionLeaveChat.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE

        separatorLeave.visibility = if (optionLeaveChat.visibility == View.VISIBLE &&
            optionRemoveParticipantChat.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE

        super.onViewCreated(view, savedInstanceState)
    }

    private fun isTextEmpty(text: String?): Boolean {
        return text == null || text.trim { it <= ' ' }.isEmpty()
    }

    override fun onClick(v: View) {
        val id = v.getId()
        if (id == R.id.contact_info_group_participants_chat) {
            val intent = Intent(requireActivity(), ContactInfoActivity::class.java)
            intent.putExtra(NAME, chatController.getParticipantEmail(participantHandle))
            startActivity(
                intent
            )
        } else if (id == R.id.start_chat_group_participants_chat) {
            viewModel.onSendMsgTap()
        } else if (id == R.id.contact_list_option_call_layout) {
            userWaitingForCall = participantHandle
            if (canCallBeStartedFromContactOption(requireActivity())) {
                viewModel.onStartCallTap()
            }
        } else if (id == R.id.change_permissions_group_participants_chat) {
            viewModel.onChangePermissionsTap()
        } else if (id == R.id.remove_group_participants_chat) {
            viewModel.onRemoveParticipantTap(true)
        } else if (id == R.id.edit_profile_group_participants_chat) {
            val editProfile = Intent(requireActivity(), MyAccountActivity::class.java)
            startActivity(editProfile)
        } else if (id == R.id.leave_group_participants_chat) {
            viewModel.onLeaveGroupTap()
        } else if (id == R.id.invite_group_participants_chat) {
            viewModel.onInviteContactTap()
        }

        dismissAllowingStateLoss()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ChatNavKey.LEGACY_CHAT_ID, chatId)
        outState.putLong(CONTACT_HANDLE, participantHandle)
    }

    companion object {
        fun newInstance(
            chatId: Long,
            participantHandle: Long,
        ): ScheduledMeetingParticipantBottomSheetDialogFragment {
            val fragment = ScheduledMeetingParticipantBottomSheetDialogFragment()
            val arguments = Bundle()
            arguments.putLong(ChatNavKey.LEGACY_CHAT_ID, chatId)
            arguments.putLong(CONTACT_HANDLE, participantHandle)
            fragment.setArguments(arguments)
            return fragment
        }
    }
}
