package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.databinding.MeetingRingingFragmentBinding
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor
import mega.privacy.android.app.utils.CallUtil.getDefaultAvatarCall
import mega.privacy.android.app.utils.CallUtil.getImageAvatarCall
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR
import mega.privacy.android.app.utils.Constants.AVATAR_SIZE
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.permissionsBuilder
import mega.privacy.android.domain.entity.call.ChatCallStatus
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber

@AndroidEntryPoint
class RingingMeetingFragment : MeetingBaseFragment() {

    private val inMeetingViewModel by viewModels<InMeetingViewModel>()

    private lateinit var binding: MeetingRingingFragmentBinding

    private lateinit var toolbarTitle: EmojiTextView
    private lateinit var toolbarSubtitle: TextView

    private var chatId: Long = MEGACHAT_INVALID_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            chatId = it.getLong(MEETING_CHAT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        (requireActivity() as MeetingActivity).let {
            toolbarTitle = it.binding.titleToolbar
            toolbarSubtitle = it.binding.subtitleToolbar
        }

        binding = MeetingRingingFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        permissionsRequester.launch(true)
        initComponent()
    }

    /**
     * Initialize components of UI
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initComponent() {

        // Always be 'calling'.
        toolbarSubtitle.text = getString(R.string.outgoing_call_starting)

        binding.answerVideoFab.setOnClickListener {
            inMeetingViewModel.checkAnotherCallsInProgress(inMeetingViewModel.getChatId())
            answerCall(enableVideo = true)
        }

        binding.answerAudioFab.setOnClickListener {
            inMeetingViewModel.checkAnotherCallsInProgress(inMeetingViewModel.getChatId())
            answerCall(enableVideo = false)
        }

        binding.rejectFab.setOnClickListener {
            inMeetingViewModel.onRejectBottomTap(chatId)
            requireActivity().finish()
        }
    }

    /**
     * Method to answer the call with audio enabled
     *
     * @param enableVideo True, if it should be answered with video on. False, if it should be answered with video off
     */
    private fun answerCall(enableVideo: Boolean) {
        val audio =
            PermissionUtils.hasPermissions(requireContext(), Manifest.permission.RECORD_AUDIO)

        if (!audio) {
            showSnackBar(getString(R.string.meeting_required_permissions_warning))
            return
        }
        var video = enableVideo
        if (video) {
            video = PermissionUtils.hasPermissions(requireContext(), Manifest.permission.CAMERA)
        }

        sharedModel.answerCall(enableVideo = enableVideo, enableAudio = true, speakerAudio = video)
            .observe(viewLifecycleOwner) { (chatHandle, enableVideo, _) ->
                val actionString = if (enableVideo) {
                    Timber.d("Call answered with video ON and audio ON")
                    MEETING_ACTION_RINGING_VIDEO_ON
                } else {
                    Timber.d("Call answered with video OFF and audio ON")
                    MEETING_ACTION_RINGING_VIDEO_OFF
                }

                val action = RingingMeetingFragmentDirections.actionGlobalInMeeting(
                    actionString,
                    chatHandle
                )
                findNavController().navigate(action)
            }
    }

    /**
     * Initialize ViewModel
     */
    private fun initViewModel() {
        if (chatId != MEGACHAT_INVALID_HANDLE) {
            sharedModel.updateChatRoomId(chatId)
            inMeetingViewModel.setChatId(chatId)
        }
        collectFlows()

        var bitmap: Bitmap?

        // Set caller's name and avatar
        inMeetingViewModel.state.value.chat?.let {
            if (inMeetingViewModel.isOneToOneCall()) {
                val callerId = it.peerHandlesList[0]

                bitmap = getImageAvatarCall(callerId)
                if (bitmap == null) {
                    bitmap = getDefaultAvatarCall(context, callerId)
                }
            } else {
                bitmap = getDefaultAvatar(
                    getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR),
                    it.title,
                    AVATAR_SIZE,
                    true,
                    true
                )
            }

            binding.avatar.setImageBitmap(bitmap)
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            if (allowed) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.CAMERA)
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresPermission(l)
                            // Continue expected action after granted
                            sharedModel.clickCamera(true)
                        }
                    }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onPermNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            if (allowed) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.RECORD_AUDIO)
                )
                    .setOnRequiresPermission { l ->
                        run {
                            onRequiresPermission(l)
                            // Continue expected action after granted
                            sharedModel.clickMic(true)
                        }
                    }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onPermNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }
    }

    private fun collectFlows() {
        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.chatTitle }
            .distinctUntilChanged()) {
            if (it.isNotBlank()) {
                toolbarTitle.text = it
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.callAnsweredInAnotherClient }
            .distinctUntilChanged()) {
            if (it) {
                requireActivity().finish()
            }
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state.map { it.call?.status }
            .distinctUntilChanged()) {
            it?.let { callStatus ->
                if (callStatus == ChatCallStatus.Destroyed) {
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsRequester = permissionsBuilder(permissions)
            .setOnPermissionDenied { l -> onPermissionDenied(l) }
            .setOnRequiresPermission { l -> onRequiresPermission(l) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onNeverAskAgain(l) }
            .setPermissionEducation { showPermissionsEducation() }
            .build()
    }

    private fun showSnackBar(message: String) =
        (activity as BaseActivity).showSnackbar(binding.root, message)

    /**
     * user denies the RECORD_AUDIO or CAMERA permission
     *
     * @param permissions permission list
     */
    private fun onPermNeverAskAgain(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)
            || permissions.contains(Manifest.permission.CAMERA)
        ) {
            Timber.d("user denies the permission")
            showSnackBar(getString(R.string.meeting_required_permissions_warning))
        }
    }

    override fun onDestroy() {
        RunOnUIThreadUtils.stop()
        super.onDestroy()
    }
}
