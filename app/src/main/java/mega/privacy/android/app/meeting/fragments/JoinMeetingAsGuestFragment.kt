package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.TextUtil
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

        if (!isGuestNameValid()) {
            Util.showToast(requireContext(), getString(R.string.error_invalid_guest_name))
            return
        }

        releaseVideoAndHideKeyboard()
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

    private fun isGuestNameValid(): Boolean {
        firstName = edit_first_name.text.toString()
        lastName = edit_last_name.text.toString()

        return !TextUtil.isTextEmpty(firstName) && !TextUtil.isTextEmpty(lastName)
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