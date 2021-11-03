package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ANSWERED_IN_ANOTHER_CLIENT
import mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE
import mega.privacy.android.app.databinding.MeetingRingingFragmentBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.meeting.MeetingPermissionCallbacks
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_OFF
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING_VIDEO_ON
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.listeners.AnswerChatCallListener
import mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar
import mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor
import mega.privacy.android.app.utils.CallUtil.getDefaultAvatarCall
import mega.privacy.android.app.utils.CallUtil.getImageAvatarCall
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.permission.*
import mega.privacy.android.app.utils.permission.PermissionUtils.onNeverAskAgain
import mega.privacy.android.app.utils.permission.PermissionUtils.onPermissionDenied
import mega.privacy.android.app.utils.permission.PermissionUtils.onRequiresPermission
import mega.privacy.android.app.utils.permission.PermissionUtils.onShowRationale
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import java.util.*


@AndroidEntryPoint
class RingingMeetingFragment : BaseFragment(),
    AnswerChatCallListener.OnCallAnsweredCallback {

    private val sharedModel: MeetingActivityViewModel by activityViewModels()
    private val inMeetingViewModel by viewModels<InMeetingViewModel>()

    private lateinit var binding: MeetingRingingFragmentBinding

    private lateinit var toolbarTitle: EmojiTextView
    private lateinit var toolbarSubtitle: TextView

    private var chatId: Long = MEGACHAT_INVALID_HANDLE

    private lateinit var permissionsRequester: PermissionsRequester

    // Default permission array for meeting
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    lateinit var meetingPermissionCallbacks: MeetingPermissionCallbacks

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewModel()
        permissionsRequester.launch(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            chatId = it.getLong(MEETING_CHAT_ID)
        }
        meetingPermissionCallbacks = MeetingPermissionCallbacks(sharedModel)
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
        inMeetingViewModel.getCall()?.let {
            inMeetingViewModel.answerChatCall(
                enableVideo,
                true,
                AnswerChatCallListener(requireContext(), this)
            )
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
                    getSpecificAvatarColor(Constants.AVATAR_GROUP_CHAT_COLOR),
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

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.CAMERA).toCollection(
                        ArrayList()
                    )
                )
                    .setOnRequiresPermission { l -> onRequiresPermission(l, meetingPermissionCallbacks) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onPermNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.RECORD_AUDIO).toCollection(
                        ArrayList()
                    )
                )
                    .setOnRequiresPermission { l -> onRequiresPermission(l, meetingPermissionCallbacks) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onPermNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsRequester = permissionsBuilder(permissions.toCollection(ArrayList()))
            .setOnPermissionDenied { l -> onPermissionDenied(l, meetingPermissionCallbacks) }
            .setOnRequiresPermission { l -> onRequiresPermission(l, meetingPermissionCallbacks) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onNeverAskAgain(l, meetingPermissionCallbacks) }
            .setPermissionEducation { showPermissionsEducation() }
            .build()
    }

    override fun onResume() {
        super.onResume()
        // Use A New Instance to Check Permissions
        // Do not share the instance with other permission check process, because the callback functions are different.
        permissionsBuilder(permissions.toCollection(ArrayList()))
            .setPermissionRequestType(PermissionType.CheckPermission)
            .setOnRequiresPermission { l ->
                onRequiresPermission(l, meetingPermissionCallbacks)
            }.setOnPermissionDenied { l ->
                onPermissionDenied(l, meetingPermissionCallbacks)
            }.build().launch(false)
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

    override fun onCallAnswered(chatId: Long, flag: Boolean) {
        val actionString = if (flag) {
            logDebug("Call answered with video ON and audio ON")
            MEETING_ACTION_RINGING_VIDEO_ON
        } else {
            logDebug("Call answered with video OFF and audio ON")
            MEETING_ACTION_RINGING_VIDEO_OFF
        }

        val action = RingingMeetingFragmentDirections.actionGlobalInMeeting(
            actionString,
            chatId
        )
        findNavController().navigate(action)
    }

    override fun onErrorAnsweredCall(errorCode: Int) {
        logDebug("Error answering the call")
        inMeetingViewModel.removeIncomingCallNotification(chatId)
        requireActivity().finish()
    }

    /**
     * Check the condition of display of permission education dialog
     * Then continue permission check without education dialog
     */
    private fun showPermissionsEducation() {
        val sp = app.getSharedPreferences(MEETINGS_PREFERENCE, Context.MODE_PRIVATE)
        val showEducation = sp.getBoolean(KEY_SHOW_EDUCATION, true)
        if (showEducation) {
            sp.edit()
                .putBoolean(KEY_SHOW_EDUCATION, false).apply()
            showPermissionsEducation(requireActivity()) { permissionsRequester.launch(false) }
        } else {
            permissionsRequester.launch(false)
        }
    }

    /**
     * Shows a permission education.
     * It will be displayed at the beginning of meeting activity.
     *
     * @param context current Context.
     * @param checkPermission a callback for check permissions
     */
    private fun showPermissionsEducation(context: Context, checkPermission: () -> Unit) {

        val permissionsWarningDialogBuilder =
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        permissionsWarningDialogBuilder.setTitle(StringResourcesUtils.getString(R.string.meeting_permission_info))
            .setMessage(StringResourcesUtils.getString(R.string.meeting_permission_info_message))
            .setCancelable(false)
            .setPositiveButton(StringResourcesUtils.getString(R.string.button_permission_info)) { dialog, _ ->
                run {
                    dialog.dismiss()
                    checkPermission()
                }
            }

        permissionsWarningDialogBuilder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        logDebug("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var i = 0
        while (i < grantResults.size) {
            val bPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(bPermission)
                }
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(bPermission)
                }
            }
            i++
        }
    }
}