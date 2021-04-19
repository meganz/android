package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MeetingOnBoardingFragmentBinding
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiJava.*


/**
 * The abstract class of Join/JoinAsGuest/Create Meeting Fragments
 * These 3 fragments have common major UI elements as well as UI behaviors.
 * E.g. Turn on/off mic/camera/speaker buttons, self video preview,
 * click the big bottom button to move forward, etc.
 */
abstract class AbstractMeetingOnBoardingFragment : MeetingBaseFragment() {

    private val abstractMeetingOnBoardingViewModel: AbstractMeetingOnBoardingViewModel by viewModels()
    protected lateinit var binding: MeetingOnBoardingFragmentBinding
    private var videoListener: MeetingVideoListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProfileAvatar()

        (activity as AppCompatActivity).supportActionBar?.apply {
            title = arguments?.getString(MeetingActivity.MEETING_NAME)
            subtitle = arguments?.getString(MeetingActivity.MEETING_LINK)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.sharedviewmodel = sharedModel

        initViewModel()
        checkMeetingPermissions(permissions)
    }

    private fun initBinding() {
        binding = MeetingOnBoardingFragmentBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.btnStartJoinMeeting.setOnClickListener { onMeetingButtonClick() }
    }

    /**
     * Initialize ViewModel
     * Use ViewModel to manage UI-related data
     */
    private fun initViewModel() {
        sharedModel.let { model ->
            model.micLiveData.observe(viewLifecycleOwner) {
                fab_mic.isOn = it
            }
            model.cameraLiveData.observe(viewLifecycleOwner) {
                switchCamera(it)
            }
            model.speakerLiveData.observe(viewLifecycleOwner) {
                fab_speaker.isOn = it
            }
            model.tips.observe(viewLifecycleOwner) {
                showToast(fab_tip_location, it, Toast.LENGTH_SHORT)
            }
            model.cameraPermissionCheck.observe(viewLifecycleOwner) {
                if (it) {
                    checkMeetingPermissions(
                        arrayOf(Manifest.permission.CAMERA),
                    ) { showSnackbar() }
                }
            }
            model.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
                if (it) {
                    checkMeetingPermissions(
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                    ) { showSnackbar() }
                }
            }
        }
    }

    /**
     * Notify the client to manually open the permission in system setting, This only needed when bRequested is true
     */
    fun showSnackbar() {
        val warningText =
            StringResourcesUtils.getString(R.string.meeting_required_permissions_warning)
        (activity as BaseActivity).showSnackbar(
            Constants.PERMISSIONS_TYPE,
            binding.root,
            warningText
        )
    }

    /**
     * Show tip when switching fabs, such as mic, camera, and speaker
     *
     * @param v Get location of tip
     * @param message The text to show
     * @param duration How long to display the message.
     */
    private fun showToast(v: View, message: String, duration: Int) {
        var xOffset = 0
        var yOffset = 0
        val gvr = Rect()
        if (v.getGlobalVisibleRect(gvr)) {
            val root = v.rootView
            val halfWidth = root.right / 2
            val halfHeight = root.bottom / 2
            val parentCenterX: Int = (gvr.right - gvr.left) / 2 + gvr.left
            val parentCenterY: Int = (gvr.bottom - gvr.top) / 2 + gvr.top
            yOffset = if (parentCenterY <= halfHeight) {
                -(halfHeight - parentCenterY)
            } else {
                parentCenterY - halfHeight
            }
            if (parentCenterX < halfWidth) {
                xOffset = -(halfWidth - parentCenterX)
            }
            if (parentCenterX >= halfWidth) {
                xOffset = parentCenterX - halfWidth
            }
        }
        val toast = Toast.makeText(requireContext(), message, duration)
        toast.setGravity(Gravity.CENTER, xOffset, yOffset)
        toast.show()
    }

    /**
     * Used by inherit subclasses
     * Create / Join / Join as Guest
     */
    abstract fun onMeetingButtonClick()

    /**
     * Get Avatar and display
     */
    fun setProfileAvatar() {
        logDebug("setProfileAvatar")
        abstractMeetingOnBoardingViewModel.avatar.observe(viewLifecycleOwner) {
            meeting_thumbnail.setImageBitmap(it)
        }
    }

    /**
     * Switch Camera
     *
     * @param bOn true: turn on; off: turn off
     */
    fun switchCamera(bOn: Boolean) {
        fab_cam.isOn = bOn

        // Always try to start the call using the front camera
        val frontCamera = VideoCaptureUtils.getFrontCamera()
        if (frontCamera != null) {
            when (bOn) {
                true -> {
                    megaChatApi.setChatVideoInDevice(frontCamera, null)
                    activateVideo()
                }
                false -> deactivateVideo()
            }
        }
    }

    /**
     * Method for activating the video.
     * TODO("Refactor code")
     */
    private fun activateVideo() {
        if (localSurfaceView == null || localSurfaceView.visibility == View.VISIBLE) {
            logError("Error activating video")
            return
        }
        if (videoListener == null) {
            videoListener = MeetingVideoListener(
                context,
                localSurfaceView,
                outMetrics,
                false
            )
            megaChatApi.addChatLocalVideoListener(
                MEGACHAT_INVALID_HANDLE,
                videoListener
            )
        } else {
            videoListener?.let {
                it.height = 0
                it.width = 0
            }
        }
        localSurfaceView.visibility = View.VISIBLE
    }

    /**
     * Method for deactivating the video.
     */
    private fun deactivateVideo() {
        if (localSurfaceView == null || videoListener == null || localSurfaceView.visibility == View.GONE) {
            logError("Error deactivating video")
            return
        }
        logDebug("Removing suface view")
        localSurfaceView.visibility = View.GONE
        removeChatVideoListener()
    }

    /**
     * Method for removing the video listener.
     */
    private fun removeChatVideoListener() {
        if (videoListener == null) return
        logDebug("Removing remote video listener")
        megaChatApi.removeChatVideoListener(
            MEGACHAT_INVALID_HANDLE,
            MEGACHAT_INVALID_HANDLE,
            false,
            videoListener
        )
        videoListener = null
    }
}