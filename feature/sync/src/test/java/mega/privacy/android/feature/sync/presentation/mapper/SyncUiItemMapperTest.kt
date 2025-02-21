package mega.privacy.android.feature.sync.presentation.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.data.mapper.SyncStatusMapper
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.shared.sync.DeviceFolderUINodeErrorMessageMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncUiItemMapperTest {

    private val deviceFolderUINodeErrorMessageMapper: DeviceFolderUINodeErrorMessageMapper = mock()
    private val backupInfoTypeIntMapper: BackupInfoTypeIntMapper = mock()
    private val syncStatusMapper: SyncStatusMapper = mock()
    private val underTest = SyncUiItemMapper(
        deviceFolderUINodeErrorMessageMapper = deviceFolderUINodeErrorMessageMapper,
        backupInfoTypeIntMapper = backupInfoTypeIntMapper,
        syncStatusMapper = syncStatusMapper,
    )

    private val folderPair = FolderPair(
        id = 3L,
        syncType = SyncType.TYPE_TWOWAY,
        pairName = "folderPair",
        localFolderPath = "DCIM",
        remoteFolder = RemoteFolder(id = 233L, name = "photos"),
        syncStatus = SyncStatus.SYNCING,
        syncError = null
    )
    private val folderPairsList = listOf(
        folderPair
    )

    @Test
    fun `test on correct conversion`() {
        whenever(deviceFolderUINodeErrorMessageMapper(folderPair.syncError)).thenReturn(null)
        val syncUiItems = listOf(
            SyncUiItem(
                id = 3L,
                syncType = SyncType.TYPE_TWOWAY,
                folderPairName = "folderPair",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = "DCIM",
                megaStoragePath = "photos",
                megaStorageNodeId = NodeId(233L),
                expanded = false
            )
        )

        assertThat(underTest(folderPairsList)).isEqualTo(syncUiItems)
    }

    @Test
    fun `test on incorrect conversion`() {
        whenever(deviceFolderUINodeErrorMessageMapper(folderPair.syncError)).thenReturn(null)
        val syncUiItems = listOf(
            SyncUiItem(
                id = 4L,
                syncType = SyncType.TYPE_TWOWAY,
                folderPairName = "folderPair",
                status = SyncStatus.SYNCING,
                hasStalledIssues = false,
                deviceStoragePath = "DCIM",
                megaStoragePath = "photos",
                megaStorageNodeId = NodeId(1234L),
                expanded = false
            )
        )

        assertThat(underTest(folderPairsList)).isNotEqualTo(syncUiItems)
    }
}