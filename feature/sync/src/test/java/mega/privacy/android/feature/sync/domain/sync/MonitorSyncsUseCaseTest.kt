package mega.privacy.android.feature.sync.domain.sync

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
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
import mega.privacy.android.feature.sync.domain.usecase.sync.SetSyncWorkerForegroundPreferenceUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncsUseCaseTest {

    private val syncRepository: SyncRepository = mock()
    private val changeSyncLocalRootUseCase: ChangeSyncLocalRootUseCase = mock()
    private val resumeSyncUseCase: ResumeSyncUseCase = mock()
    private val canReadUriUseCase: CanReadUriUseCase = mock()

    private val setSyncWorkerForegroundPreferenceUseCase: SetSyncWorkerForegroundPreferenceUseCase =
        mock()

    private val underTest = MonitorSyncsUseCaseImpl(
        syncRepository = syncRepository,
        changeSyncLocalRootUseCase = changeSyncLocalRootUseCase,
        resumeSyncUseCase = resumeSyncUseCase,
        canReadUriUseCase = canReadUriUseCase,
        setSyncWorkerForegroundPreferenceUseCase = setSyncWorkerForegroundPreferenceUseCase
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
        reset(
            syncRepository,
            changeSyncLocalRootUseCase,
            resumeSyncUseCase,
            canReadUriUseCase,
            setSyncWorkerForegroundPreferenceUseCase
        )
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
            val expectedFolderPairs = validFolderPairs + invalidFolderPairs.map {
                it.copy(syncStatus = SyncStatus.PAUSED, syncError = SyncError.NO_SYNC_ERROR)
            }
            Truth.assertThat(result).containsExactlyElementsIn(expectedFolderPairs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that duplicate emissions are filtered and rapid emissions are conflated`() = runTest {
        val folderPair = validFolderPairs.first()
        val invalidFolderPair = invalidFolderPairs.first()
        val emissions = listOf(
            listOf(
                folderPair,
                invalidFolderPair
            ), // emit 1
            listOf(folderPair), // emit 2
            listOf(folderPair.copy(syncStatus = SyncStatus.PAUSED)), // new, should emit
            listOf(folderPair.copy(syncStatus = SyncStatus.PAUSED)), // duplicate, should be filtered
        )

        whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
            flow {
                emissions.forEach {
                    emit(it)
                    delay(100)
                }
                awaitCancellation()
            }
        )

        wheneverBlocking { canReadUriUseCase(any()) }.doSuspendableAnswer {
            delay(300)
            true // Simulate that the URI is readable
        }

        underTest().test {
            // First emission: SYNCING
            val first = awaitItem()
            Truth.assertThat(first).containsExactlyElementsIn(
                listOf(
                    folderPair,
                    invalidFolderPair.copy(
                        syncStatus = SyncStatus.PAUSED,
                        syncError = SyncError.NO_SYNC_ERROR
                    )
                )
            )
            awaitItem() // Consume the second emission after changing the root and resuming the sync
            advanceTimeBy(300)


            // Second emission: PAUSED (after processing by use case)
            val second = awaitItem()
            Truth.assertThat(second).containsExactlyElementsIn(
                listOf(
                    folderPair.copy(
                        syncStatus = SyncStatus.PAUSED,
                    )
                )
            )

            // No more emissions
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that only syncs with MISMATCH_OF_ROOT_FSID error are processed as invalid`() =
        runTest {
            val syncWithDifferentError = FolderPair(
                id = 5L,
                syncType = SyncType.TYPE_TWOWAY,
                pairName = "DifferentErrorSync",
                localFolderPath = "SomePath",
                remoteFolder = RemoteFolder(id = NodeId(999L), name = "test"),
                syncStatus = SyncStatus.SYNCING,
                syncError = SyncError.ACTIVE_SYNC_SAME_PATH // Different error
            )

            val mixedSyncs = validFolderPairs + invalidFolderPairs + listOf(syncWithDifferentError)

            whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
                flow {
                    emit(mixedSyncs)
                    awaitCancellation()
                }
            )

            invalidFolderPairs.forEach {
                whenever(canReadUriUseCase(it.localFolderPath)).thenReturn(false)
            }

            underTest().test {
                val result = awaitItem()

                // Only MISMATCH_OF_ROOT_FSID syncs should be converted to paused
                val expectedResult = validFolderPairs +
                        listOf(syncWithDifferentError) + // This should remain unchanged
                        invalidFolderPairs.map {
                            it.copy(
                                syncStatus = SyncStatus.PAUSED,
                                syncError = SyncError.NO_SYNC_ERROR
                            )
                        }

                Truth.assertThat(result).containsExactlyElementsIn(expectedResult)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that setSyncWorkerForegroundPreference is called when all syncs are completed`() =
        runTest {
            val completedSyncs = listOf(
                validFolderPairs[0].copy(syncStatus = SyncStatus.SYNCED),
                validFolderPairs[1].copy(syncStatus = SyncStatus.PAUSED)
            )

            whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
                flow {
                    emit(completedSyncs)
                    awaitCancellation()
                }
            )

            underTest().test {
                awaitItem()
                verify(setSyncWorkerForegroundPreferenceUseCase).invoke(false)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that setSyncWorkerForegroundPreference is not called when syncs are still running`() =
        runTest {
            val runningSyncs = listOf(
                validFolderPairs[0].copy(syncStatus = SyncStatus.SYNCING),
                validFolderPairs[1].copy(syncStatus = SyncStatus.SYNCED)
            )

            whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
                flow {
                    emit(runningSyncs)
                    awaitCancellation()
                }
            )

            underTest().test {
                awaitItem()
                verify(setSyncWorkerForegroundPreferenceUseCase, never()).invoke(any())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that setSyncWorkerForegroundPreference is not called when sync list is empty`() =
        runTest {
            whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
                flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            )

            underTest().test {
                val result = awaitItem()
                Truth.assertThat(result).isEmpty()
                verify(setSyncWorkerForegroundPreferenceUseCase, never()).invoke(any())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
