package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.android.synthetic.main.activity_meeting.*
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.databinding.MeetingOnBoardingFragmentBinding
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.listeners.MeetingVideoListener
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE


/**
 * The abstract class of Join/JoinAsGuest/Create Meeting Fragments
 * These 3 fragments have common major UI elements as well as UI behaviors.
 * E.g. Turn on/off mic/camera/speaker buttons, self video preview,
 * click the big bottom button to move forward, etc.
 */
abstract class AbstractMeetingOnBoardingFragment : MeetingBaseFragment() {

    protected lateinit var binding: MeetingOnBoardingFragmentBinding
    private var videoListener: MeetingVideoListener? = null

    protected var meetingName = ""
    protected var chatId: Long = MEGACHAT_INVALID_HANDLE
    protected var meetingLink = ""

    // Views
    lateinit var toolbar: MaterialToolbar

    var mRootViewHeight: Int = 0

    // Soft keyboard open and close listener
    private var keyboardLayoutListener: OnGlobalLayoutListener? = OnGlobalLayoutListener {
        val r = Rect()
        val decorView: View = requireActivity().window.decorView
        decorView.getWindowVisibleDisplayFrame(r)
        val visibleHeight = r.height()
        if (mRootViewHeight == 0) {
            // save height of root view
            mRootViewHeight = visibleHeight
            return@OnGlobalLayoutListener
        }
        if (mRootViewHeight == visibleHeight) {
            // set bottom margin to 40dp
            setMarginBottomOfMeetingButton(40f)
            return@OnGlobalLayoutListener
        }
        if (mRootViewHeight - visibleHeight > 200) {
            // layout changing (keyboard popup), set bottom margin to 10dp
            setMarginBottomOfMeetingButton(10f)
            return@OnGlobalLayoutListener
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Do not share the instance with other permission check process, because the callback functions are different.
        permissionsRequester = permissionsBuilder(permissions.toCollection(ArrayList()))
            .setOnPermissionDenied { l -> onPermissionDenied(l) }
            .setOnRequiresPermission { l -> onRequiresPermission(l) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onNeverAskAgain(l) }
            .setPermissionEducation { showPermissionsEducation() }
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding()
        initMetaData()
        return binding.root
    }

    private fun initMetaData() {
        arguments?.let { args ->
            args.getString(MeetingActivity.MEETING_NAME)?.let {
                meetingName = it
            }
            args.getString(MeetingActivity.MEETING_LINK)?.let {
                meetingLink = it
            }
            chatId = args.getLong(MeetingActivity.MEETING_CHAT_ID)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProfileAvatar()
        setMarginTopOfMeetingName(
            Util.getStatusBarHeight() + ChatUtil.getActionBarHeight(
                activity, activity?.resources
            ) + Util.dp2px(16f)
        )

        meetingActivity.toolbar?.apply {
            meetingActivity.title_toolbar?.text = meetingName
            meetingActivity.subtitle_toolbar?.text = meetingLink
        }
    }

    override fun onResume() {
        super.onResume()
        logDebug("addOnGlobalLayoutListener: keyboardLayoutListener")
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
    }

    override fun onPause() {
        super.onPause()
        logDebug("removeOnGlobalLayoutListener: keyboardLayoutListener")
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(keyboardLayoutListener)
    }

    private fun setMarginTopOfMeetingName(marginTop: Int) {
        val menuLayoutParams = type_meeting_edit_text.layoutParams as ViewGroup.MarginLayoutParams
        menuLayoutParams.setMargins(0, marginTop, 0, 0)
        type_meeting_edit_text.layoutParams = menuLayoutParams
    }

    protected fun setMarginBottomOfMeetingButton(marginBottom: Float) {
        logDebug("setMarginBottomOfMeetingButton: $marginBottom")
        val layoutParams = btn_start_join_meeting.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.bottomMargin = Util.dp2px(marginBottom)
        btn_start_join_meeting.layoutParams = layoutParams
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.sharedviewmodel = sharedModel

        initViewModel()

        permissionsRequester.launch(true)
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
                when (it) {
                    AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                        fab_speaker.isOn = true
                        fab_speaker.setOnIcon(R.drawable.ic_speaker_on)
                        fab_speaker_label.text =
                            StringResourcesUtils.getString(R.string.general_speaker)
                    }
                    AppRTCAudioManager.AudioDevice.EARPIECE -> {
                        fab_speaker.isOn = false
                        fab_speaker.setOnIcon(R.drawable.ic_speaker_off)
                        fab_speaker_label.text =
                            StringResourcesUtils.getString(R.string.general_speaker)
                    }
                    else -> {
                        fab_speaker.isOn = true
                        fab_speaker.setOnIcon(R.drawable.ic_headphone)
                        fab_speaker_label.text =
                            StringResourcesUtils.getString(R.string.general_headphone)
                    }
                }
            }
            model.tips.observe(viewLifecycleOwner) {
                showToast(fab_tip_location, it, Toast.LENGTH_SHORT)
            }
            model.notificationNetworkState.observe(viewLifecycleOwner) {
                logDebug("Network state changed, Online :$it")
            }
            model.cameraPermissionCheck.observe(viewLifecycleOwner) {
                if (it) {
                    permissionsRequester = permissionsBuilder(
                        arrayOf(Manifest.permission.CAMERA).toCollection(ArrayList())
                    )
                        .setOnRequiresPermission { l ->
                            run {
                                onRequiresCameraPermission(l)
                                // Continue expected action after granted
                                sharedModel.clickCamera(true)
                            }
                        }
                        .setOnShowRationale { l -> onShowRationale(l) }
                        .setOnNeverAskAgain { l -> onCameraNeverAskAgain(l) }
                        .build()
                    permissionsRequester.launch(false)
                }
            }
            model.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
                if (it) {
                    permissionsRequester = permissionsBuilder(
                        arrayOf(Manifest.permission.RECORD_AUDIO).toCollection(ArrayList())
                    )
                        .setOnRequiresPermission { l ->
                            run {
                                onRequiresAudioPermission(l)
                                // Continue expected action after granted
                                sharedModel.clickMic(true)
                            }
                        }
                        .setOnShowRationale { l -> onShowRationale(l) }
                        .setOnNeverAskAgain { l -> onAudioNeverAskAgain(l) }
                        .build()
                    permissionsRequester.launch(false)
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

    /**
     * Notify the client to manually open the permission in system setting, This only needed when bRequested is true
     */
    private fun showSnackBar() {
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
        sharedModel.avatarLiveData.observe(viewLifecycleOwner) {
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
        setViewEnable(fab_cam, false)
        when (bOn) {
            true -> {
                // Always try to start the video using the front camera
                mask.visibility = View.VISIBLE

                sharedModel.setChatVideoInDevice(null)
                // Hide avatar when camera open
                meeting_thumbnail.visibility = View.GONE
                activateVideo()
            }
            false -> {
                mask.visibility = View.GONE

                // Show avatar when camera close
                meeting_thumbnail.visibility = View.VISIBLE
                deactivateVideo()
            }
        }
    }

    /**
     * Method for activating the video.
     */
    private fun activateVideo() {
        if (localSurfaceView == null || localSurfaceView.visibility == View.VISIBLE) {
            logError("Error activating video")
            setViewEnable(fab_cam, true)
            return
        }
        if (videoListener == null) {
            videoListener = MeetingVideoListener(
                localSurfaceView,
                outMetrics,
                MEGACHAT_INVALID_HANDLE,
                false,
                true
            )

            sharedModel.addLocalVideo(MEGACHAT_INVALID_HANDLE, videoListener)
        } else {
            videoListener?.let {
                it.height = 0
                it.width = 0
            }
        }
        localSurfaceView.visibility = View.VISIBLE
        setViewEnable(fab_cam, true, bSync = false)
    }

    /**
     * Set the button state
     *
     * @param bEnable set the view to be enable or not
     * @param bSync execute synchronously or asynchronously
     */
    private fun setViewEnable(view: View, bEnable: Boolean, bSync: Boolean = true) {
        when {
            bEnable && bSync -> (view as OnOffFab).enable = true
            bEnable && !bSync -> {
                lifecycleScope.launch {
                    delay(1000L)
                    withContext(Dispatchers.Main) {
                        (view as OnOffFab).enable = true
                    }
                }
            }
            !bEnable && bSync -> (view as OnOffFab).enable = false
            !bEnable && !bSync -> {
                lifecycleScope.launch {
                    delay(1000L)
                    withContext(Dispatchers.Main) {
                        (view as OnOffFab).enable = false
                    }
                }
            }
        }
    }

    /**
     * Method for deactivating the video.
     */
    private fun deactivateVideo() {
        if (localSurfaceView == null || videoListener == null || localSurfaceView.visibility == View.GONE) {
            logError("Error deactivating video")
            setViewEnable(fab_cam, true)
            return
        }
        logDebug("Removing surface view")
        localSurfaceView.visibility = View.GONE
        removeChatVideoListener()
        setViewEnable(fab_cam, true)
    }

    /**
     * Method for removing the video listener.
     */
    private fun removeChatVideoListener() {
        if (videoListener == null) return
        logDebug("Removing remote video listener")
        sharedModel.removeLocalVideo(MEGACHAT_INVALID_HANDLE, videoListener)
        videoListener = null
    }
}