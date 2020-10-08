package mega.privacy.android.app.sync.cusync.callback

import ash.TL
import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.sync.mock.RequestType
import mega.privacy.android.app.sync.mock.SyncEventResult
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


class DeleteBackupCallback : SyncEventCallback {

    override fun requestType(): Int = RequestType.REQUEST_TYPE_DELETE.value

    override fun onSuccess(
        result: SyncEventResult,
        api: MegaApiJava?,
        request: MegaRequest?,
        error: MegaError?
    ) {
        getDatabase().deleteSyncPairById(result.syncId)
        logDebug("Successful callback: delete ${result.syncId}.")
        TL.log("Successful callback: delete ${result.syncId}.")
    }

    override fun onFail(result: SyncEventResult, error: MegaError?) {
        logDebug("Delete sync with id ${result.syncId} failed.")
        getDatabase().setSyncPairAsOutdated(result.syncId)
    }
}