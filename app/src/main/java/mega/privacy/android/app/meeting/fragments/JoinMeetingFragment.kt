package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R

@AndroidEntryPoint
class JoinMeetingFragment : AbstractMeetingOnBoardingFragment() {

    private val viewModel: JoinMeetingViewModel by viewModels()

    override fun meetingButtonClick() {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btn_start_join_meeting.setText(R.string.btn_join_meeting)
    }
}
