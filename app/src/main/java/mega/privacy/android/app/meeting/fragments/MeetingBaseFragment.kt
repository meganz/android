package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.PermissionUtils
import mega.privacy.android.app.utils.StringResourcesUtils

// TODO: Add class comment
open class MeetingBaseFragment : BaseFragment() {

    lateinit var meetingActivity : MeetingActivity

    private val KEY_SHOW_EDUCATION: String = "show_education"
    private val MEETINGS_PREFERENCE: String = "meeting_prefrence"
    protected var sharedModel: MeetingActivityViewModel? = null
    private var bRequested = false; // If permission has been requested
    private var bRefreshPermission: Boolean = false
    protected var requestCode = 0

    // Default permission array for meeting
    protected val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment BlankFragment.
         */
        @JvmStatic
        fun newInstance() =
            MeetingBaseFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        meetingActivity = requireActivity() as MeetingActivity
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions(permissions)
    }

    /**
     * Process when it switch to offline
     *
     * @param offline true if off line mode, false if on line mode
     */
    fun processOfflineMode(offline: Boolean) {
        logDebug("processOfflineMode:$offline")
    }

    /**
     * Shows a permission education.
     * It will be displayed at the beginning of meeting activity.
     *
     * @param context current Context.
     * @param checkPermission a callback for check permissions
     */
    private fun showPermissionsEducation(context: Context, checkPermission: () -> Unit) {

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
                    checkPermission()
                }
            }

        permissionsWarningDialogBuilder.show()
    }

    /**
     * Check all the permissions for meeting
     * 1. Check whether permission is granted
     * 2. Request permission
     * 3. Callback after requesting permission
     * 4. Determine whether the user denies permission is to check the don't ask again option, if checked, the client needs to manually open the permission
     *
     * @param permissions Array of permissions
     * @param showSnackbar a callback that display SnackBar, notify the client to manually open the permission in system setting, This only needed when {bRequested} is true
     *
     */
    protected fun checkMeetingPermissions(
        permissions: Array<String>,
        showSnackbar: (() -> Unit)? = null
    ) {
        val sp = app.getSharedPreferences(MEETINGS_PREFERENCE, Context.MODE_PRIVATE)
        val showEducation = sp.getBoolean(KEY_SHOW_EDUCATION, true)
        val mPermissionList: MutableList<String> = ArrayList()
        requestCode = 0
        for (i in permissions.indices) {
            val bPermission = PermissionUtils.hasPermissions(requireContext(), permissions[i])
            // 1. If this permission has not been requested, the user will not necessarily refuse, so it returns false;
            // 2. Requested but rejected, return true at this time;
            // 3. The request for permission is forbidden, and the pop-up window is not reminded, so return false;
            // 4. The request is allowed, so false is returned.
            val showRequestPermission =
                PermissionUtils.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    permissions[i]
                )
            if (!bPermission && bRequested) {
                if (!showRequestPermission) {
                    // The user ticket 'Don't ask again' and deny a permission request.
                    logDebug("the user ticket 'Don't ask again' and deny a permission request.")
                    bRefreshPermission = true
                    if (showSnackbar != null) {
                        showSnackbar()
                    }
                    return
                }
            }
            when (permissions[i]) {
                Manifest.permission.CAMERA -> {
                    sharedModel?.let {
                        it.setCameraPermission(bPermission)
                        if (!bPermission) {
                            requestCode += Constants.REQUEST_CAMERA
                        }
                    }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel?.let {
                        it.setRecordAudioPermission(bPermission)
                        if (!bPermission) {
                            requestCode += Constants.REQUEST_RECORD_AUDIO
                        }
                    }
                }
            }
            if (!bPermission) {
                if (bRequested) {
                    // If 'Don't ask again' is not selected, show the permission request dialog
                    if (showRequestPermission) {
                        mPermissionList.add(permissions[i])
                    }
                } else {
                    // The first time, if bPermission == false, send request
                    mPermissionList.add(permissions[i])
                }
            }
        }
        if (mPermissionList.isNotEmpty()) {
            if (showEducation) {
                sp.edit()
                    .putBoolean(KEY_SHOW_EDUCATION, false).apply()
                showPermissionsEducation(requireActivity()) {
                    checkMeetingPermissions(
                        permissions
                    )
                }
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
     * Update the permission state of ViewModel,
     *
     * @param permission One or more permission strings.
     *
     */
    private fun refreshPermissions(permission: Array<String>) {
        if (bRefreshPermission) {
            bRefreshPermission = false
            sharedModel?.let {
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
                    sharedModel?.let { it.setCameraPermission(bPermission) }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    sharedModel?.let { it.setRecordAudioPermission(bPermission) }
                }
            }
            i++
        }
    }
}