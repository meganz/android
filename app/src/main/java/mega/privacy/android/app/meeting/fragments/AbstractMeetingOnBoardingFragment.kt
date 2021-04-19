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
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MeetingOnBoardingFragmentBinding
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface


abstract class AbstractMeetingOnBoardingFragment : MeetingBaseFragment() {

    private var videoListener: MeetingVideoListener? = null
    private val abstractMeetingOnBoardingViewModel: AbstractMeetingOnBoardingViewModel by viewModels()
    protected lateinit var binding: MeetingOnBoardingFragmentBinding


    // Receive information about requests.
    val listener = object : MegaChatRequestListenerInterface {
        override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest?) {

        }

        override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) {

        }

        override fun onRequestFinish(
            api: MegaChatApiJava?,
            request: MegaChatRequest?,
            e: MegaChatError?
        ) {
            when (request?.type) {
                MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE -> {
                    val bOpen = request.flag
                    logDebug("open video: $bOpen")
                    if (request.chatHandle == MEGACHAT_INVALID_HANDLE) {
                        if (bOpen) {
                            fab_cam.isOn = true
                            activateVideo()
                        } else {
                            fab_cam.isOn = false
                            deactivateVideo()
                        }
                    }
                }
            }
        }

        override fun onRequestTemporaryError(
            api: MegaChatApiJava?,
            request: MegaChatRequest?,
            e: MegaChatError?
        ) {

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding()
        initComponents()
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
        sharedModel = ViewModelProvider(requireActivity()).get(MeetingActivityViewModel::class.java)
        binding.sharedviewmodel = sharedModel

        initViewModel()
        checkMeetingPermissions(permissions)
    }

    /**
     * Bind layout views to ViewModel
     */
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
        sharedModel?.let { model ->
            model.micLiveData.observe(viewLifecycleOwner) {
                switchMic(it)
            }
            model.cameraLiveData.observe(viewLifecycleOwner) {
                switchCamera(it)
            }
            model.speakerLiveData.observe(viewLifecycleOwner) {
                switchSpeaker(it)
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
            model.storagePermissionCheck.observe(viewLifecycleOwner) {
                if (it) {
                    checkMeetingPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    ) { showSnackbar() }
                }
            }
        }
    }

    /**
     * Initialize components of meeting
     */
    private fun initComponents() {
        // TODO("Set front camera")

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
     * Response to meeting button's 'onClick' event
     * Dispatch to current sub fragment
     */
    private fun onMeetingButtonClick() {
        meetingButtonClick()
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
    abstract fun meetingButtonClick()

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
        if (bOn) {
            megaChatApi.openVideoDevice(listener)
        } else {
            megaChatApi.releaseVideoDevice(listener)
        }
    }

    /**
     * Switch Speaker / Headphone
     *
     * @param bOn true: switch to speaker; false: switch to headphone
     */
    private fun switchSpeaker(bOn: Boolean) {
        fab_speaker.isOn = bOn
    }

    /**
     * Turn On / Off Mic
     *
     * @param bOn true: turn on; off: turn off
     */
    private fun switchMic(bOn: Boolean) {
        fab_mic.isOn = bOn
    }

    /**
     * Method for activating the video.
     * TODO("Refactor code")
     */
    private fun activateVideo() {
        if (localSurfaceView == null || localSurfaceView.visibility == View.VISIBLE) {
            LogUtil.logError("Error activating video")
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
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE,
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
     * TODO("Refactor code")
     */
    private fun deactivateVideo() {
        if (localSurfaceView == null || videoListener == null || localSurfaceView.visibility == View.GONE) {
            LogUtil.logError("Error deactivating video")
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