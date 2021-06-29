package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava


@AndroidEntryPoint
class JoinMeetingAsGuestFragment : AbstractMeetingOnBoardingFragment() {

    private var firstName: String = ""
    private var lastName: String = ""

    override fun onMeetingButtonClick() {
        if (chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            LogUtil.logError("Chat Id is invalid when join meeting")
            return
        }

        releaseVideoAndHideKeyboard()
        firstName = edit_first_name.text.toString()
        lastName = edit_last_name.text.toString()

        val action = JoinMeetingFragmentDirections
            .actionGlobalInMeeting(
                MeetingActivity.MEETING_ACTION_GUEST,
                chatId,
                meetingName,
                meetingLink,
                firstName,
                lastName
            )
        findNavController().navigate(action)
    }

    fun releaseVideoAndHideKeyboard() {
        Util.hideKeyboardView(type_meeting_edit_text.context, type_meeting_edit_text, 0)
        releaseVideoDeviceAndRemoveChatVideoListener()
    }

    private fun watchChangeOfGuestName() {
        btn_start_join_meeting.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btn_start_join_meeting.isEnabled = !TextUtils.isEmpty(edit_first_name.text)
                        && !TextUtils.isEmpty(edit_last_name.text)
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        edit_first_name.addTextChangedListener(textWatcher)
        edit_last_name.addTextChangedListener(textWatcher)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRTCAudioManager()

        main_bk.visibility = View.VISIBLE
        edit_first_name.visibility = View.VISIBLE
        edit_last_name.visibility = View.VISIBLE
        btn_start_join_meeting.setText(R.string.btn_join_meeting_as_guest)
        Util.showKeyboardDelayed(edit_first_name)
        reLayoutCameraPreviewView()
        type_meeting_edit_text.visibility = View.GONE
        watchChangeOfGuestName()
    }

    override fun setProfileAvatar() {
        meeting_thumbnail.apply {
            borderColors = null
            borderWidth = 0
            setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_guest_avatar
                )
            )
        }
    }

    /**
     * Constrain the bottom of camera preview surface view to the top of name input EditText
     */
    private fun reLayoutCameraPreviewView() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(create_meeting)
        constraintSet.connect(
            R.id.localSurfaceView,
            ConstraintSet.BOTTOM,
            R.id.edit_first_name,
            ConstraintSet.TOP,
        )
        constraintSet.applyTo(create_meeting)
    }
}