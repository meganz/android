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


class UpdateSyncCallback: SyncEventCallback {

    override fun requestType(): Int = RequestType.REQUEST_TYPE_UPDATE.value

    override fun onSuccess(
        result: SyncEventResult,
        api: MegaApiJava?,
        request: MegaRequest?,
        error: MegaError?
    ) {
        val syncPair = getDatabase().getSyncPairBySyncId(result.syncId)
        if (syncPair != null && !syncPair.outdated) {
            syncPair.apply {
                if (result.targetNode != null) {
                    targetFodlerHanlde = result.targetNode!!
                }
                if (result.localFolder != null) {
                    localFolderPath = result.localFolder!!
                }
            }
            getDatabase().updateSync(syncPair)
            TL.log("Successful callback: update $syncPair.")
        }
    }
}