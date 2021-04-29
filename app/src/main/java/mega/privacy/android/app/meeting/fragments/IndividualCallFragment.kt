package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.individual_call_fragment.view.*
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.databinding.IndividualCallFragmentBinding
import mega.privacy.android.app.databinding.SelfFeedFloatingWindowFragmentBinding
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.CallUtil.getImageAvatarCall
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRoom

@AndroidEntryPoint
class IndividualCallFragment : MeetingBaseFragment() {

    private var chatId: Long? = null
    private var peerId: Long? = null
    private var clientId: Long? = null
    private var chat: MegaChatRoom? = null

    private var isFloatingWindow = false

    // Views
    private lateinit var vVideo: SurfaceView
    private lateinit var vAvatar: RoundedImageView
    private lateinit var vOnHold: ImageView

    private val abstractMeetingOnBoardingViewModel: AbstractMeetingOnBoardingViewModel by viewModels()

    private var videoListener: MeetingVideoListener? = null

    var videoAlpha = 255

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID)
            peerId = it.getLong(Constants.PEER_ID)
            clientId = it.getLong(Constants.CLIENT_ID)
            isFloatingWindow = it.getBoolean(Constants.IS_FLOATING_WINDOW)
        }

        chat = abstractMeetingOnBoardingViewModel.getChatRoom(chatId!!)
        if(chat == null || abstractMeetingOnBoardingViewModel.getChatCall(chatId!!) == null || peerId == MEGACHAT_INVALID_HANDLE){
            return
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = if (!isFloatingWindow) IndividualCallFragmentBinding.inflate(
            inflater,
            container,
            false
        ) else SelfFeedFloatingWindowFragmentBinding.inflate(
            inflater,
            container,
            false
        )

        binding.root.let {
            vVideo = it.video
            vAvatar = it.avatar
            vOnHold = it.on_hold_icon
        }

        return binding.root
    }

    private fun initialiseAvatar(){
        var avatar = getImageAvatarCall(sharedModel.chatRoomLiveData.value!!, peerId!!)
        if(avatar == null){
            avatar = CallUtil.getDefaultAvatarCall(context, peerId!!)
        }
        vAvatar.setImageBitmap(avatar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initialiseAvatar()
        initShareViewModel()

        if (isFloatingWindow) {
            (parentFragment as InMeetingFragment).bottomFloatingPanelViewHolder.propertyUpdaters.apply {
                add {
                    view.alpha = 1 - it
                }
                add {
                    videoAlpha = ((1 - it) * 255).toInt()
                }
            }
        }
    }

    /**
     * Method for activating the video.
     */
    fun activateVideo() {
        // Always try to start the call using the front camera
        vAvatar.visibility = View.GONE

//        abstractMeetingOnBoardingViewModel.setChatVideoInDevice(true, null)

        if (videoListener == null) {
            videoListener = MeetingVideoListener(
                vVideo,
                outMetrics,
                MEGACHAT_INVALID_HANDLE,
                isFloatingWindow
            )

            if (abstractMeetingOnBoardingViewModel.isMe(peerId!!)) {
                abstractMeetingOnBoardingViewModel.activateLocalVideo(
                    chatId!!, videoListener!!
                )
            } else {
                abstractMeetingOnBoardingViewModel.activateRemoteVideo(
                    chatId!!,
                    clientId!!,
                    true,
                    videoListener!!
                )
            }
        } else {
            videoListener?.let {
                it.height = 0
                it.width = 0
            }
        }

        vVideo.visibility = View.VISIBLE
    }

    fun closeVideo() {
        if (videoListener == null) {
            logError("Error deactivating video")
            return
        }
        logDebug("Removing surface view")
        vVideo.visibility = View.GONE

        removeChatVideoListener()

        checkOneToOneCallAudioCall()
    }

    /**
     * Method for updating the muted call bar on individual calls.
     */
    private fun checkOneToOneCallAudioCall() {
        if (!isFloatingWindow) {
            vAvatar.visibility = View.VISIBLE
            return
        }

        if (sharedModel.isOneToOneCall()) {
            vAvatar.visibility = View.GONE
        } else {
            vAvatar.visibility = View.VISIBLE
        }
    }

    private fun removeChatVideoListener() {
        if (videoListener == null) return

        logDebug("Removing remote video listener")

        if (abstractMeetingOnBoardingViewModel.isMe(peerId!!)) {
            abstractMeetingOnBoardingViewModel.closeLocalVideo(
                chatId!!, videoListener!!
            )
        } else {
            abstractMeetingOnBoardingViewModel.closeRemoteVideo(
                chatId!!,
                clientId!!,
                true,
                videoListener!!
            )
        }

        videoListener = null
    }

    companion object {

        const val TAG = "IndividualCallFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Chat ID
         * @param peerId peer ID
         * @param isFloatingWindow True, if it's floating window. False, otherwise.
         * @return A new instance of fragment MeetingFragment.
         */
        @JvmStatic
        fun newInstance(chatId: Long, peerId: Long, isFloatingWindow: Boolean) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.PEER_ID, peerId)
                    putBoolean(Constants.IS_FLOATING_WINDOW, isFloatingWindow)
                }
            }

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param chatId Chat ID
         * @param peerId peer ID
         * @param clientId client ID
         * @return A new instance of fragment MeetingFragment.
         */
        fun newInstance(chatId: Long, peerId: Long, clientId: Long) =
            IndividualCallFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                    putLong(Constants.PEER_ID, peerId)
                    putLong(Constants.CLIENT_ID, clientId)
                    putBoolean(Constants.IS_FLOATING_WINDOW, false)
                }
            }
    }

    /**
     * Init Share View Model
     */
    private fun initShareViewModel() {
        sharedModel.micLiveData.observe(viewLifecycleOwner) {
            logDebug("show banner SILENCIADA")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (vVideo.parent != null && vVideo.parent.parent != null) {
            logDebug("Removing suface view")
            (vVideo.parent as ViewGroup).removeView(vVideo)
        }

        removeChatVideoListener()
        vAvatar.setImageBitmap(null)
    }

}