package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_LINK

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [JoinMeetingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class JoinMeetingFragment : AbstractMeetingOnBoardingFragment() {

    private val viewModel: JoinMeetingViewModel by viewModels()

    override fun meetingButtonClick() {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_start_join_meeting.setText(R.string.btn_join_meeting_as_guest)
        type_meeting_edit_text.visibility = View.GONE
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = "meeting name"
            subtitle = arguments?.getString(MEETING_LINK)
        }
    }
}
