package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.showKeyboardDelayed
import nz.mega.sdk.*
import java.util.*

@AndroidEntryPoint
class CreateMeetingFragment : AbstractMeetingOnBoardingFragment() {

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
            Util.hideKeyboardView(type_meeting_edit_text.context, type_meeting_edit_text, 0)
            findNavController().navigate(CreateMeetingFragmentDirections.actionCreateMeetingFragmentToInMeeting())
        }

        // TODO delete test code start: to InMeetingFragment
        findNavController().navigate(R.id.inMeetingFragment)
        // TODO delete test code end: to InMeetingFragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        type_meeting_edit_text.visibility = View.VISIBLE
        type_meeting_edit_text.hint = StringResourcesUtils.getString(
            R.string.type_meeting_name, megaChatApi.myFullname
        )
        initViewModel()
    }

    /**
     * Initialize ViewModel
     */
    private fun initViewModel() {
        binding.let {
            it.createviewmodel = viewModel
            if (it.typeMeetingEditText.isVisible) {
                showKeyboardDelayed(type_meeting_edit_text)
                // Set default meeting name
                viewModel.initMeetingName(StringResourcesUtils.getString(
                    R.string.type_meeting_name, megaChatApi.myFullname
                ))
            }
        }
    }
}