package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.fragment.app.activityViewModels
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.permission.*

/**
 * Base fragment for meeting fragment:
 * [AbstractMeetingOnBoardingFragment]
 * [CreateMeetingFragment],
 * [JoinMeetingAsGuestFragment],
 * [JoinMeetingFragment],
 * [InMeetingFragment],
 * [MakeModeratorFragment],
 * [RingingMeetingFragment],
 * [IndividualCallFragment],
 * [GridViewCallFragment],
 * [SpeakerViewCallFragment]
 * include some common functions: Permissions...
 * Use shareModel to share data between sub fragments
 */
open class MeetingBaseFragment : BaseFragment() {

    lateinit var meetingActivity: MeetingActivity

    protected lateinit var permissionsRequester: PermissionsRequester

    protected val sharedModel: MeetingActivityViewModel by activityViewModels()

    // Indicate if permission has been requested. After requested, we should check "shouldShowRequestPermissionRationaleSnackBar"
    private var bRequested = false
    private var bRefreshPermission = false
    protected var requestCode = 0

    // Default permission array for meeting
    protected val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        meetingActivity = activity as MeetingActivity
    }

    override fun onResume() {
        super.onResume()
        // Use A New Instance to Check Permissions
        // Do not share the instance with other permission check process, because the callback functions are different.
        permissionsBuilder(permissions)
            .setPermissionRequestType(PermissionType.CheckPermission)
            .setOnRequiresPermission { l ->
                onCheckRequiresPermission(l)
            }.setOnPermissionDenied { l ->
                onCheckPermissionDenied(l)
            }.build().launch(false)
    }

    /**
     * Callback function for granting permissions
     *
     * @param permissions permission list
     */
    private fun onCheckRequiresPermission(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user check the permissions: $it")
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
     * Callback function for denying permissions
     *
     * @param permissions permission list
     */
    private fun onCheckPermissionDenied(permissions: ArrayList<String>) {
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

    /**
     * Callback function for granting permissions for sub class
     *
     * @param permissions permission list
     */
    protected open fun onRequiresPermission(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user requires the permissions: $it")
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
    protected fun showPermissionsEducation() {
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
     *
     * @param permissions permission list
     */
    protected open fun onPermissionDenied(permissions: ArrayList<String>) {
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

    /**
     * Callback function that allow for continuation or cancellation of a permission request..
     *
     * @param request allow for continuation or cancellation of a permission request.
     */
    protected fun onShowRationale(request: PermissionRequest) {
        request.proceed()
    }

    /**
     * Callback function that will be called when the user denies the permissions and tickets "Never Ask Again" after calls requestPermissions()
     *
     * @param permissions permission list
     */
    protected fun onNeverAskAgain(permissions: ArrayList<String>) {
        permissions.forEach {
            logDebug("user denies and never ask for the permissions: $it")
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

    /**
     * Callback function that user requires the Audio permissions
     *
     * @param permissions permission list
     */
    protected fun onRequiresAudioPermission(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
            logDebug("user requires the Audio permissions")
            sharedModel.setRecordAudioPermission(true)
        }
    }

    /**
     * Callback function that user requires the Camera permissions
     *
     * @param permissions permission list
     */
    protected fun onRequiresCameraPermission(permissions: ArrayList<String>) {
        if (permissions.contains(Manifest.permission.CAMERA)) {
            logDebug("user requires the Camera permissions")
            sharedModel.setRecordAudioPermission(true)
        }
    }

    /**
     * Shows a permission education.
     * It will be displayed at the beginning of meeting activity.
     *
     * @param context current Context.
     * @param checkPermission a callback for check permissions
     */
    protected fun showPermissionsEducation(context: Context, checkPermission: () -> Unit) {

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

    /**
     * Update the permission state of ViewModel.
     *
     * @param permission One or more permission strings.
     */
    private fun refreshPermissions(permission: Array<String>) {
        if (bRefreshPermission) {
            bRefreshPermission = false
            sharedModel.let {
                for (i in permission.indices) {
                    val bPermission =
                        PermissionUtils.hasPermissions(requireContext(), permission[i])
                    when (permission[i]) {
                        Manifest.permission.CAMERA -> {
                            it.setCameraPermission(bPermission)
                        }
                        Manifest.permission.RECORD_AUDIO -> {
                            it.setRecordAudioPermission(bPermission)
                        }
                    }
                }
            }
        }
    }

    @Suppress("deprecation") // TODO Migrate to registerForActivityResult()
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        logDebug("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // After request, we should consider "shouldShowRequestPermissionRationale" because the user may ticket 'Don't ask again'
        bRequested = true
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

    companion object {
        // The name of the preference to retrieve.
        protected const val KEY_SHOW_EDUCATION = "show_education"

        // SharedPreference file name
        protected const val MEETINGS_PREFERENCE = "meeting_preference"
    }
}