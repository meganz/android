package mega.privacy.android.app.meeting.fragments

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.R

class CreateMeetingFragment : MeetingBaseFragment() {

    companion object {
        fun newInstance() = CreateMeetingFragment()
    }

    private lateinit var viewModel: CreateMeetingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.create_meeting_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateMeetingViewModel::class.java)
    }

}