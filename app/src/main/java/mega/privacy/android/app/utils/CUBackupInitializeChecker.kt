package mega.privacy.android.app.utils

import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_DEVICE_NAMES
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER
import timber.log.Timber

/**
 * A checker class for CU backup.
 *
 * 1. Check if set device name.
 * 2. Check if CU is enabled but hasn't created backup yet.
 */
class CUBackupInitializeChecker(
    private val megaApi: MegaApiAndroid,
) {

    /**
     * If the client has enabled CU, but hasn't set backup, here create the backup for current account.
     * Also check if set device name here.
     */
    fun initCuSync() {
        // Check device name.
        megaApi.getDeviceName(OptionalMegaRequestListenerInterface(
            onRequestFinish = { request, e ->
                if (request.type == TYPE_GET_ATTR_USER || request.paramType == USER_ATTR_DEVICE_NAMES) {
                    Timber.d("${request.requestString} finished with ${e.errorCode}: ${e.errorString}")
                    if (request.name == null || e.errorCode == API_ENOENT) {
                        megaApi.setDeviceName(Util.getDeviceName(),
                            OptionalMegaRequestListenerInterface(
                                onRequestFinish = { setDeviceNameRequest, setDeviceNameError ->
                                    Timber.d("${setDeviceNameRequest.requestString} " +
                                            "finished with ${setDeviceNameError.errorCode}" +
                                            ": ${setDeviceNameError.errorString}")
                                }
                            ))
                    } else {
                        Timber.d("Already set device name.")
                    }
                }
            }
        ))

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
}