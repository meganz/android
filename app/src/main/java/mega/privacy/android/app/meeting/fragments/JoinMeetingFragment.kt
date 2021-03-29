package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R

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
    private lateinit var viewModel: JoinMeetingViewModel
    override fun onSubCreateView(view: View) {

    }

    override fun meetingButtonClick() {

    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.join_meeting_fragment, container, false)
//    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(JoinMeetingViewModel::class.java)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance() = CreateMeetingFragment()
    }
}
