package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.R

class IndividualCallFragment : MeetingBaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.individual_call_fragment, container, false)
    }

    companion object {

        const val TAG = "IndividualCallFragment"

        @JvmStatic
        fun newInstance() = IndividualCallFragment()
    }
}