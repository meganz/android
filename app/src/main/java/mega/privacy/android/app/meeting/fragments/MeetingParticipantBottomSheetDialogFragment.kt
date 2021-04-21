package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private val bottomViewModel: MeetingParticipantBottomSheetDialogViewModel by viewModels()

    // Get from activity
    private var isModerator = true
    private var isGuest = false

    // Get from participant
    private var isContact = false
    private var isMe = false

    private lateinit var participantItem: Participant

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        if (arguments?.getSerializable(EXTRA_PARTICIPANT) != null) {
            participantItem = arguments?.getSerializable(EXTRA_PARTICIPANT) as Participant
            isMe = participantItem.isMe
            isContact = participantItem.isContact
        }

        isGuest = arguments?.getBoolean(EXTRA_IS_GUEST) == true
        isModerator = arguments?.getBoolean(EXTRA_IS_MODERATOR) == true

        bottomViewModel.initValue(requireContext(), isModerator, isGuest, participantItem)

        val binding =
            BottomSheetMeetingParticipantBinding.inflate(LayoutInflater.from(context), null, false)
                .apply {
                    lifecycleOwner = this@MeetingParticipantBottomSheetDialogFragment
                    viewModel = bottomViewModel
                    participant = participantItem
                }

        contentView = binding.root
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true)

        initHost()
        initItemAction(binding)
    }

    private fun initGuest() {
        isGuest = true
        isModerator = false
    }


    private fun initHost() {
        isGuest = false
        isModerator = true
    }

    private fun initUser() {
        isGuest = false
        isModerator = false
    }

    /**
     * Init the action for different items
     */
    private fun initItemAction(binding: BottomSheetMeetingParticipantBinding) {
        listenAction(binding.addContact) { onAddContact() }
        listenAction(binding.contactInfo) { onContactInfoOrEditProfile() }
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
        Toast.makeText(requireContext(), "onAddContact", Toast.LENGTH_SHORT).show()
//        ContactController(this).inviteContact(email)
    }

    private fun onContactInfoOrEditProfile() {
        if (!bottomViewModel.showEditProfile()) {
            Toast.makeText(requireContext(), "onContactInfo", Toast.LENGTH_SHORT).show()
//        ContactUtil.openContactInfoActivity(context, chatC.getParticipantEmail(participantHandle))
        } else {
            Toast.makeText(requireContext(), "onEditProfile", Toast.LENGTH_SHORT).show()
            editProfile()
        }

    }

    private fun onSendMessage() {
//        startConversation()

        Toast.makeText(requireContext(), "onSendMessage", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(requireContext(), "onPingToSpeakerView", Toast.LENGTH_SHORT).show()
        // Notify the `in-meeting-fragment` to update the background
    }

    private fun onMakeModerator() {
        Toast.makeText(requireContext(), "onMakeModerator", Toast.LENGTH_SHORT).show()

//        megaChatApi.updateChatPermissions(
//            chatid,
//            handler,
//            privilege,
//            context as GroupChatInfoActivityLollipop
//        )
    }

    /**
     * Shows an alert dialog to confirm the deletion of a participant.
     *
     */
    private fun onRemoveParticipant() {
        Toast.makeText(requireContext(), "onRemoveParticipant", Toast.LENGTH_SHORT).show()

//        val name: String = chatC.getParticipantFullName(handle)
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).apply {
            setMessage(resources.getString(R.string.confirmation_remove_chat_contact, "name"))
            setPositiveButton(R.string.general_remove) { _, _ -> removeParticipant() }
            setNegativeButton(R.string.general_cancel, null)
            show()
        }
    }

    private fun removeParticipant() {
//        chatC.removeParticipant(chatHandle, selectedHandleParticipant)
    }


    /**
     * Open edit profile page
     */
    private fun editProfile() {
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
