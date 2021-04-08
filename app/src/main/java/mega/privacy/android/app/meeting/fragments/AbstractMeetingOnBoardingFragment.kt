package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MeetingOnBoardingFragmentBinding
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.PermissionUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util


abstract class AbstractMeetingOnBoardingFragment : MeetingBaseFragment() {

    protected val abstractMeetingOnBoardingViewModel: AbstractMeetingOnBoardingViewModel by viewModels()
    protected lateinit var binding: MeetingOnBoardingFragmentBinding
    private var requestCode = 0

    // Default permission array for meeting
    val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

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
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = arguments?.getString(MeetingActivity.MEETING_NAME)
            subtitle = arguments?.getString(MeetingActivity.MEETING_LINK)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        checkMeetingPermissions(permissions, true)
    }

    /**
     * Bind layout views to ViewModel
     */
    private fun initBinding() {
        binding = MeetingOnBoardingFragmentBinding.inflate(layoutInflater)
        binding.viewmodel = abstractMeetingOnBoardingViewModel
        binding.lifecycleOwner = this
        binding.btnStartJoinMeeting.setOnClickListener { onMeetingButtonClick() }
    }

    /**
     * Initialize ViewModel
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
        abstractMeetingOnBoardingViewModel.tips.observe(viewLifecycleOwner) {
            showToast(fab_tip_location, it, Toast.LENGTH_SHORT)
        }
        abstractMeetingOnBoardingViewModel.cameraPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                checkMeetingPermissions(
                    arrayOf(Manifest.permission.CAMERA),
                    dialogShow = false,
                    sysSettingShow = true
                )
            }
        }
        abstractMeetingOnBoardingViewModel.recordAudioPermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                checkMeetingPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    dialogShow = false,
                    sysSettingShow = true
                )
            }
        }
        abstractMeetingOnBoardingViewModel.storagePermissionCheck.observe(viewLifecycleOwner) {
            if (it) {
                checkMeetingPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    dialogShow = false,
                    sysSettingShow = true
                )
            }
        }
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
     * Check all the permissions for meeting
     * 1. Check whether permission is granted
     * 2. Request permission
     * 3. Callback after requesting permission
     * 4. Determine whether the user denies permission is to check the don't ask again option, if checked, the client needs to manually open the permission
     *
     * @param permissions Array of permissions
     * @param dialogShow true: show the permission education dialog; false: don't show the permission education dialog
     * @param sysSettingShow Check if the user ticket 'Don't ask again' and deny a permission request, if so, direct to system setting of MEGA.
     *
     */
    private fun checkMeetingPermissions(
        permissions: Array<String>,
        dialogShow: Boolean,
        sysSettingShow: Boolean = false
    ) {

        val mPermissionList: MutableList<String> = ArrayList()
        requestCode = 0
        for (i in permissions.indices) {
            val bPermission = PermissionUtils.hasPermissions(requireContext(), permissions[i])
            val showRequestPermission =
                PermissionUtils.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    permissions[i]
                )
            if (!bPermission && sysSettingShow) {
                val showRequestPermission =
                    PermissionUtils.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        permissions[i]
                    )
                if (!showRequestPermission) {
                    // The user ticket 'Don't ask again' and deny a permission request.
                    logDebug("the user ticket 'Don't ask again' and deny a permission request.")
                    val warningText =
                        StringResourcesUtils.getString(R.string.meeting_required_permissions_warning)
                    (activity as BaseActivity).showSnackbar(
                        Constants.PERMISSIONS_TYPE,
                        binding.root,
                        warningText
                    )
                    return
                }
            }
            when (permissions[i]) {
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    abstractMeetingOnBoardingViewModel.setStoragePermission(bPermission)
                    if (!bPermission) {
                        requestCode += Constants.REQUEST_READ_WRITE_STORAGE
                    }
                }
                Manifest.permission.CAMERA -> {
                    abstractMeetingOnBoardingViewModel.setCameraPermission(bPermission)
                    if (!bPermission) {
                        requestCode += Constants.REQUEST_CAMERA
                    }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    abstractMeetingOnBoardingViewModel.setRecordAudioPermission(bPermission)
                    if (!bPermission) {
                        requestCode += Constants.REQUEST_RECORD_AUDIO
                    }
                }
            }
            if (!bPermission) {
                // If 'Don't ask again' is not selected, show the permission request dialog
                if (showRequestPermission) {
                    mPermissionList.add(permissions[i])
                }
            }
        }
        if (mPermissionList.isNotEmpty()) {
            if (dialogShow) {
                showPermissionsEducation(requireActivity())
            } else {
                // Some permissions are not granted
                val permissionsArr = mPermissionList.toTypedArray()
                requestPermissions(
                    permissionsArr,
                    requestCode
                )
            }
        }

    }

    /**
     * Shows a permission education.
     * It will be displayed at the beginning of meeting activity.
     *
     * @param context current Context.
     */
    private fun showPermissionsEducation(context: Context) {

        val permissionsWarningDialogBuilder =
            MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Mega_MaterialAlertDialog)

        permissionsWarningDialogBuilder.setTitle(StringResourcesUtils.getString(R.string.meeting_permission_info))
            .setMessage(StringResourcesUtils.getString(R.string.meeting_permission_info_message))
            .setCancelable(false)
            .setNegativeButton(StringResourcesUtils.getString(R.string.button_cancel)) { dialog, _ ->
                run {
                    dialog.dismiss()
                    requireActivity().finish()
                }
            }
            .setPositiveButton(StringResourcesUtils.getString(R.string.button_permission_info)) { dialog, _ ->
                run {
                    dialog.dismiss()
                    checkMeetingPermissions(permissions, false)
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
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    abstractMeetingOnBoardingViewModel.setStoragePermission(bPermission)
                }
                Manifest.permission.CAMERA -> {
                    abstractMeetingOnBoardingViewModel.setCameraPermission(bPermission)
                }
                Manifest.permission.RECORD_AUDIO -> {
                    abstractMeetingOnBoardingViewModel.setRecordAudioPermission(bPermission)
                }
            }
            if (!bPermission) {
                // Check if the user ticket 'Don't ask again' and deny a permission request.
                val showRequestPermission =
                    PermissionUtils.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        permissions[i]
                    )
                if (showRequestPermission) {
                    // Don't show permission dialog again when user select "DENY", Recheck it when using
                    //@Suppress("UNCHECKED_CAST")
                    //checkMeetingPermissions(permissions as Array<String>, false)
                    return
                } else {
                    // The user ticket 'Don't ask again' and deny a permission request.
                    logDebug("the user ticket 'Don't ask again' and deny a permission request.")
                }
            }
            i++
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