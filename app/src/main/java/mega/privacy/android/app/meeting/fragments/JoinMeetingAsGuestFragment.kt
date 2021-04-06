package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class JoinMeetingAsGuestFragment : Fragment() {

    private val viewModel: JoinMeetingAsGuestViewModel by viewModels()

    companion object {
        fun newInstance() = JoinMeetingAsGuestFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}