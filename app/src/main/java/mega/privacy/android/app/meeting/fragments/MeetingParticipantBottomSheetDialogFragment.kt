package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetMeetingParticipantBinding
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.lollipop.controllers.ContactController
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.*

/**
 * Can use the SDK api from BaseFragment
 */
class MeetingParticipantBottomSheetDialogFragment : BaseBottomSheetDialogFragment(),
    MegaChatRequestListenerInterface {
    private val bottomViewModel: MeetingParticipantBottomSheetDialogViewModel by viewModels()
    private val sharedViewModel: MeetingActivityViewModel by activityViewModels()
    private val inMeetingViewModel: InMeetingViewModel by activityViewModels()

    // Get from activity
    private var isModerator = false
    private var isGuest = false
    private var isSpeakerMode = false

    // Get from participant
    private var isContact = false
    private var isMe = false

    private lateinit var participantItem: Participant
    private lateinit var binding: BottomSheetMeetingParticipantBinding

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
        isSpeakerMode = arguments?.getBoolean(EXTRA_IS_SPEAKER_MODE) == true

        bottomViewModel.initValue(
            requireContext(),
            isModerator,
            isGuest,
            isSpeakerMode,
            participantItem
        )

        binding =
            BottomSheetMeetingParticipantBinding.inflate(LayoutInflater.from(context), null, false)
                .apply {
                    lifecycleOwner = this@MeetingParticipantBottomSheetDialogFragment
                    viewModel = bottomViewModel
                    participant = participantItem
                }

        contentView = binding.root
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true)

        initItemAction(binding)
        initAvatar(participantItem)
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
        listenAction(binding.removeParticipant) {
            sharedViewModel.currentChatId.value?.let {
                onRemoveParticipant(it)
            }
        }
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
        ContactController(requireContext()).inviteContact(
            ChatController(context).getParticipantEmail(
                participantItem.peerId
            )
        )
    }

    private fun onContactInfoOrEditProfile() {
        if (!bottomViewModel.showEditProfile()) {
            ContactUtil.openContactInfoActivity(
                context,
                ChatController(context).getParticipantEmail(participantItem.peerId)
            )
        } else {
            editProfile()
        }

    }

    private fun onSendMessage() {
        startConversation(participantItem.peerId)
    }

    /**
     * Start Conversation
     */
    fun startConversation(handle: Long) {
        logDebug("Handle: $handle")
        val chat = megaChatApi.getChatRoomByUser(handle)
        val peers = MegaChatPeerList.createInstance()
        if (chat == null) {
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD)
            megaChatApi.createChat(false, peers, this)
        } else {
            val intentOpenChat = Intent(requireActivity(), ChatActivityLollipop::class.java)
            intentOpenChat.action = Constants.ACTION_CHAT_SHOW_MESSAGES
            intentOpenChat.putExtra(Constants.CHAT_ID, chat.chatId)
            requireActivity().startActivity(intentOpenChat)
        }
    }

    /**
     * Pin to speaker
     *
     */
    private fun onPingToSpeakerView() {
        inMeetingViewModel.onItemClick(participantItem)
    }

    private fun onMakeModerator() {
        sharedViewModel.currentChatId.value?.let {
            megaChatApi.updateChatPermissions(
                it,
                participantItem.peerId,
                MegaChatRoom.PRIV_MODERATOR,
                this
            )
        }
    }

    /**
     * Shows an alert dialog to confirm the deletion of a participant.
     *
     */
    private fun onRemoveParticipant(chatId: Long) {
        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.ThemeOverlay_Mega_MaterialAlertDialog
        ).apply {
            setMessage(
                resources.getString(
                    R.string.confirmation_remove_chat_contact,
                    participantItem.name
                )
            )
            setPositiveButton(R.string.general_remove) { _, _ ->
               removeParticipant(chatId)
            }
            setNegativeButton(R.string.general_cancel, null)
            show()
        }
    }

    private fun removeParticipant(chatId: Long) {
        if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            megaChatApi.removeFromChat(chatId, participantItem.peerId, this)
        }
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

    private fun initAvatar(participant: Participant) {
        var avatar = bottomViewModel.getImageAvatarCall(participantItem.peerId)
        if (avatar == null) {
            avatar = CallUtil.getDefaultAvatarCall(
                MegaApplication.getInstance().applicationContext,
                participant.peerId
            )
        }

        binding.avatar.setImageBitmap(avatar)
    }

    companion object {
        private const val EXTRA_PARTICIPANT = "extra_participant"
        private const val EXTRA_IS_GUEST = "extra_is_guest"
        private const val EXTRA_IS_MODERATOR = "extra_is_moderator"
        private const val EXTRA_IS_SPEAKER_MODE = "extra_is_speaker_mode"

        /**
         * Get the participant object
         */
        fun newInstance(
            isGuest: Boolean,
            isModerator: Boolean,
            isSpeakerMode: Boolean,
            participant: Participant
        ): MeetingParticipantBottomSheetDialogFragment {
            val args = Bundle()

            args.putSerializable(EXTRA_PARTICIPANT, participant)
            args.putBoolean(EXTRA_IS_GUEST, isGuest)
            args.putBoolean(EXTRA_IS_MODERATOR, isModerator)
            args.putBoolean(EXTRA_IS_SPEAKER_MODE, isSpeakerMode)
            val fragment = MeetingParticipantBottomSheetDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest?) {
    }

    override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) {
    }

    override fun onRequestFinish(
        api: MegaChatApiJava?,
        request: MegaChatRequest?,
        e: MegaChatError?
    ) {

    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava?,
        request: MegaChatRequest?,
        e: MegaChatError?
    ) {

    }
}
