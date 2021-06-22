package mega.privacy.android.app.meeting.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.StringResourcesUtils

class MeetingHasEndedDialogFragment(private val clickCallback: ClickCallback) :
    DialogFragment() {

    interface ClickCallback {
        fun onViewMeetingChat()
        fun onLeave()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setMessage(StringResourcesUtils.getString(R.string.meeting_has_ended))
            .setPositiveButton(R.string.general_cancel, null)
            .setNegativeButton(R.string.view_meeting_chat, null)

        val dialog = builder.create()
        dialog.show()
        dialog.setCanceledOnTouchOutside(false)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            clickCallback.onLeave()
            dismiss()
        }

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            clickCallback.onViewMeetingChat()
            dismiss()
        }

        return dialog
    }

    companion object {
        const val TAG = "MeetingHasEndedDialog"
    }
}