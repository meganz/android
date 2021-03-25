package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.create_meeting_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomepageBottomSheetBehavior
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

@AndroidEntryPoint
class CreateMeetingFragment : MeetingBaseFragment(), MegaRequestListenerInterface {
    private var meetingName: String = ""
    private val viewModel: CreateMeetingViewModel  by viewModels()

    companion object {
        private const val KEY_MEETING_NAME = "meetingName"
        fun newInstance() = CreateMeetingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.create_meeting_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.let {
            meetingName = it.getString(KEY_MEETING_NAME, "").toString()
            if (!TextUtils.isEmpty(meetingName)){
                type_meeting_edit_text.setText(meetingName)
            }
        }

        viewModel.result.observe(viewLifecycleOwner){
            (activity as MeetingActivity).setBottomFloatingPanelViewHolder(true)
        }
        btn_start_meeting.setOnClickListener{
            viewModel.createMeeting()
        }

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

    /**
     * Get Avatar
     */
    fun setProfileAvatar() {
        LogUtil.logDebug("setProfileAvatar")
        viewModel.avatar.observe(viewLifecycleOwner) {
            meeting_thumbnail.setImageBitmap(it)
        }
    }

    /**
     * Pop key board immediately when "Create Meeting" screen is shown
     */
    private fun showKeyboardDelayed(view: EditText) {
        GlobalScope.async {
            delay(50)
            view.isFocusable = true;
            view.isFocusableInTouchMode = true;
            view.requestFocus();
            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
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