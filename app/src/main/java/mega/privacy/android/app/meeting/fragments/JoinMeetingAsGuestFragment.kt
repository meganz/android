package mega.privacy.android.app.meeting.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment.ClickCallback
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

        sharedModel.checkIfCallExists(meetingLink)
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


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                sharedModel.state.collect { (isMeetingEnded) ->
                    isMeetingEnded?.let {
                        if (it) {
                            MeetingHasEndedDialogFragment(object : ClickCallback {
                                override fun onViewMeetingChat() {}
                                override fun onLeave() {
                                    val intent =
                                        Intent(requireContext(), LeftMeetingActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    sharedModel.finishMeetingActivity()
                                }
                            }, true).show(parentFragmentManager,
                                MeetingHasEndedDialogFragment.TAG)
                        } else {
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
                    }
                }
            }
        }
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