package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import mega.privacy.android.app.databinding.BottomSheetMeetingParticipantBinding
import mega.privacy.android.app.meeting.BottomFloatingPanelListener
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment

class MeetingParticipantBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val binding =
            BottomSheetMeetingParticipantBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root;
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true)

        if (!isModerator || isGuest) {
            binding.dividerPingToSpeaker.isVisible = false
            binding.makeModerator.isVisible = false
            binding.dividerMakeModerator.isVisible = false
            binding.removeParticipant.isVisible = false
        }

        if (!isContact || isGuest) {
            if (isGuest) {
                binding.dividerContactInfo.isVisible = false
            } else {
                binding.addContact.isVisible = true
            }

            binding.contactInfo.isVisible = false
            binding.dividerSendMessage.isVisible = false
            binding.sendMessage.isVisible = false
        }

        listenAction(binding.addContact) { listener?.onAddContact() }
        listenAction(binding.contactInfo) { listener?.onContactInfo() }
        listenAction(binding.sendMessage) { listener?.onSendMessage() }
        listenAction(binding.pingToSpeaker) { listener?.onPingToSpeakerView() }
        listenAction(binding.makeModerator) { listener?.onMakeModerator() }
        listenAction(binding.removeParticipant) { listener?.onRemoveParticipant() }

        switchRole()
    }

    private fun listenAction(view: View, action: () -> Unit) {
        view.setOnClickListener {
            action()
            dismiss()
        }
    }

    private val listener: BottomFloatingPanelListener?
        get() {
            val host = activity
            return if (host is BottomFloatingPanelListener) {
                host
            } else {
                null
            }
        }

    companion object {
        private var isModerator = true
        private var isContact = true
        private var isGuest = false

        private fun switchRole() {
            when {
                isModerator && isContact -> isContact = false
                isModerator && !isContact -> isModerator = false
                !isModerator && !isContact -> isContact = true
                !isModerator && isContact -> isGuest = true
                isGuest -> {
                    isModerator = true
                    isContact = true
                    isGuest = false
                }
            }
        }
    }
}
