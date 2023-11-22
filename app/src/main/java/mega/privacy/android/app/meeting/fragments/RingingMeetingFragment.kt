package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.databinding.MeetingRingingFragmentBinding
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor
import mega.privacy.android.app.utils.CallUtil.getDefaultAvatarCall
import mega.privacy.android.app.utils.CallUtil.getImageAvatarCall
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR
import mega.privacy.android.app.utils.Constants.AVATAR_SIZE
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.permissionsBuilder
import mega.privacy.android.domain.entity.meeting.AnotherCallType
import mega.privacy.android.domain.entity.meeting.CallUIStatusType
import mega.privacy.android.domain.entity.meeting.SubtitleCallType
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
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

        sharedModel.answerCall(enableVideo, audio, video)
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
            inMeetingViewModel.setChatId(chatId, requireContext())
        }

        viewLifecycleOwner.collectFlow(inMeetingViewModel.state) { state: InMeetingUiState ->
            if (state.chatTitle.isNotEmpty()) {
                toolbarTitle.text = state.chatTitle
            }
        }

        var bitmap: Bitmap?

        // Set caller's name and avatar
        inMeetingViewModel.getChat()?.let {
            if (inMeetingViewModel.state.value.isOneToOneCall) {
                val callerId = it.getPeerHandle(0)

                bitmap = getImageAvatarCall(callerId)
                if (bitmap == null) {
                    bitmap = getDefaultAvatarCall(context, callerId)
                }
            } else {
                bitmap = getDefaultAvatar(
                    getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR),
                    getTitleChat(it),
                    AVATAR_SIZE,
                    true,
                    true
                )
            }

            binding.avatar.setImageBitmap(bitmap)
        }

        LiveEventBus.get(EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT, Long::class.java)
            .observe(this) {
                if (chatId == it) {
                    requireActivity().finish()
                }
            }

        // Caller cancelled the call.
        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall::class.java)
            .observeSticky(this) {
                if (it.status == MegaChatCall.CALL_STATUS_DESTROYED) {
                    requireActivity().finish()
                }
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