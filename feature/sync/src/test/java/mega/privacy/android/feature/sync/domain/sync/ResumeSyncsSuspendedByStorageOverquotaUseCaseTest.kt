package mega.privacy.android.feature.sync.domain.sync

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncsSuspendedByStorageOverquotaUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.IsSyncPausedByTheUserUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResumeSyncsSuspendedByStorageOverquotaUseCaseTest {

    private lateinit var underTest: ResumeSyncsSuspendedByStorageOverquotaUseCase

    private val getFolderPairsUseCase = mock<GetFolderPairsUseCase>()
    private val resumeSyncUseCase = mock<ResumeSyncUseCase>()
    private val isSyncPausedByTheUserUseCase = mock<IsSyncPausedByTheUserUseCase>()

    private val firstSyncId = 1L
    private val secondSyncId = 2L

    @BeforeAll
    fun setUp() {
        underTest = ResumeSyncsSuspendedByStorageOverquotaUseCase(
            getFolderPairsUseCase = getFolderPairsUseCase,
            resumeSyncUseCase = resumeSyncUseCase,
            isSyncPausedByTheUserUseCase = isSyncPausedByTheUserUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFolderPairsUseCase,
            resumeSyncUseCase,
            isSyncPausedByTheUserUseCase,
        )
    }

    @Test
    fun `test that invoke resumes syncs with storage overquota error`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(
                folderPair(firstSyncId, SyncError.STORAGE_OVERQUOTA),
                folderPair(secondSyncId, SyncError.STORAGE_OVERQUOTA),
            )
        )
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(false)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(false)

        underTest()

        verify(resumeSyncUseCase).invoke(firstSyncId)
        verify(resumeSyncUseCase).invoke(secondSyncId)
    }

    @Test
    fun `test that invoke does not resume syncs paused by the user`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(
                folderPair(firstSyncId, SyncError.STORAGE_OVERQUOTA),
                folderPair(secondSyncId, SyncError.STORAGE_OVERQUOTA),
            )
        )
        whenever(isSyncPausedByTheUserUseCase(firstSyncId)).thenReturn(true)
        whenever(isSyncPausedByTheUserUseCase(secondSyncId)).thenReturn(false)

        underTest()

        verify(resumeSyncUseCase, times(0)).invoke(firstSyncId)
        verify(resumeSyncUseCase).invoke(secondSyncId)
    }

    @Test
    fun `test that invoke does not resume syncs without storage overquota error`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(
            listOf(
                folderPair(firstSyncId, null),
                folderPair(secondSyncId, SyncError.UNKNOWN_ERROR),
            )
        )

        underTest()

        verifyNoInteractions(resumeSyncUseCase)
    }

    private fun folderPair(id: Long, syncError: SyncError?) = FolderPair(
        id = id,
        syncType = SyncType.TYPE_TWOWAY,
        pairName = "name$id",
        localFolderPath = "localPath$id",
        remoteFolder = RemoteFolder(NodeId(id), "remotePath$id"),
        syncStatus = if (syncError == null) SyncStatus.SYNCING else SyncStatus.ERROR,
        syncError = syncError,
    )
}
