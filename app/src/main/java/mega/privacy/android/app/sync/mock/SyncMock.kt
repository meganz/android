package mega.privacy.android.app.sync.mock

import ash.TL
import mega.privacy.android.app.sync.name
import mega.privacy.android.app.utils.LogUtil.logDebug
import kotlin.random.Random

const val SYNC_TYPE_PRIMARY = 0
const val SYNC_TYPE_SECONDARY = 1
const val NAME_PRIMARY = "camera uploads"
const val NAME_SECONDARY = "media uploads"
const val NAME_OTHER = "other sync"

enum class RequestType(val value: Int) {
    REQUEST_TYPE_SET(0), REQUEST_TYPE_UPDATE(1), REQUEST_TYPE_DELETE(2)
}

fun setBackupMock(
    backupType: Int,
    targetNode: Long,
    localFolder: String,
    backupName: String,
    listener: MockListener
) {
    logDebug("Set backup in SDK: $backupType $backupName $localFolder $targetNode(${targetNode.name()})")
    TL.log("Set backup in SDK: $backupType $backupName $localFolder $targetNode(${targetNode.name()})")
    val result = true//randomResult()
    if (result) {
        listener.onFinish(
            SyncEventResult(
                RequestType.REQUEST_TYPE_SET,
                Random.nextLong(),
                backupType,
                backupName,
                localFolder,
                targetNode
            ), 0
        )
    } else {
        listener.onFinish(
            SyncEventResult(
                RequestType.REQUEST_TYPE_SET,
                -1L,
                backupType,
                backupName,
                localFolder,
                targetNode
            ),
            -3
        )
    }
}

fun updateBackupMock(
    backupId: Long,
    targetNode: Long?,
    localFolder: String?,
    listener: MockListener
) {
    logDebug("Update backup in SDK: $backupId $localFolder $targetNode(${targetNode?.name()})")
    TL.log("Update backup in SDK: $backupId $localFolder $targetNode(${targetNode?.name()})")
    val result = true//randomResult()
    if (result) {
        listener.onFinish(updateResult(backupId, targetNode, localFolder), 0)
    } else {
        listener.onFinish(updateResult(backupId, targetNode, localFolder), -3)
    }
}

fun deleteBackupMock(
    backupId: Long,
    listener: MockListener
) {
    logDebug("Delete backup in SDK: $backupId")
    TL.log("Delete backup in SDK: $backupId")
    val result = true//randomResult()
    if (result) {
        listener.onFinish(deleteResult(backupId), 0)
    } else {
        listener.onFinish(deleteResult(backupId), -3)
    }
}

data class SyncEventResult(
    var requestType: RequestType,
    var syncId: Long,
    var backupType: Int?,
    var backupName: String?,
    var localFolder: String?,
    var targetNode: Long?
)

fun deleteResult(id: Long) =
    SyncEventResult(RequestType.REQUEST_TYPE_DELETE, id, null, null, null, null)

fun updateResult(id: Long, targetNode: Long?, localFolder: String?) = SyncEventResult(
    RequestType.REQUEST_TYPE_UPDATE, id, null, null, localFolder, targetNode
)

interface MockListener {

    fun onFinish(result: SyncEventResult, errorCode: Int)

}