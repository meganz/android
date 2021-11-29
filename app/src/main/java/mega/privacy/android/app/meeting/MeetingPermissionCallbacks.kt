package mega.privacy.android.app.meeting

import android.Manifest
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.permission.PermissionUtils

/**
 * Class which allows implement only the necessary callbacks of PermissionUtils.PermissionCallbacks.
 *
 * @param viewModel The instance of MeetingActivityViewModel
 */
open class MeetingPermissionCallbacks(val viewModel: MeetingActivityViewModel) :
    PermissionUtils.PermissionCallbacks {

    override fun onPermissionsCallback(requestType: Int, perms: ArrayList<String>) {
        LogUtil.logDebug("PermissionsCallback requestType = $requestType")
        perms.forEach {
            when (it) {
                Manifest.permission.CAMERA -> {
                    when (requestType) {
                        PermissionUtils.TYPE_DENIED, PermissionUtils.TYPE_NEVER_ASK_AGAIN -> {
                            viewModel.setCameraPermission(false)
                        }
                        else -> {
                            viewModel.setCameraPermission(true)
                        }
                    }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    when (requestType) {
                        PermissionUtils.TYPE_DENIED, PermissionUtils.TYPE_NEVER_ASK_AGAIN -> {
                            viewModel.setRecordAudioPermission(false)
                        }
                        else -> {
                            viewModel.setRecordAudioPermission(true)
                        }
                    }
                }
            }
        }
    }
}