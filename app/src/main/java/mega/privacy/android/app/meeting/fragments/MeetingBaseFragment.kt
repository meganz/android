package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.permission.PermissionRequest
import mega.privacy.android.app.utils.permission.PermissionType
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.app.utils.permission.PermissionsRequester
import mega.privacy.android.app.utils.permission.permissionsBuilder
import timber.log.Timber

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
open class MeetingBaseFragment : Fragment() {

    lateinit var meetingActivity: MeetingActivity

    protected lateinit var permissionsRequester: PermissionsRequester

    protected val sharedModel: MeetingActivityViewModel by activityViewModels()

    // Indicate if permission has been requested. After requested, we should check "shouldShowRequestPermissionRationaleSnackBar"
    private var bRequested = false
    private var bRefreshPermission = false
    protected var requestCode = 0

    // Default permission array for meeting
    protected val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
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
            Timber.d("user check the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(true)
                }
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(true)
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
            Timber.d("user denies the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(false)
                }
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(false)
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
            Timber.d("user requires the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(true)
                }
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(true)
                }
            }
        }
    }

    /**
     * Check the condition of display of permission education dialog
     * Then continue permission check without education dialog
     */
    protected fun showPermissionsEducation() {
        val sp = requireActivity().getSharedPreferences(MEETINGS_PREFERENCE, Context.MODE_PRIVATE)
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
            Timber.d("user denies the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(false)
                }
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(false)
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
            Timber.d("user denies and never ask for the permissions: $it")
            when (it) {
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(false)
                }
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(false)
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
            Timber.d("user requires the Audio permissions")
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
            Timber.d("user requires the Camera permissions")
            sharedModel.setCameraPermission(true)
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
                        Manifest.permission.RECORD_AUDIO -> {
                            it.setRecordAudioPermission(bPermission)
                        }
                        Manifest.permission.CAMERA -> {
                            it.setCameraPermission(bPermission)
                        }
                    }
                }
            }
        }
    }

    @Suppress("deprecation")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        Timber.d("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // After request, we should consider "shouldShowRequestPermissionRationale" because the user may ticket 'Don't ask again'
        bRequested = true
        var i = 0
        while (i < grantResults.size) {
            val bPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED
            when (permissions[i]) {
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel.setRecordAudioPermission(bPermission)
                }
                Manifest.permission.CAMERA -> {
                    sharedModel.setCameraPermission(bPermission)
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