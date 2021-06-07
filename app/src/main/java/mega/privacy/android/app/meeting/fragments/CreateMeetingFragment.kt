package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_CREATE
import mega.privacy.android.app.utils.ChatUtil.isAllowedTitle
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.hideKeyboardView
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaChatRoom
import java.util.*


@AndroidEntryPoint
class CreateMeetingFragment : AbstractMeetingOnBoardingFragment() {

    private val viewModel: CreateMeetingViewModel by viewModels()

    //Create first the chat
    var chats = ArrayList<MegaChatRoom>()

    override fun onMeetingButtonClick() {
        if (meetingName.isEmpty() || !isAllowedTitle(meetingName)) {
            type_meeting_edit_text.error =
                StringResourcesUtils.getString(R.string.error_meeting_name_error)
            return
        }

        logDebug("Meeting Name: $meetingName")
        hideKeyboardView(type_meeting_edit_text.context, type_meeting_edit_text, 0)
        // TODO: better to pass meeting name via fragment args in Navigation
        sharedModel.setMeetingsName(meetingName)
        releaseVideoDeviceAndRemoveChatVideoListener()
        val action = InMeetingFragmentDirections.actionGlobalInMeeting(action = MEETING_ACTION_CREATE, meetingName = meetingName)
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        initComponent()
    }

    /**
     * Initialize components of UI
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initComponent() {
        binding.root.setOnTouchListener { v, event ->
            when (event.action) {
                ACTION_DOWN -> {
                    if (v != null) {
                        if (v != type_meeting_edit_text)
                            hideKeyboardView(
                                type_meeting_edit_text.context,
                                type_meeting_edit_text,
                                0
                            )
                    }
                }
            }
            true
        }
        binding.typeMeetingEditText.let {
            it.visibility = View.VISIBLE
            it.hint = meetingName
            showKeyboardDelayed(type_meeting_edit_text)
            it.setOnFocusChangeListener { v, hasFocus ->
                run {
                    if (hasFocus) {
                        type_meeting_edit_text.setSelection(type_meeting_edit_text.text.length)
                    } else {
                        hideKeyboardView(v.context, type_meeting_edit_text, 0)
                    }
                }
            }
        }
    }

    /**
     * Initialize ViewModel
     */
    private fun initViewModel() {
        binding.createviewmodel = viewModel
        // Set default meeting name
        viewModel.initMeetingName()
        viewModel.initRTCAudioManager()

        viewModel.meetingName.observe(viewLifecycleOwner) {
            meetingName = it
        }
    }
}