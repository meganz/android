package mega.privacy.android.feature.sync.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.data.mapper.backup.SyncErrorMapper
import mega.privacy.android.data.mapper.sync.SyncTypeMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncStats
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FolderPairMapperTest {

    private val syncStatusMapper: SyncStatusMapper = mock()
    private val syncErrorMapper: SyncErrorMapper = mock()
    private val syncTypeMapper: SyncTypeMapper = mock()
    private val underTest = FolderPairMapper(
        mapSyncStatus = syncStatusMapper,
        syncErrorMapper = syncErrorMapper,
        syncTypeMapper = syncTypeMapper,
    )

    @Test
    fun `test that folder pair sdk object is mapped correctly`() {
        val syncId = 1234L
        val syncLocalFolder = "Local Folder"
        val syncRemoteFolder = "Remote Folder"

        val model: MegaSync = mock {
            on { backupId } doReturn syncId
            on { type } doReturn MegaSync.SyncType.TYPE_TWOWAY.swigValue()
            on { name } doReturn ""
            on { localFolder } doReturn syncLocalFolder
            on { megaHandle } doReturn 1L
            on { runState } doReturn MegaSync.SyncRunningState.RUNSTATE_RUNNING.swigValue()
            on { error } doReturn MegaSync.Error.NO_SYNC_ERROR.swigValue()
        }

        val syncStats: MegaSyncStats = mock()

        whenever(syncTypeMapper(MegaSync.SyncType.swigToEnum(model.type))).thenReturn(SyncType.TYPE_TWOWAY)
        whenever(syncStatusMapper(syncStats, model.runState, false)).thenReturn(SyncStatus.SYNCED)

        val expected = FolderPair(
            id = syncId,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "",
            localFolderPath = syncLocalFolder,
            remoteFolder = RemoteFolder(id = NodeId(1L), name = syncRemoteFolder),
            syncStatus = SyncStatus.SYNCED
        )

        val actual = underTest(
            model = model,
            megaFolderName = syncRemoteFolder,
            syncStats = syncStats,
            isStorageOverQuota = false,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }
}
