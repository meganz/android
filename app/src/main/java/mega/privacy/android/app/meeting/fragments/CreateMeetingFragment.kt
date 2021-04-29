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
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.meeting.listeners.StartChatCallListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util.hideKeyboardView
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.MegaChatRoom
import java.util.*


@AndroidEntryPoint
class CreateMeetingFragment : AbstractMeetingOnBoardingFragment(), SnackbarShower {

    private val viewModel: CreateMeetingViewModel by viewModels()
    private var meetingName: String? = null

    //Create first the chat
    var chats = ArrayList<MegaChatRoom>()

    override fun onMeetingButtonClick() {
        meetingName = viewModel.meetingName.value
        if (meetingName.isNullOrEmpty()) {
            type_meeting_edit_text.error =
                StringResourcesUtils.getString(R.string.error_meeting_name_error)
            return
        }
        logDebug("Meeting Name: $meetingName")
        meetingName?.let {
            hideKeyboardView(type_meeting_edit_text.context, type_meeting_edit_text, 0)
            findNavController().navigate(CreateMeetingFragmentDirections.actionCreateMeetingFragmentToInMeeting())
        }
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
            it.hint = StringResourcesUtils.getString(
                R.string.type_meeting_name, megaChatApi.myFullname
            )
            showKeyboardDelayed(type_meeting_edit_text)
            it.setOnFocusChangeListener { v, hasFocus ->
                run {
                    if (hasFocus) {
                        type_meeting_edit_text.setSelection(type_meeting_edit_text.text.length);
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
        viewModel.initMeetingName(
            StringResourcesUtils.getString(
                R.string.type_meeting_name, megaChatApi.myFullname
            )
        )

        viewModel.initRTCAudioManager()
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        meetingActivity.showSnackbar(type, binding.root, content, chatId)
    }
}