package mega.privacy.android.app.meeting.fragments

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.openlink.OpenLinkActivity
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util

class PasteMeetingLinkGuestDialogFragment : DialogFragment() {

    private lateinit var linkEdit: EditText
    private lateinit var errorLayout: ViewGroup
    private lateinit var errorText: TextView

    private var meetingLink: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_paste_meeting_link_guest, null)

        linkEdit = view.findViewById(R.id.meeting_link)
        errorLayout = view.findViewById(R.id.error)
        errorText = view.findViewById(R.id.error_text)

        linkEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (errorLayout.visibility == View.VISIBLE) {
                    hideError()
                }
            }
        })

        builder.setTitle(R.string.paste_meeting_link_guest_dialog_title)
            .setMessage(getString(R.string.paste_meeting_link_guest_instruction))
            .setView(view)
            .setPositiveButton(R.string.general_ok, null)
            .setNegativeButton(R.string.general_cancel, null)

        val dialog = builder.create()
        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
        Util.showKeyboardDelayed(linkEdit)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            meetingLink = linkEdit.text.toString()

            if (TextUtils.isEmpty(meetingLink)) {
                showError(R.string.invalid_meeting_link_empty)
                return@setOnClickListener
            }

            // Meeting Link and Chat Link are exactly the same format.
            // Using extra approach(getMegaHandleList of openChatPreview())
            // to judge if its a meeting link later on
            if (Util.matchRegexs(meetingLink, Constants.CHAT_LINK_REGEXS)) {
                // Need to call the async checkChatLink() to check if the chat has a call and
                // get the meeting name
                // Delegate the checking to OpenLinkActivity
                // If yes, show Join Meeting, If no, show Chat history
                startOpenLinkActivity()
                dismiss()
            } else {
                showError(R.string.invalid_meeting_link_args)
            }
        }

        return dialog
    }

    private fun startOpenLinkActivity() {
        val intent = Intent(requireContext(), OpenLinkActivity::class.java)
        intent.putExtra(ACTION_JOIN_AS_GUEST, "any")
        intent.data = Uri.parse(meetingLink)
        startActivity(intent)
    }

    private fun showError(errorStringId: Int) {
        setErrorAwareInputAppearance(linkEdit, true)
        errorLayout.visibility = View.VISIBLE
        errorText.text = getString(errorStringId)
    }

    private fun hideError() {
        setErrorAwareInputAppearance(linkEdit, false)
        errorLayout.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        Util.showKeyboardDelayed(linkEdit)
    }

    override fun onPause() {
        super.onPause()
        Util.hideKeyboard(activity)
    }

    companion object {
        const val TAG = "PasteMeetingLinkGuestDialog"

        const val ACTION_JOIN_AS_GUEST = "action_join_as_guest"
    }
}