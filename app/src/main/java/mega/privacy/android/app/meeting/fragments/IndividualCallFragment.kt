package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatRoom

class IndividualCallFragment : MeetingBaseFragment() {

    private var chatId: Long? = null
    private var clientId: Long? = null
    private var chat: MegaChatRoom? = null
    private var isFloatingWindow = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID)
            clientId = it.getLong(Constants.CLIENT_ID)
            isFloatingWindow = it.getBoolean(Constants.IS_FLOATING_WINDOW)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (isFloatingWindow)
            SelfFeedFloatingWindowFragmentBinding.inflate(inflater, container, false).root
        else
            IndividualCallFragmentBinding.inflate(inflater, container, false).root
    }

    companion object {

        const val TAG = "IndividualCallFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Parameter 1.
         * @param clientId Parameter 2.
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance(chatId: Long, clientId: Long, isFloatingWindow: Boolean) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.CLIENT_ID, clientId)
                    putBoolean(Constants.IS_FLOATING_WINDOW, isFloatingWindow)
                }
            }
    }
}