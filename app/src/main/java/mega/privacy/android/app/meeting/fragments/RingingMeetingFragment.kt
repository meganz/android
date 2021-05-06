package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_ringing_fragment.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MeetingRingingFragmentBinding
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
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

    private val viewModel by viewModels<InMeetingViewModel>()

    private lateinit var binding: MeetingRingingFragmentBinding

    private var chatId: Long? = MEGACHAT_INVALID_HANDLE

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewModel()
        permissionsRequester.launch(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chatId = arguments?.getLong(
            MEETING_CHAT_ID,
            MEGACHAT_INVALID_HANDLE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        answer_video_fab.startAnimation(
            AnimationUtils.loadAnimation(
                meetingActivity,
                R.anim.shake
            )
        )

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
        sharedModel.avatarLiveData.observe(viewLifecycleOwner) {
            avatar.setImageBitmap(it)
        }

        chatId?.let {
            if (it != MEGACHAT_INVALID_HANDLE) {
                sharedModel.updateChatRoomId(it)
                viewModel.setChatId(it)
            }
        }

        sharedModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsRequester = permissionsBuilder(arrayOf(Manifest.permission.CAMERA).toCollection(ArrayList()))
                    .setOnRequiresPermission { l -> onRequiresCameraPermission(l) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onCameraNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        sharedModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                permissionsRequester = permissionsBuilder(arrayOf(Manifest.permission.RECORD_AUDIO).toCollection(ArrayList()))
                    .setOnRequiresPermission { l -> onRequiresAudioPermission(l) }
                    .setOnShowRationale { l -> onShowRationale(l) }
                    .setOnNeverAskAgain { l -> onAudioNeverAskAgain(l) }
                    .build()
                permissionsRequester.launch(false)
            }
        }

        binding.inMeetingViewModel = viewModel
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

    private fun showSnackBar() {
        val warningText =
            StringResourcesUtils.getString(R.string.meeting_required_permissions_warning)
        (activity as BaseActivity).showSnackbar(
            Constants.PERMISSIONS_TYPE,
            binding.root,
            warningText
        )
    }

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
                    showSnackBar()
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
                    showSnackBar()
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