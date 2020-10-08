package mega.privacy.android.app.sync.cusync.callback

import ash.TL
import mega.privacy.android.app.sync.SyncEventCallback
import mega.privacy.android.app.sync.SyncPair
import mega.privacy.android.app.sync.mock.RequestType
import mega.privacy.android.app.sync.mock.SyncEventResult
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest


open class SetBackupCallback : SyncEventCallback {

    override fun requestType(): Int = RequestType.REQUEST_TYPE_SET.value

    override fun onSuccess(
        result: SyncEventResult,
        api: MegaApiJava?,
        request: MegaRequest?,
        error: MegaError?
    ) {
        result.apply {
            val syncPair =
                SyncPair.create(syncId!!, backupName!!, backupType!!, localFolder!!, targetNode!!)
            getDatabase().saveSyncPair(syncPair)
            TL.log("Successful callback: save $syncPair.")
        }
    }
}