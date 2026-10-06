package mega.privacy.android.feature.sync.data.mapper.progress

import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaSyncStats
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal fun mockMegaSync(
    backupId: Long,
    runState: Int = MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue(),
    error: Int = 0,
): MegaSync = mock {
    on { this.backupId } doReturn backupId
    on { this.runState } doReturn runState
    on { this.error } doReturn error
}

internal fun mockMegaSyncStats(
    backupId: Long,
    uploads: Int = 0,
    downloads: Int = 0,
    isScanning: Boolean = false,
    isSyncing: Boolean = false,
): MegaSyncStats = mock {
    on { this.backupId } doReturn backupId
    on { this.uploadCount } doReturn uploads
    on { this.downloadCount } doReturn downloads
    on { this.isScanning } doReturn isScanning
    on { this.isSyncing } doReturn isSyncing
}

internal fun mockMegaSyncList(vararg syncs: MegaSync): MegaSyncList = mock {
    on { size() } doReturn syncs.size
    on { get(any()) } doAnswer { syncs[it.getArgument<Int>(0)] }
}
