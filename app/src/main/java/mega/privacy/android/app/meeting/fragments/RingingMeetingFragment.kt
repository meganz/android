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
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
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
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import java.util.*

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
        savedInstanceState: Bundle?
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
        toolbarSubtitle.text = StringResourcesUtils.getString(R.string.outgoing_call_starting)

        binding.answerVideoFab.setOnClickListener {
            inMeetingViewModel.checkAnotherCallsInProgress(inMeetingViewModel.currentChatId)
            answerCall(enableVideo = true)
        }

        binding.answerAudioFab.setOnClickListener {
            inMeetingViewModel.checkAnotherCallsInProgress(inMeetingViewModel.currentChatId)
            answerCall(enableVideo = false)
        }

        binding.rejectFab.setOnClickListener {
            inMeetingViewModel.removeIncomingCallNotification(chatId)

            if (inMeetingViewModel.isOneToOneCall()) {
                inMeetingViewModel.leaveMeeting()
            } else {
                inMeetingViewModel.ignoreCall()
            }
            requireActivity().finish()
        }
    }

    /**
     * Method to answer the call with audio enabled
     *
     * @param enableVideo True, if it should be answered with video on. False, if it should be answered with video off
     */
    private fun answerCall(enableVideo: Boolean) {
        sharedModel.answerCall(enableVideo, true, enableVideo).observe(viewLifecycleOwner) {
            result ->
            val actionString = if (result.enableVideo) {
                logDebug("Call answered with video ON and audio ON")
                MEETING_ACTION_RINGING_VIDEO_ON
            } else {
                logDebug("Call answered with video OFF and audio ON")
                MEETING_ACTION_RINGING_VIDEO_OFF
            }

            val action = RingingMeetingFragmentDirections.actionGlobalInMeeting(
                actionString,
                result.chatHandle
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

        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) { title ->
            toolbarTitle.text = title
        }

        var bitmap: Bitmap?

        // Set caller's name and avatar
        inMeetingViewModel.getChat()?.let {
            if (inMeetingViewModel.isOneToOneCall()) {
                val callerId = it.getPeerHandle(0)

                bitmap = getImageAvatarCall(it, callerId)
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
                requirePermission(Manifest.permission.CAMERA)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) { allowed ->
            if (allowed) {
                requirePermission(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun requirePermission(permission: String) {
        permissionsBuilder(
            arrayOf(permission)
        )

            .setOnRequiresPermission { l -> onRequiresPermission(l) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onPermNeverAskAgain(l) }
            .build()
            .launch(false)
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
            || permissions.contains(Manifest.permission.CAMERA)) {
            logDebug("user denies the permission")
            showSnackBar(StringResourcesUtils.getString(R.string.meeting_required_permissions_warning))
        }
    }

    override fun onDestroy() {
        RunOnUIThreadUtils.stop()
        super.onDestroy()
    }
}