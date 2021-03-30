package mega.privacy.android.app.meeting.fragments

import androidx.fragment.app.Fragment
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.utils.LogUtil.logDebug

/**
 * A simple [Fragment] subclass.
 * Use the [MeetingBaseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
open class MeetingBaseFragment : BaseFragment() {

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment BlankFragment.
         */
        @JvmStatic
        fun newInstance() =
            MeetingBaseFragment()
    }

    /**
     * Process when it switch to offline
     *
     * @param offLine true if off line mode, false if on line mode
     */
    fun processOfflineMode(offLine: Boolean){
        logDebug("processOfflineMode:$offLine")
    }

}