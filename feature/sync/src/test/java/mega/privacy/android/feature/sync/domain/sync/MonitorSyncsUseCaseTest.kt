package mega.privacy.android.feature.sync.domain.sync

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.file.CanReadUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.ChangeSyncLocalRootUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCaseImpl
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSyncsUseCaseTest {

    private val syncRepository: SyncRepository = mock()
    private val changeSyncLocalRootUseCase: ChangeSyncLocalRootUseCase = mock()
    private val resumeSyncUseCase: ResumeSyncUseCase = mock()
    private val canReadUriUseCase: CanReadUriUseCase = mock()

    private val underTest = MonitorSyncsUseCaseImpl(
        syncRepository = syncRepository,
        changeSyncLocalRootUseCase = changeSyncLocalRootUseCase,
        resumeSyncUseCase = resumeSyncUseCase,
        canReadUriUseCase = canReadUriUseCase,
    )

    private val validFolderPairs = listOf(
        FolderPair(
            id = 1L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "ValidFolder",
            localFolderPath = "DCIM",
            remoteFolder = RemoteFolder(id = NodeId(123L), name = "photos"),
            syncStatus = SyncStatus.SYNCING,
            syncError = SyncError.NO_SYNC_ERROR
        ),
        FolderPair(
            id = 2L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "ValidFolder",
            localFolderPath = "Documents",
            remoteFolder = RemoteFolder(id = NodeId(123L), name = "documents"),
            syncStatus = SyncStatus.SYNCING,
            syncError = SyncError.NO_SYNC_ERROR
        )
    )

    private val invalidFolderPairs = listOf(
        FolderPair(
            id = 3L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "InvalidFolder",
            localFolderPath = "InvalidPath",
            remoteFolder = RemoteFolder(id = NodeId(456L), name = "videos"),
            syncStatus = SyncStatus.SYNCING,
            syncError = SyncError.MISMATCH_OF_ROOT_FSID
        ),
        FolderPair(
            id = 4L,
            syncType = SyncType.TYPE_BACKUP,
            pairName = "AnotherInvalidFolder",
            localFolderPath = "AnotherInvalidPath",
            remoteFolder = RemoteFolder(id = NodeId(789L), name = "documents"),
            syncStatus = SyncStatus.SYNCING,
            syncError = SyncError.MISMATCH_OF_ROOT_FSID
        )
    )


    @AfterEach
    fun resetMocks() {
        reset(syncRepository, changeSyncLocalRootUseCase, resumeSyncUseCase, canReadUriUseCase)
    }

    @Test
    fun `test that monitor folder pair changes emits valid folder pairs`() = runTest {
        whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
            flow {
                emit(validFolderPairs)
                awaitCancellation()
            }
        )

        underTest().test {
            val result = awaitItem()
            Truth.assertThat(result).containsExactlyElementsIn(validFolderPairs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invalid folder pairs are processed and resumed if URI is readable`() = runTest {
        whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
            flow {
                emit(validFolderPairs + invalidFolderPairs)
                awaitCancellation()
            }
        )
        invalidFolderPairs.forEach {
            whenever(canReadUriUseCase(it.localFolderPath)).thenReturn(true)
        }


        underTest().test {
            invalidFolderPairs.forEach { invalidFolderPair ->
                verify(changeSyncLocalRootUseCase).invoke(
                    invalidFolderPair.id,
                    invalidFolderPair.localFolderPath
                )
                verify(resumeSyncUseCase).invoke(invalidFolderPair.id)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that invalid folder pairs are not resumed if URI is not readable`() = runTest {
        whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
            flow {
                emit(validFolderPairs + invalidFolderPairs)
                awaitCancellation()
            }
        )
        invalidFolderPairs.forEach {
            whenever(canReadUriUseCase(it.localFolderPath)).thenReturn(false)
        }

        underTest().test {
            val result = awaitItem()
            verify(changeSyncLocalRootUseCase, never()).invoke(any(), any())
            verify(resumeSyncUseCase, never()).invoke(any())
            Truth.assertThat(result)
                .containsExactlyElementsIn(validFolderPairs + invalidFolderPairs)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
