package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetMeetingParticipantBinding
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants

/**
 * Can use the SDK api from BaseFragment
 */
class MeetingParticipantBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    // Get from activity
    private var isModerator = true
    private var isGuest = false

    // Get from participant
    private var isContact = false
    private var isMe = true

    private lateinit var participant: Participant

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val binding =
            BottomSheetMeetingParticipantBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true)

        if (arguments?.getSerializable(EXTRA_PARTICIPANT) != null) {
            participant = arguments?.getSerializable(EXTRA_PARTICIPANT) as Participant
            isMe = participant.isMe
        }

        initItem(binding)
        initItemAction(binding)
    }

    private fun initItem(binding: BottomSheetMeetingParticipantBinding) {
        if (!isModerator || isGuest || isMe) {
            binding.dividerPingToSpeaker.isVisible = false
            binding.makeModerator.isVisible = false
            binding.dividerMakeModerator.isVisible = false
            binding.removeParticipant.isVisible = false
        }

        if (!isContact || isGuest) {
            if (isGuest) {
                binding.dividerContactInfo.isVisible = false
            } else {
                binding.addContact.isVisible = !isMe
            }

            binding.contactInfo.isVisible = false
            binding.dividerSendMessage.isVisible = false
            binding.sendMessage.isVisible = false
        }

        /**
         * If user click the own item, for users, will show the `Edit Profile` item
         * if Guest, will disable the three dots button
         */
        if (isMe && !isGuest) {
            binding.pingToSpeaker.isVisible = false
            binding.contactInfo.isVisible = true
            binding.contactInfo.text = context.getString(R.string.group_chat_edit_profile_label)
        }
    }

    /**
     * Init the action for different items
     */
    private fun initItemAction(binding: BottomSheetMeetingParticipantBinding) {
        listenAction(binding.addContact) { onAddContact() }
        listenAction(binding.contactInfo) { onContactInfo() }
        listenAction(binding.sendMessage) { onSendMessage() }
        listenAction(binding.pingToSpeaker) { onPingToSpeakerView() }
        listenAction(binding.makeModerator) { onMakeModerator() }
        listenAction(binding.removeParticipant) { onRemoveParticipant() }
    }

    /**
     * After execute the action, close the item dialog
     */
    private fun listenAction(view: View, action: () -> Unit) {
        view.setOnClickListener {
            action()
            dismiss()
        }
    }

    private fun onAddContact() {
//        ContactController(this).inviteContact(email)
    }

    private fun onContactInfo() {
//        ContactUtil.openContactInfoActivity(context, chatC.getParticipantEmail(participantHandle))
    }

    private fun onSendMessage() {
//        startConversation()
    }

    /**
     * Start Conversation
     */
    fun startConversation(handle: Long) {
//        LogUtil.logDebug("Handle: $handle")
//        val chat = megaChatApi.getChatRoomByUser(handle)
//        val peers = MegaChatPeerList.createInstance()
//        if (chat == null) {
//            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD)
//            megaChatApi.createChat(false, peers, this)
//        } else {
//            val intentOpenChat = Intent(this, ChatActivityLollipop::class.java)
//            intentOpenChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
//            intentOpenChat.putExtra(Constants.CHAT_ID, chat.chatId)
//            this.startActivity(intentOpenChat)
//        }
    }

    private fun onPingToSpeakerView() {

    }

    private fun onMakeModerator() {

    }

    private fun onRemoveParticipant() {

    }

    /**
     * Open edit profile page
     */
    private fun editProfile(){
        val editProfile = Intent(context, ManagerActivityLollipop::class.java)
        editProfile.action = Constants.ACTION_SHOW_MY_ACCOUNT
        startActivity(editProfile)
        dismissAllowingStateLoss()
    }

    companion object {
        private const val EXTRA_PARTICIPANT = "extra_participant"
        private const val EXTRA_IS_GUEST = "extra_is_guest"
        private const val EXTRA_IS_MODERATOR = "extra_is_moderator"

        /**
         * Get the participant object
         */
        fun newInstance(
            isGuest: Boolean,
            isModerator: Boolean,
            participant: Participant
        ): MeetingParticipantBottomSheetDialogFragment {
            val args = Bundle()

            args.putSerializable(EXTRA_PARTICIPANT, participant)
            args.putBoolean(EXTRA_IS_GUEST, isGuest)
            args.putBoolean(EXTRA_IS_MODERATOR, isModerator)
            val fragment = MeetingParticipantBottomSheetDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
