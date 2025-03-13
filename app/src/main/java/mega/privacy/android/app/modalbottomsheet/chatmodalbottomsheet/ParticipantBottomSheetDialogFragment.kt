package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication.Companion.userWaitingForCall
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.myAccount.MyAccountActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.ChatUtil.StatusIconLocation
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaUser
import timber.log.Timber

@AndroidEntryPoint
class ParticipantBottomSheetDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private var selectedChat: MegaChatRoom? = null
    private var chatId = MegaApiJava.INVALID_HANDLE
    private var participantHandle = MegaApiJava.INVALID_HANDLE
    private var chatC: ChatController? = null

    private var titleNameContactChatPanel: EmojiTextView? = null
    private var contactImageView: RoundedImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        contentView = View.inflate(context, R.layout.bottom_sheet_group_participant, null)
        itemsLayout = contentView.findViewById<View?>(R.id.items_layout)
        titleNameContactChatPanel =
            contentView.findViewById<EmojiTextView>(R.id.group_participants_chat_name_text)

        chatId = arguments?.getLong(Constants.CHAT_ID, MegaApiJava.INVALID_HANDLE)
            ?: savedInstanceState?.getLong(Constants.CHAT_ID, MegaApiJava.INVALID_HANDLE)
                    ?: (requireActivity() as GroupChatInfoActivity).chatHandle

        participantHandle =
            arguments?.getLong(Constants.CONTACT_HANDLE, MegaApiJava.INVALID_HANDLE)
                ?: savedInstanceState?.getLong(Constants.CONTACT_HANDLE, MegaApiJava.INVALID_HANDLE)
                        ?: (requireActivity() as GroupChatInfoActivity).selectedHandleParticipant

        selectedChat = megaChatApi.getChatRoom(chatId)

        chatC = ChatController(requireActivity())

        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (selectedChat == null || participantHandle == MegaApiJava.INVALID_HANDLE) {
            Timber.w("Error. Selected chat is NULL or participant handle is -1")
            return
        }

        val stateIcon = contentView.findViewById<ImageView>(R.id.group_participants_state_circle)

        stateIcon.setVisibility(View.VISIBLE)

        stateIcon.maxWidth = Util.scaleWidthPx(6, resources.displayMetrics)
        stateIcon.maxHeight = Util.scaleHeightPx(6, resources.displayMetrics)

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
                .isMeeting
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
        optionStartCall.visibility = View.GONE
        val separatorInfo = contentView.findViewById<View>(R.id.separator_info)
        val separatorChat = contentView.findViewById<View>(R.id.separator_chat)
        val separatorOptions = contentView.findViewById<View>(R.id.separator_options)
        val separatorLeave = contentView.findViewById<View>(R.id.separator_leave)

        if (Util.isScreenInPortrait(requireContext())) {
            titleNameContactChatPanel?.setMaxWidthEmojis(Util.dp2px(Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT.toFloat()))
            titleMailContactChatPanel.setMaxWidth(Util.dp2px(Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_PORT.toFloat()))
        } else {
            titleNameContactChatPanel?.setMaxWidthEmojis(Util.dp2px(Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND.toFloat()))
            titleMailContactChatPanel.setMaxWidth(Util.dp2px(Constants.MAX_WIDTH_BOTTOM_SHEET_DIALOG_LAND.toFloat()))
        }

        val userStatus =
            if (participantHandle == megaChatApi.myUserHandle) megaChatApi.onlineStatus else ChatUtil.getUserStatus(
                participantHandle
            )
        ChatUtil.setContactStatus(userStatus, stateIcon, StatusIconLocation.DRAWER)

        if (participantHandle == megaApi.myUser?.handle) {
            var myFullName = chatC?.getMyFullName()
            if (TextUtil.isTextEmpty(myFullName)) {
                myFullName = megaChatApi.myEmail
            }

            titleNameContactChatPanel?.text = myFullName

            titleMailContactChatPanel.text = megaChatApi.myEmail

            val permission = selectedChat?.ownPrivilege ?: -1

            if (permission == MegaChatRoom.PRIV_STANDARD) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_write)
            } else if (permission == MegaChatRoom.PRIV_MODERATOR) {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_full_access)
            } else {
                permissionsIcon.setImageResource(R.drawable.ic_permissions_read_only)
            }

            if (permission < MegaChatRoom.PRIV_RO) {
                optionLeaveChat.visibility = View.GONE
            } else {
                optionLeaveChat.visibility = View.VISIBLE
            }

            optionEditProfileChat.visibility = View.VISIBLE

            optionContactInfoChat.visibility = View.GONE
            optionStartConversationChat.visibility = View.GONE
            optionChangePermissionsChat.visibility = View.GONE
            optionRemoveParticipantChat.visibility = View.GONE

            optionInvite.visibility = View.GONE

            megaApi.myUser?.handle?.let {
                AvatarUtil.setImageAvatar(
                    it,
                    megaChatApi.myEmail,
                    myFullName,
                    contactImageView
                )
            }
        } else {
            val fullName = chatC?.getParticipantFullName(participantHandle)
            titleNameContactChatPanel?.text = fullName
            val email = chatC?.getParticipantEmail(participantHandle)

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
                optionInvite.visibility =
                    if (chatC?.getParticipantEmail(participantHandle) == null) View.GONE else View.VISIBLE
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

            AvatarUtil.setImageAvatar(
                participantHandle,
                if (TextUtil.isTextEmpty(email)) MegaApiAndroid.userHandleToBase64(participantHandle) else email,
                fullName,
                contactImageView
            )
        }

        separatorInfo.visibility = if ((optionContactInfoChat.isVisible ||
                    optionEditProfileChat.isVisible) &&
            (optionStartCall.isVisible ||
                    optionStartConversationChat.isVisible)
        )
            View.VISIBLE
        else
            View.GONE

        separatorChat.visibility = if ((optionStartCall.isVisible ||
                    optionStartConversationChat.isVisible) &&
            (optionChangePermissionsChat.isVisible ||
                    optionInvite.isVisible)
        ) View.VISIBLE else View.GONE

        separatorOptions.visibility = if ((optionChangePermissionsChat.isVisible ||
                    optionInvite.isVisible) && optionLeaveChat.isVisible
        ) View.VISIBLE else View.GONE

        separatorLeave.visibility = if (optionLeaveChat.isVisible &&
            optionRemoveParticipantChat.isVisible
        ) View.VISIBLE else View.GONE

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.contact_info_group_participants_chat) {
            ContactUtil.openContactInfoActivity(
                requireActivity(),
                chatC?.getParticipantEmail(participantHandle)
            )
        } else if (id == R.id.start_chat_group_participants_chat) {
            (requireActivity() as GroupChatInfoActivity).startConversation(participantHandle)
        } else if (id == R.id.contact_list_option_call_layout) {
            userWaitingForCall = participantHandle
            if (CallUtil.canCallBeStartedFromContactOption(requireActivity())) {
                (requireActivity() as GroupChatInfoActivity).startCall()
            }
        } else if (id == R.id.change_permissions_group_participants_chat) {
            selectedChat?.let {
                (requireActivity() as GroupChatInfoActivity).showChangePermissionsDialog(
                    participantHandle,
                    it
                )
            }
        } else if (id == R.id.remove_group_participants_chat) {
            (requireActivity() as GroupChatInfoActivity).showRemoveParticipantConfirmation(
                participantHandle
            )
        } else if (id == R.id.edit_profile_group_participants_chat) {
            val editProfile = Intent(requireActivity(), MyAccountActivity::class.java)
            startActivity(editProfile)
        } else if (id == R.id.leave_group_participants_chat) {
            (requireActivity() as GroupChatInfoActivity).showConfirmationLeaveChat()
        } else if (id == R.id.invite_group_participants_chat) {
            (requireActivity() as GroupChatInfoActivity).inviteContact(
                chatC?.getParticipantEmail(
                    participantHandle
                )
            )
        }

        dismissAllowingStateLoss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(Constants.CHAT_ID, chatId)
        outState.putLong(Constants.CONTACT_HANDLE, participantHandle)
    }

    fun updateContactData() {
        if (participantHandle == megaApi.myUser?.handle) {
            var myFullName = chatC?.getMyFullName()
            if (TextUtil.isTextEmpty(myFullName)) {
                myFullName = megaChatApi.myEmail
            }

            titleNameContactChatPanel?.text = myFullName
            megaApi.myUser?.handle?.let {
                AvatarUtil.setImageAvatar(
                    it,
                    megaChatApi.myEmail,
                    myFullName,
                    contactImageView
                )
            }
        } else {
            val fullName = chatC?.getParticipantFullName(participantHandle)
            titleNameContactChatPanel?.text = fullName
            val email = chatC?.getParticipantEmail(participantHandle)

            AvatarUtil.setImageAvatar(
                participantHandle,
                if (TextUtil.isTextEmpty(email)) MegaApiAndroid.userHandleToBase64(participantHandle) else email,
                fullName,
                contactImageView
            )
        }
    }

    companion object {
        fun newInstance(
            chatId: Long,
            participantHandle: Long,
        ): ParticipantBottomSheetDialogFragment {
            val fragment = ParticipantBottomSheetDialogFragment()
            val arguments = Bundle()
            arguments.putLong(Constants.CHAT_ID, chatId)
            arguments.putLong(Constants.CONTACT_HANDLE, participantHandle)
            fragment.setArguments(arguments)
            return fragment
        }
    }
}
