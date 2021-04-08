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
import kotlinx.android.synthetic.main.dialog_paste_meeting_link_guest.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.lollipop.megachat.AndroidMegaRichLinkMessage
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.ColorUtils.setErrorAwareInputAppearance
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.showSnackbar
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest

class PasteMeetingLinkGuestFragment : DialogFragment() {

    private lateinit var linkEdit: EditText
    private lateinit var errorLayout: ViewGroup
    private lateinit var errorText: TextView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val inflater = requireActivity().layoutInflater;
        val view = inflater.inflate(R.layout.dialog_paste_meeting_link_guest, null)

        linkEdit = view.findViewById(R.id.meeting_link)
        errorLayout = view.findViewById(R.id.error)
        errorText = view.findViewById(R.id.error_text)

        linkEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (errorLayout.visibility == View.VISIBLE) {
                    setErrorAwareInputAppearance(linkEdit, false)
                    errorLayout.visibility = View.GONE
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
            val meetingLink = linkEdit.text.toString()

            if (validateLink(meetingLink)) {
                MegaApplication.getInstance().getMegaChatApi().checkChatLink(
                    meetingLink,
                    object : ChatBaseListener(requireContext()) {
                        override fun onRequestFinish(
                            api: MegaChatApiJava,
                            request: MegaChatRequest,
                            e: MegaChatError
                        ) {
                            if (e.errorCode == MegaChatError.ERROR_OK || e.errorCode == MegaChatError.ERROR_EXIST) {
                                if (isTextEmpty(request.link) && request.chatHandle == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                                    showSnackbar(
                                        requireContext(),
                                        Constants.SNACKBAR_TYPE,
                                        getString(R.string.error_chat_link_init_error),
                                        MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                                    )
                                    return
                                }

                                val joinMeetingLinkIntent =
                                    Intent(requireContext(), MeetingActivity::class.java)
                                joinMeetingLinkIntent.action = Constants.ACTION_JOIN_MEETING
                                joinMeetingLinkIntent.data = Uri.parse(request.link)
                                joinMeetingLinkIntent.putExtra(
                                    MeetingActivity.MEETING_TYPE,
                                    MeetingActivity.MEETING_TYPE_JOIN
                                )
                                startActivity(joinMeetingLinkIntent)

                                dismiss()
                            } else if (e.errorCode == MegaChatError.ERROR_NOENT) {
                                Util.showAlert(
                                    requireContext(),
                                    getString(R.string.invalid_meeting_link),
                                    getString(R.string.meeting_link)
                                )
                            } else {
                                setErrorAwareInputAppearance(linkEdit, true)
                                errorLayout.visibility = View.VISIBLE
                                errorText.text = getString(R.string.invalid_meeting_link_args)
                            }
                        }
                    })
            }
        }

        return dialog
    }

    private fun validateLink(link: String): Boolean {
        val isEmpty = TextUtils.isEmpty(link)
        val isMeetingLink = AndroidMegaRichLinkMessage.isMeetingLink(link)

        if (!isEmpty && isMeetingLink) return true

        setErrorAwareInputAppearance(linkEdit, true)
        errorLayout.visibility = View.VISIBLE

        errorText.text = if (isEmpty) {
            getString(R.string.invalid_meeting_link_empty)
        } else {
            getString(R.string.invalid_meeting_link_args)
        }

        return false
    }

    override fun onResume() {
        super.onResume()
        Util.showKeyboardDelayed(meeting_link)
    }

    companion object {
        const val TAG = "PasteMeetingLinkGuestDialog"
    }
}