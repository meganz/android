package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.OnOffFab
import mega.privacy.android.app.databinding.MeetingOnBoardingFragmentBinding
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.listeners.IndividualCallVideoListener
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants.MEETING_BOTTOM_MARGIN
import mega.privacy.android.app.utils.Constants.MEETING_BOTTOM_MARGIN_WITH_KEYBOARD
import mega.privacy.android.app.utils.Constants.MEETING_NAME_MARGIN_TOP
import mega.privacy.android.app.utils.Constants.MIN_MEETING_HEIGHT_CHANGE
import mega.privacy.android.app.utils.Constants.PERMISSIONS_TYPE
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.permissionsBuilder
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber

/**
 * The abstract class of Join/JoinAsGuest/Create Meeting Fragments
 * These 3 fragments have common major UI elements as well as UI behaviors.
 * E.g. Turn on/off mic/camera/speaker buttons, self video preview,
 * click the big bottom button to move forward, etc.
 */

abstract class AbstractMeetingOnBoardingFragment : MeetingBaseFragment() {

    protected lateinit var binding: MeetingOnBoardingFragmentBinding

    private var videoListener: IndividualCallVideoListener? = null

    protected var meetingName = ""
    protected var chatId: Long = MEGACHAT_INVALID_HANDLE
    protected var publicChatHandle: Long = MEGACHAT_INVALID_HANDLE

    protected var meetingLink = ""

    var mRootViewHeight: Int = 0
    protected var toast: Toast? = null

    protected var bCameraOpen = false
    protected var bKeyBoardExtend = false
    private var preFabTop = 0

    // Soft keyboard open and close listener
    private var keyboardLayoutListener: OnGlobalLayoutListener? = OnGlobalLayoutListener {
        val r = Rect()
        val decorView: View = requireActivity().window.decorView
        decorView.getWindowVisibleDisplayFrame(r)
        val visibleHeight = r.height()

        val avatarRect = Rect()
        binding.meetingThumbnail.getGlobalVisibleRect(avatarRect)

        val fabRect = Rect()
        binding.onOffFab.fabCam.getGlobalVisibleRect(fabRect)

        if (mRootViewHeight == 0) {
            // save height of root view
            mRootViewHeight = visibleHeight
            return@OnGlobalLayoutListener
        }

        if (mRootViewHeight == visibleHeight) {
            // set bottom margin to 40dp
            setMarginBottomOfMeetingButton(MEETING_BOTTOM_MARGIN)
            bKeyBoardExtend = false
            triggerAvatar(View.VISIBLE)
            return@OnGlobalLayoutListener
        }

        if (mRootViewHeight - visibleHeight > MIN_MEETING_HEIGHT_CHANGE) {
            // layout changing (keyboard popup), set bottom margin to 10dp
            setMarginBottomOfMeetingButton(MEETING_BOTTOM_MARGIN_WITH_KEYBOARD)
            bKeyBoardExtend = true
            if ((preFabTop == fabRect.top) && (fabRect.top - avatarRect.bottom < 10)) {
                triggerAvatar(View.GONE)
            }
            preFabTop = fabRect.top
            return@OnGlobalLayoutListener
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Do not share the instance with other permission check process, because the callback functions are different.
        permissionsRequester = permissionsBuilder(permissions)
            .setOnPermissionDenied { l -> onPermissionDenied(l) }
            .setOnRequiresPermission { l -> onRequiresPermission(l) }
            .setOnShowRationale { l -> onShowRationale(l) }
            .setOnNeverAskAgain { l -> onNeverAskAgain(l) }
            .setPermissionEducation { showPermissionsEducation() }
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
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
            args.getLong(MeetingActivity.MEETING_CHAT_ID).let {
                chatId = it
                sharedModel.updateChatRoomId(chatId)
            }
            args.getLong(MeetingActivity.MEETING_PUBLIC_CHAT_HANDLE).let {
                publicChatHandle = it
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProfileAvatar()
        setMarginTopOfMeetingName(
            Util.getStatusBarHeight() + ChatUtil.getActionBarHeight(
                activity, activity?.resources
            ) + Util.dp2px(MEETING_NAME_MARGIN_TOP)
        )

        meetingActivity.binding.toolbar.apply {
            meetingActivity.binding.titleToolbar.text = meetingName
            meetingActivity.binding.subtitleToolbar.text = meetingLink
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("addOnGlobalLayoutListener: keyboardLayoutListener")
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardLayoutListener)
    }

    override fun onPause() {
        super.onPause()
        Timber.d("removeOnGlobalLayoutListener: keyboardLayoutListener")
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(keyboardLayoutListener)
    }

    private fun setMarginTopOfMeetingName(marginTop: Int) {
        val menuLayoutParams = binding.meetingInfo.layoutParams as ViewGroup.MarginLayoutParams
        menuLayoutParams.setMargins(0, marginTop, 0, 0)
        binding.meetingInfo.layoutParams = menuLayoutParams
    }

    private fun setMarginBottomOfMeetingButton(marginBottom: Float) {
        val layoutParams = binding.btnStartJoinMeeting.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.bottomMargin = Util.dp2px(marginBottom)
        binding.btnStartJoinMeeting.layoutParams = layoutParams
    }

    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.sharedviewmodel = sharedModel

        initViewModel()

        permissionsRequester.launch(true)
    }

    private fun initBinding() {
        binding = MeetingOnBoardingFragmentBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.btnStartJoinMeeting.setOnClickListener {
            permissionsRequester = permissionsBuilder(
                arrayOf(Manifest.permission.RECORD_AUDIO)
            )
                .setOnPermissionDenied { l -> onPermissionDenied(l) }
                .setOnRequiresPermission { l ->
                    run {
                        toast?.cancel()
                        onRequiresPermission(l)
                        onMeetingButtonClick()
                    }
                }
                .setOnShowRationale { l -> onShowRationale(l) }
                .setOnNeverAskAgain { l ->
                    run {
                        onNeverAskAgain(l)
                        showSnackBar()
                    }
                }
                .build()
            permissionsRequester.launch(false)
        }
    }

    /**
     * Initialize ViewModel
     * Use ViewModel to manage UI-related data
     */
    private fun initViewModel() {
        sharedModel.let { model ->
            model.apply {
                micLiveData.observe(viewLifecycleOwner) {
                    binding.onOffFab.fabMic.isOn = it
                }
                cameraLiveData.observe(viewLifecycleOwner) {
                    switchCamera(it)
                }
                speakerLiveData.observe(viewLifecycleOwner) {
                    when (it) {
                        AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                            binding.onOffFab.fabSpeaker.enable = true
                            binding.onOffFab.fabSpeaker.isOn = true
                            binding.onOffFab.fabSpeaker.setOnIcon(R.drawable.ic_speaker_on)
                            binding.onOffFab.fabSpeakerLabel.text =
                                StringResourcesUtils.getString(R.string.general_speaker)
                        }
                        AppRTCAudioManager.AudioDevice.EARPIECE -> {
                            binding.onOffFab.fabSpeaker.enable = true
                            binding.onOffFab.fabSpeaker.isOn = false
                            binding.onOffFab.fabSpeaker.setOnIcon(R.drawable.ic_speaker_off)
                            binding.onOffFab.fabSpeakerLabel.text =
                                StringResourcesUtils.getString(R.string.general_speaker)
                        }
                        AppRTCAudioManager.AudioDevice.WIRED_HEADSET,
                        AppRTCAudioManager.AudioDevice.BLUETOOTH,
                        -> {
                            binding.onOffFab.fabSpeaker.enable = true
                            binding.onOffFab.fabSpeaker.isOn = true
                            binding.onOffFab.fabSpeaker.setOnIcon(R.drawable.ic_headphone)
                            binding.onOffFab.fabSpeakerLabel.text =
                                StringResourcesUtils.getString(R.string.general_headphone)
                        }
                        else -> {
                            binding.onOffFab.fabSpeaker.enable = false
                            binding.onOffFab.fabSpeaker.isOn = true
                            binding.onOffFab.fabSpeaker.setOnIcon(R.drawable.ic_speaker_on)
                            binding.onOffFab.fabSpeakerLabel.text =
                                StringResourcesUtils.getString(R.string.general_speaker)
                        }
                    }
                }
                tips.observe(viewLifecycleOwner) {
                    showToast(binding.fabTipLocation, it, Toast.LENGTH_SHORT)
                }
                notificationNetworkState.observe(viewLifecycleOwner) {
                    Timber.d("Network state changed, Online :$it")
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
        }
    }

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
            showSnackBar()
        }
    }

    /**
     * Notify the client to manually open the permission in system setting.
     */
    protected fun showSnackBar() {
        val warningText =
            StringResourcesUtils.getString(R.string.meeting_required_permissions_warning)
        (activity as BaseActivity).showSnackbar(
            PERMISSIONS_TYPE,
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
    @SuppressLint("ShowToast")
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

        toast?.cancel()
        toast = Toast.makeText(requireContext(), message, duration)
        toast?.let {
            it.setGravity(Gravity.CENTER, xOffset, yOffset)
            it.show()
        }
    }

    /**
     * Used by inherit subclasses
     * Create / Join / Join as Guest
     */
    abstract fun onMeetingButtonClick()

    /**
     * Get Avatar and display
     */
    open fun setProfileAvatar() {
        Timber.d("setProfileAvatar")
        sharedModel.avatarLiveData.observe(viewLifecycleOwner) {
            binding.meetingThumbnail.setImageBitmap(it)
        }
    }

    /**
     * Switch Camera
     *
     * @param shouldVideoBeEnabled True, If the video is to be enabled. False, otherwise
     */
    fun switchCamera(shouldVideoBeEnabled: Boolean) {
        binding.onOffFab.fabCam.isOn = shouldVideoBeEnabled
        setViewEnable(binding.onOffFab.fabCam, false)

        if (shouldVideoBeEnabled) {
            // Always try to start the video using the front camera
            binding.mask.visibility = View.VISIBLE
            bCameraOpen = true
            sharedModel.setChatVideoInDevice(null)

            // Hide avatar when camera open
            triggerAvatar(View.GONE)
            activateVideo()
        } else {
            binding.mask.visibility = View.GONE
            bCameraOpen = false

            // Show avatar when camera close
            triggerAvatar(View.VISIBLE)
            deactivateVideo()
        }
    }

    /**
     * Show or hide Avatar according to camera and keyboard
     * 1 - Canera open - hide Avatar
     * 2 - Camera close and KeyBoard hide - show Avatar
     * 3 - Camera close and KeyBoard show - visibility
     *
     * @param visibility View.GONE / View.VISIBLE / View.INVISIBLE
     */
    private fun triggerAvatar(visibility: Int) {
        Timber.d("triggerAvatar bCameraOpen: $bCameraOpen & bKeyBoardExtend: $bKeyBoardExtend")
        if (bCameraOpen) {
            if (binding.meetingThumbnail.visibility == View.GONE)
                return

            binding.meetingThumbnail.visibility = View.GONE
        } else if (!bKeyBoardExtend) {
            if (binding.meetingThumbnail.visibility == View.VISIBLE)
                return

            binding.meetingThumbnail.visibility = View.VISIBLE
        } else {
            if (binding.meetingThumbnail.visibility == visibility)
                return

            binding.meetingThumbnail.visibility = visibility
        }
    }

    /**
     * Method for activating the video.
     */
    private fun activateVideo() {
        if (binding.localSurfaceView.visibility == View.VISIBLE) {
            Timber.e("Error activating video")
            setViewEnable(binding.onOffFab.fabCam, true)
            return
        }

        if (videoListener == null) {
            videoListener = IndividualCallVideoListener(
                binding.localSurfaceView,
                resources.displayMetrics,
                MEGACHAT_INVALID_HANDLE,
                isFloatingWindow = false,
                isOneToOneCall = true
            )

            sharedModel.addLocalVideo(MEGACHAT_INVALID_HANDLE, videoListener)
        } else {
            videoListener?.let {
                it.height = 0
                it.width = 0
            }
        }

        binding.localSurfaceView.visibility = View.VISIBLE
        setViewEnable(binding.onOffFab.fabCam, true, bSync = false)
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
        if (videoListener == null || binding.localSurfaceView.visibility == View.GONE) {
            Timber.e("Error deactivating video")
            setViewEnable(binding.onOffFab.fabCam, true)
            return
        }

        Timber.d("Removing surface view")
        binding.localSurfaceView.visibility = View.GONE
        removeChatVideoListener()
        setViewEnable(binding.onOffFab.fabCam, true)
    }

    /**
     * Method for removing the video listener.
     */
    private fun removeChatVideoListener() {
        if (videoListener == null) return

        Timber.d("Removing remote video listener")
        sharedModel.removeLocalVideo(MEGACHAT_INVALID_HANDLE, videoListener)
        videoListener = null
    }

    /**
     * Method for release the video device and removing the video listener.
     */
    fun releaseVideoDeviceAndRemoveChatVideoListener() {
        if (sharedModel.cameraLiveData.value == true) {
            sharedModel.releaseVideoDevice()
            removeChatVideoListener()
        }
    }

    /**
     * Method to create the RTC Audio Manager
     */
    fun initRTCAudioManager() {
        sharedModel.initRTCAudioManager()
    }
}