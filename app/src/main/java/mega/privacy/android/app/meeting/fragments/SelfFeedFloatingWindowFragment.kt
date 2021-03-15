package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.app.utils.Constants.CLIENT_ID
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaChatRoom


class SelfFeedFloatingWindowFragment : BaseFragment() {

    private var chatId: Long? = null
    private var clientId: Long? = null
    private var chat: MegaChatRoom? = null

    // View Binding
    private var _binding: SelfFeedFloatingWindowFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SelfFeedFloatingWindowViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID)
            clientId = it.getLong(Constants.CLIENT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = SelfFeedFloatingWindowFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logDebug("************** onViewCreated")
        setupClickListeners()
        fragmentTextUpdateObserver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Setup the button in our fragment to call getUpdatedText method in viewModel
    private fun setupClickListeners() {
        //binding.fragmentButton.setOnClickListener { viewModel.getUpdatedText() }
    }

    // Observer is waiting for viewModel to update our UI
    private fun fragmentTextUpdateObserver() {
//        viewModel.uiTextLiveData.observe(viewLifecycleOwner, Observer { updatedText ->
//            binding.fragmentTextView.text = updatedText
//        })
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        viewModel = ViewModelProvider(this).get(SelfFeedFloatingWindowViewModel::class.java)
//    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Parameter 1.
         * @param clientId Parameter 2.
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance(chatId: Long, clientId: Long) =
            SelfFeedFloatingWindowFragment().apply {
                arguments = Bundle().apply {
                    putLong(CHAT_ID, chatId)
                    putLong(CLIENT_ID, clientId)
                }
            }
    }

}