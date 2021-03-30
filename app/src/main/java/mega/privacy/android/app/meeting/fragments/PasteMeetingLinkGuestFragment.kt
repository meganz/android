package mega.privacy.android.app.meeting.fragments

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util


class PasteMeetingLinkGuestFragment : DialogFragment() {
    lateinit var editText: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater;
        val view = inflater.inflate(R.layout.dialog_paste_meeting_link_guest, null)

        builder.setTitle(R.string.paste_meeting_link_guest_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.general_ok)
            { _, _ ->

            }
            .setNegativeButton(
                R.string.general_cancel
            ) { dialog, _ ->
                dialog.cancel()
            }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        editText = view.findViewById(R.id.meeting_link)
        Util.showKeyboardDelayed(editText)

        return dialog
    }

    override fun onResume() {
        super.onResume()
        Util.showKeyboardDelayed(editText)
    }

    companion object {
        const val TAG = "PasteMeetingLinkGuestDialog"
    }
}