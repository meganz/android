package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.viewbinding.ViewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_ringing_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MeetingRingingFragmentBinding
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.permission.PermissionRequest
import mega.privacy.android.app.utils.permission.permissionsBuilder
import java.util.*


@AndroidEntryPoint
class RingingMeetingFragment : MeetingBaseFragment() {

    private val viewModel: RingingMeetingViewModel by viewModels()

    private lateinit var binding: ViewBinding

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
        initViewModel()
        initComponent()
    }

    /**
     * Initialize components of UI
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initComponent() {
        answer_video_fab.startAnimation(AnimationUtils.loadAnimation(meetingActivity, R.anim.shake))

        animationAlphaArrows(fourth_arrow_call)
        runDelay(250) {
            animationAlphaArrows(third_arrow_call)
            runDelay(250) {
                animationAlphaArrows(second_arrow_call)
                runDelay(250) {
                    animationAlphaArrows(first_arrow_call)
                }
            }
        }
    }

    private fun animationAlphaArrows(arrow: ImageView) {
        logDebug("animationAlphaArrows")
        val alphaAnimArrows = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 1000
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
    }

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
     * Process when the user denies the permissions
     */
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

    private fun onShowRationale(request: PermissionRequest) {
        request.proceed()
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
}