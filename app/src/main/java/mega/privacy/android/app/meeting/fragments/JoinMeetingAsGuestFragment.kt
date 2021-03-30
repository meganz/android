package mega.privacy.android.app.meeting.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.R

class JoinMeetingAsGuestFragment : Fragment() {

    companion object {
        fun newInstance() = JoinMeetingAsGuestFragment()
    }

    private lateinit var viewModel: JoinMeetingAsGuestViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.join_meeting_as_guest_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(JoinMeetingAsGuestViewModel::class.java)
    }

}