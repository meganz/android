package mega.privacy.android.app.utils

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.USER_ATTR_DEVICE_NAMES
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER

/**
 * A checker class for CU backup.
 *
 * 1. Check if set device name.
 * 2. Check if CU is enabled but hasn't created backup yet.
 */
class CUBackupInitializeChecker(
    private val megaApi: MegaApiAndroid
) {

    /**
     * If the client has enabled CU, but hasn't set backup, here create the backup for current account.
     * Also check if set device name here.
     */
    fun initCuSync() {
        // Check device name.
        setDefaultDeviceName()

        val dbH = MegaApplication.getInstance().dbH

        if (CameraUploadUtil.isPrimaryEnabled() && dbH.cuBackup == null) {
            CameraUploadSyncManager.setPrimaryBackup()
        } else if (dbH.cuBackup != null) {
            // Update to make sure backup name is applied on startup.
            CameraUploadSyncManager.updatePrimaryBackupName()
        }

        if (CameraUploadUtil.isSecondaryEnabled() && dbH.muBackup == null) {
            CameraUploadSyncManager.setSecondaryBackup()
        } else if (dbH.muBackup != null) {
            // Update to make sure backup name is applied on startup.
            CameraUploadSyncManager.updateSecondaryBackupName()
        }
    }

    /**
     * Set default device name for the account, only set it when no device name hasn't been set before.
     * Otherwise will overwrite the device name set by the user.
     *
     * Will try to set when login successfully for backward compatibility.
     */
    private fun setDefaultDeviceName() {
        megaApi.getDeviceName(object : BaseListener(null) {

            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                if (request.type != TYPE_GET_ATTR_USER || request.paramType != USER_ATTR_DEVICE_NAMES) return
                logDebug("${request.requestString} finished with ${e.errorCode}: ${e.errorString}")

                // Haven't set device name yet, should set a default name. Otherwise do nothing.
                if (request.name == null || e.errorCode == API_ENOENT) {
                    api.setDeviceName(Util.getDeviceName(), object : BaseListener(null) {

                        override fun onRequestFinish(
                            api: MegaApiJava,
                            request: MegaRequest,
                            e: MegaError
                        ) {
                            logDebug("${request.requestString} finished with ${e.errorCode}: ${e.errorString}")
                        }
                    })
                } else {
                    logDebug("Already set device name.")
                }
            }
        })
    }
}