package mega.privacy.android.feature.sync.domain.usecase.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetFolderPairsUseCaseTest {

    private lateinit var underTest: GetFolderPairsUseCase

    private val syncRepository = mock<SyncRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFolderPairsUseCase(syncRepository = syncRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncRepository)
    }

    @Test
    fun `test that empty list is returned when no folder pairs exist`() = runTest {
        whenever(syncRepository.getFolderPairs()).thenReturn(emptyList())

        val result = underTest.invoke()

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that single folder pair is returned when one exists`() = runTest {
        val folderPair = createFolderPair(
            id = 1L,
            pairName = "Documents Sync",
            localFolderPath = "/storage/emulated/0/Documents"
        )
        whenever(syncRepository.getFolderPairs()).thenReturn(listOf(folderPair))

        val result = underTest.invoke()

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(folderPair)
    }

    @Test
    fun `test that multiple folder pairs are returned when multiple exist`() = runTest {
        val folderPair1 = createFolderPair(
            id = 1L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Documents Sync",
            localFolderPath = "/storage/emulated/0/Documents"
        )
        val folderPair2 = createFolderPair(
            id = 2L,
            syncType = SyncType.TYPE_BACKUP,
            pairName = "Camera Upload",
            localFolderPath = "/storage/emulated/0/DCIM/Camera"
        )
        val folderPair3 = createFolderPair(
            id = 3L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Media Sync",
            localFolderPath = "content://com.android.providers.media.documents/tree/primary%3ADCIM"
        )

        val folderPairs = listOf(folderPair1, folderPair2, folderPair3)
        whenever(syncRepository.getFolderPairs()).thenReturn(folderPairs)

        val result = underTest.invoke()


        assertThat(result).containsExactly(folderPair1, folderPair2, folderPair3).inOrder()
    }

    @Test
    fun `test that folder pairs with different sync types are returned correctly`() = runTest {
        // Given
        val backupPair = createFolderPair(
            id = 1L,
            syncType = SyncType.TYPE_BACKUP,
            pairName = "Backup Folder"
        )
        val syncPair = createFolderPair(
            id = 2L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "Sync Folder"
        )
        val downSyncPair = createFolderPair(
            id = 3L,
            syncType = SyncType.TYPE_BACKUP,
            pairName = "Down Sync Folder"
        )

        val folderPairs = listOf(backupPair, syncPair, downSyncPair)
        whenever(syncRepository.getFolderPairs()).thenReturn(folderPairs)

        // When
        val result = underTest.invoke()

        // Then
        assertThat(result).hasSize(3)
        assertThat(result[0].syncType).isEqualTo(SyncType.TYPE_BACKUP)
        assertThat(result[1].syncType).isEqualTo(SyncType.TYPE_TWOWAY)
        assertThat(result[2].syncType).isEqualTo(SyncType.TYPE_BACKUP)
    }

    @Test
    fun `test that folder pairs with different sync statuses are returned correctly`() = runTest {
        val syncedPair = createFolderPair(
            id = 1L,
            syncStatus = SyncStatus.SYNCED
        )
        val syncingPair = createFolderPair(
            id = 2L,
            syncStatus = SyncStatus.SYNCING
        )
        val pausedPair = createFolderPair(
            id = 3L,
            syncStatus = SyncStatus.PAUSED
        )

        val folderPairs = listOf(syncedPair, syncingPair, pausedPair)
        whenever(syncRepository.getFolderPairs()).thenReturn(folderPairs)

        val result = underTest.invoke()

        assertThat(result[0].syncStatus).isEqualTo(SyncStatus.SYNCED)
        assertThat(result[1].syncStatus).isEqualTo(SyncStatus.SYNCING)
        assertThat(result[2].syncStatus).isEqualTo(SyncStatus.PAUSED)
    }

    private fun createFolderPair(
        id: Long = 1L,
        syncType: SyncType = SyncType.TYPE_TWOWAY,
        pairName: String = "Test Sync",
        localFolderPath: String = "/storage/emulated/0/Documents",
        remoteFolder: RemoteFolder = RemoteFolder(NodeId(123L), "Remote Test"),
        syncStatus: SyncStatus = SyncStatus.SYNCED,
    ) = FolderPair(
        id = id,
        syncType = syncType,
        pairName = pairName,
        localFolderPath = localFolderPath,
        remoteFolder = remoteFolder,
        syncStatus = syncStatus
    )
}
