package mega.privacy.android.app.sync

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.sync.mock.SyncEventResult
import mega.privacy.android.app.utils.JobUtil.stopRunningCameraUploadService
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

interface SyncEventCallback {

    fun requestType(): Int

    fun onSuccess(
        result: SyncEventResult,
        api: MegaApiJava?,
        request: MegaRequest?,
        error: MegaError?
    )

    fun onFail(result: SyncEventResult, error: MegaError?) {
        logDebug("${requestType()} failed: ${error?.errorCode}")
        stopRunningCameraUploadService(MegaApplication.getInstance())
    }

    fun getDatabase(): DatabaseHandler = DatabaseHandler.getDbHandler(MegaApplication.getInstance())
}

