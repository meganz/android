package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.meeting_ringing_fragment.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnSwipeTouchListener
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.databinding.MeetingRingingFragmentBinding
import mega.privacy.android.app.meeting.AnimationTool.clearAnimationAndGone
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_ACTION_RINGING
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.listeners.AnswerChatCallListener
import mega.privacy.android.app.utils.CallUtil.getDefaultAvatarCall
import mega.privacy.android.app.utils.CallUtil.getImageAvatarCall
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.permission.PermissionRequest
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.*


@AndroidEntryPoint
class RingingMeetingFragment : MeetingBaseFragment() {

    private val inMeetingViewModel by viewModels<InMeetingViewModel>()

    private lateinit var binding: MeetingRingingFragmentBinding

    private lateinit var toolbarTitle: EmojiTextView
    private lateinit var toolbarSubtitle: TextView

    private var chatId: Long = MEGACHAT_INVALID_HANDLE

    private var peerId: Long? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewModel()
        permissionsRequester.launch(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            chatId = args.getLong(MEETING_CHAT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity() as MeetingActivity).let {
            toolbarTitle = it.title_toolbar
            toolbarSubtitle = it.subtitle_toolbar
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
    private fun initComponent() {

        // Always be 'calling'.
        toolbarSubtitle.text = StringResourcesUtils.getString(R.string.outgoing_call_starting)

        answer_video_fab.startAnimation(
            AnimationUtils.loadAnimation(
                meetingActivity,
                R.anim.shake
            )
        )

        answer_audio_fab.setOnClickListener {
            answerCall(enableVideo = false)
        }

        answer_video_fab.setOnTouchListener(object : OnSwipeTouchListener(meetingActivity) {

            override fun onSwipeTop() {
                answer_video_fab.clearAnimation()
                animationButtons()
            }
        })

        reject_fab.setOnClickListener {
            inMeetingViewModel.leaveMeeting()
            requireActivity().finish()
        }

        animationAlphaArrows(fourth_arrow_call)
        runDelay(ALPHA_ANIMATION_DELAY) {
            animationAlphaArrows(third_arrow_call)
            runDelay(ALPHA_ANIMATION_DELAY) {
                animationAlphaArrows(second_arrow_call)
                runDelay(ALPHA_ANIMATION_DELAY) {
                    animationAlphaArrows(first_arrow_call)
                }
            }
        }
    }

    private fun animationButtons() {
        val translateAnim = TranslateAnimation(0f, 0f, 0f, -380f).apply {
            duration = 500L
            fillAfter = true
            fillBefore = true
            repeatCount = 0
            setAnimationListener(object : Animation.AnimationListener {

                override fun onAnimationStart(animation: Animation) {
                    reject_fab.isEnabled = false
                }

                override fun onAnimationRepeat(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    video_label.isVisible = false
                    answer_video_fab.hide()

                    answerCall(enableVideo = true)
                }
            })
        }

        val alphaAnim = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 600L
            fillAfter = true
            fillBefore = true
            repeatCount = 0
        }

        //false means don't share interpolator
        val s = AnimationSet(false)
        s.addAnimation(translateAnim)
        s.addAnimation(alphaAnim)

        answer_video_fab.startAnimation(s)

        first_arrow_call.clearAnimationAndGone()
        second_arrow_call.clearAnimationAndGone()
        third_arrow_call.clearAnimationAndGone()
        fourth_arrow_call.clearAnimationAndGone()
    }

    private fun answerCall(
        enableVideo: Boolean,
        enableAudio: Boolean = true
    ) = inMeetingViewModel.answerChatCall(
        enableVideo,
        enableAudio,
        object : AnswerChatCallListener.OnCallAnsweredCallback {

            override fun onCallAnswered(chatId: Long, flag: Boolean) {
                // To in-meeting
                val action = RingingMeetingFragmentDirections.actionGlobalInMeeting(
                    MEETING_ACTION_RINGING,
                    chatId
                )
                findNavController().navigate(action)
            }

            override fun onErrorAnsweredCall(errorCode: Int) {
                showSnackBar("Answer call failed, error code: $errorCode")
            }
        })

    private fun animationAlphaArrows(arrow: ImageView) {
        logDebug("animationAlphaArrows")
        val alphaAnimArrows = AlphaAnimation(1.0f, 0.0f).apply {
            duration = ALPHA_ANIMATION_DURATION
            fillAfter = true
            fillBefore = true
            repeatCount = Animation.INFINITE
        }

        arrow.startAnimation(alphaAnimArrows)
    }

    /**
     * Initialize ViewModel
     */
    private fun initViewModel() {
        inMeetingViewModel.chatTitle.observe(viewLifecycleOwner) {
            toolbarTitle.text = it
        }

//        if (chatId != MEGACHAT_INVALID_HANDLE) {
//            sharedModel.updateChatRoomId(chatId)
//            inMeetingViewModel.setChatId(chatId)
//        }
//
//        inMeetingViewModel.getCall()?.let {
//            val session = getSessionIndividualCall(it)
//            peerId = session?.peerid
//            TL.log("peer id is: $peerId")
//        }

        inMeetingViewModel.getChat()?.let {
            var bitmap = getImageAvatarCall(it, peerId!!)
            if (bitmap == null) {
                bitmap = getDefaultAvatarCall(context, peerId!!)
            }

            avatar.setImageBitmap(bitmap)
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsRequester = permissionsBuilder(
                    arrayOf(Manifest.permission.CAMERA).toCollection(
                        ArrayList()
                    )
                )
                    .setOnRequiresPermission { l -> onRequiresCameraPermission(l) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onCameraNeverAskAgain(l) }
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
                    .setOnRequiresPermission { l -> onRequiresAudioPermission(l) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onAudioNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        binding.sharedViewModel = sharedModel
    }

    //TODO start: copy from AbstractMeetingOnBoardingFragment
    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsRequester = permissionsBuilder(permissions.toCollection(ArrayList()))
            .setOnPermissionDenied { l -> onPermissionDenied(l) }
            .setOnRequiresPermission { l -> onRequiresPermission(l) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onNeverAskAgain(l) }
            .setPermissionEducation { showPermissionsEducation() }
            .build()
    }

    private fun onRequiresPermission(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(true)
                }
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(true)
                }
            }
        }
    }

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

    private fun onNeverAskAgain(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(false)
                }
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(false)
                }
            }
        }
    }

    private fun onPermissionDenied(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(false)
                }
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(false)
                }
            }
        }
    }

    private fun showSnackBar(message: String) =
        (activity as BaseActivity).showSnackbar(
            Constants.PERMISSIONS_TYPE,
            binding.root,
            message
        )


    private fun onShowRationale(request: PermissionRequest) {
        request.proceed()
    }

    private fun onRequiresAudioPermission(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(true)
                }
            }
        }
    }

    private fun onAudioNeverAskAgain(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    showSnackBar(StringResourcesUtils.getString(R.string.meeting_required_permissions_warning))
                }
            }
        }
    }

    private fun onRequiresCameraPermission(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(true)
                }
            }
        }
    }

    private fun onCameraNeverAskAgain(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies the permissions: $it")
            when (it) {
                Manifest.permission.CAMERA -> {
                    showSnackBar(StringResourcesUtils.getString(R.string.meeting_required_permissions_warning))
                }
            }
        }
    }
    //TODO end

    companion object {

        private const val ALPHA_ANIMATION_DURATION = 1000L
        private const val ALPHA_ANIMATION_DELAY = 250L

    }
}