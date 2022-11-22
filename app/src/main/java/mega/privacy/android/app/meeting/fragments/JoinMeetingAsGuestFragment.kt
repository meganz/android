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
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber

@AndroidEntryPoint
class JoinMeetingAsGuestFragment : AbstractMeetingOnBoardingFragment() {

    private var firstName: String = ""
    private var lastName: String = ""

    override fun onMeetingButtonClick() {
        if (chatId == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Chat Id is invalid when join meeting")
            return
        }

        releaseVideoAndHideKeyboard()
        firstName = binding.editFirstName.text.toString()
        lastName = binding.editLastName.text.toString()

        val action = JoinMeetingFragmentDirections
            .actionGlobalInMeeting(
                MeetingActivity.MEETING_ACTION_GUEST,
                chatId,
                MEGACHAT_INVALID_HANDLE,
                meetingName,
                meetingLink,
                firstName,
                lastName
            )
        findNavController().navigate(action)
    }

    /**
     * Method for releasing the video and hiding the keyboard
     */
    fun releaseVideoAndHideKeyboard() {
        Util.hideKeyboardView(binding.typeMeetingEditText.context, binding.typeMeetingEditText, 0)
        releaseVideoDeviceAndRemoveChatVideoListener()
    }

    private fun watchChangeOfGuestName() {
        binding.btnStartJoinMeeting.isEnabled = false

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnStartJoinMeeting.isEnabled =
                    !TextUtils.isEmpty(binding.editFirstName.text)
                            && !TextUtils.isEmpty(binding.editLastName.text)
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.editFirstName.addTextChangedListener(textWatcher)
        binding.editLastName.addTextChangedListener(textWatcher)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRTCAudioManager()

        binding.mainBk.visibility = View.VISIBLE
        binding.editFirstName.visibility = View.VISIBLE
        binding.editLastName.visibility = View.VISIBLE
        binding.btnStartJoinMeeting.setText(R.string.btn_join_meeting_as_guest)
        Util.showKeyboardDelayed(binding.editFirstName)
        reLayoutCameraPreviewView()
        binding.typeMeetingEditText.visibility = View.GONE
        watchChangeOfGuestName()
    }

    override fun setProfileAvatar() {
        binding.meetingThumbnail.apply {
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
        constraintSet.clone(binding.createMeeting)
        constraintSet.connect(
            R.id.localTextureView,
            ConstraintSet.BOTTOM,
            R.id.edit_first_name,
            ConstraintSet.TOP,
        )
        constraintSet.applyTo(binding.createMeeting)
    }
}