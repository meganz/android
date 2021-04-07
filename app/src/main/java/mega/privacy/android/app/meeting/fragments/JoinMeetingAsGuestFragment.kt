package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util

class JoinMeetingAsGuestFragment : Fragment() {

    private val viewModel: JoinMeetingAsGuestViewModel by viewModels()

    companion object {
        fun newInstance() = JoinMeetingAsGuestFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edit_first_name.visibility = View.VISIBLE
        edit_last_name.visibility = View.VISIBLE
        btn_start_join_meeting.setText(R.string.btn_join_meeting_as_guest)
        Util.showKeyboardDelayed(edit_first_name)
    }
}