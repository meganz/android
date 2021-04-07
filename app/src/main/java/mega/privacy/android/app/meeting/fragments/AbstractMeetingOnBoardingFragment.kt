package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.databinding.MeetingOnBoardingFragmentBinding
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.Util


abstract class AbstractMeetingOnBoardingFragment : MeetingBaseFragment() {

    protected val abstractMeetingOnBoardingViewModel: AbstractMeetingOnBoardingViewModel by viewModels()
    protected lateinit var binding: MeetingOnBoardingFragmentBinding
    var meetingName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding()
        initViewModel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProfileAvatar()
    }

    /**
     * Bind layout views to viewmodel
     */
    private fun initBinding() {
        binding = MeetingOnBoardingFragmentBinding.inflate(layoutInflater)
        binding.viewmodel = abstractMeetingOnBoardingViewModel
        binding.lifecycleOwner = this
        binding.btnStartJoinMeeting.setOnClickListener { onMeetingButtonClick() }
    }

    /**
     * Initialize viewmodel
     * Use ViewModel to manage UI-related data
     */
    private fun initViewModel() {
        abstractMeetingOnBoardingViewModel.micLiveData.observe(viewLifecycleOwner) {
            switchMic(it)
        }
        abstractMeetingOnBoardingViewModel.cameraLiveData.observe(viewLifecycleOwner) {
            switchCamera(it)
        }
        abstractMeetingOnBoardingViewModel.speakerLiveData.observe(viewLifecycleOwner) {
            switchSpeaker(it)
        }
    }

    /**
     * Response to meeting button's 'onClick' event
     * Dispatch to current sub fragment
     */
    private fun onMeetingButtonClick() {
        meetingName = abstractMeetingOnBoardingViewModel.meetingName.value
        meetingName?.let {
            Util.hideKeyboardView(type_meeting_edit_text.context, type_meeting_edit_text, 0)
            meetingButtonClick()
        }
    }

    /**
     * Show tip when switching fabs, such as mic, camera, and speaker
     *
     * @param v Get location of tip
     * @param message The text to show
     * @param duration How long to display the message.
     */
    fun showToast(v: View, message: String, duration: Int) {
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
    private fun setProfileAvatar() {
        logDebug("setProfileAvatar")
        abstractMeetingOnBoardingViewModel.avatar.observe(viewLifecycleOwner) {
            meeting_thumbnail.setImageBitmap(it)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        logDebug("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) return
        when (requestCode) {
            Constants.REQUEST_CAMERA, Constants.REQUEST_RECORD_AUDIO -> {
                logDebug("REQUEST_CAMERA || RECORD_AUDIO")
                if (checkPermissionsCamera()) {
                    switchCamera(true)
                }
            }
        }
    }

    /**
     * Determine whether you have been granted a particular permission
     *
     * @param permission The name of the permission being checked.
     * @param requestCode Application specific request code to match with a result
     *    reported to {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    fun checkPermissions(permission: String, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val hasPermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            requestPermissions(arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    /**
     * Check Permissions of Camera
     *
     * @return whether you have been granted a particular permission
     */
    fun checkPermissionsCamera(): Boolean {
        logDebug("checkPermissionsCamera")
        return (checkPermissions(
            Manifest.permission.CAMERA,
            Constants.REQUEST_CAMERA
        ))
    }

    /**
     * Check Permissions of Audio
     *
     * @return whether you have been granted a particular permission
     */
    fun checkPermissionsAudio(): Boolean {
        logDebug("checkPermissionsCall")
        return (checkPermissions(
            Manifest.permission.RECORD_AUDIO,
            Constants.REQUEST_RECORD_AUDIO
        ))
    }

    /**
     * Switch Camera
     *
     * @param bOn true: turn on; off: turn off
     */
    fun switchCamera(bOn: Boolean) {
        fab_cam.isOn = bOn
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
}