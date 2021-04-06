package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

@AndroidEntryPoint
class CreateMeetingFragment : AbstractMeetingOnBoardingFragment(), MegaRequestListenerInterface {
    private var meetingName: String = ""
    private val viewModel: CreateMeetingViewModel by viewModels()

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s != null) {
                btn_start_join_meeting.isClickable = s.isNotEmpty()
            }
        }

        override fun afterTextChanged(s: Editable?) {

        }

    }

    companion object {
        private const val KEY_MEETING_NAME = "meetingName"
    }

    override fun onSubCreateView(view: View) {

    }

    override fun meetingButtonClick() {
        viewModel.createMeeting()

        // TODO delete test code: to InMeetingFragment
        findNavController().navigate(R.id.inMeetingFragment)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            meetingName = it.getString(KEY_MEETING_NAME, "").toString()
            if (!TextUtils.isEmpty(meetingName)) {
                type_meeting_edit_text.setText(meetingName)
            }
        }
        abstractMeetingOnBoardingViewModel.result.observe(viewLifecycleOwner) {
            (activity as MeetingActivity).setBottomFloatingPanelViewHolder(true)
        }

        btn_start_join_meeting.setOnClickListener {
            hideKeyboard(type_meeting_edit_text)
            meetingButtonClick()
        }
        // It is valid when setting isClickable after setOnClickListener
        // TODO delete test code: Uncomment
//        btn_start_join_meeting.isClickable = false

        type_meeting_edit_text.addTextChangedListener(textWatcher)
        showKeyboardDelayed(type_meeting_edit_text)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setProfileAvatar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (!TextUtils.isEmpty(meetingName)) {
            outState.putString(KEY_MEETING_NAME, meetingName)
        }
    }

    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        Toast.makeText(requireContext(), "onRequestStart", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        Toast.makeText(requireContext(), "onRequestUpdate", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        Toast.makeText(requireContext(), "onRequestFinish", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        Toast.makeText(requireContext(), "onRequestTemporaryError", Toast.LENGTH_SHORT).show()
    }
}