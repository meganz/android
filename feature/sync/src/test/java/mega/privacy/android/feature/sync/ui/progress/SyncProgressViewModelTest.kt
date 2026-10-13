package mega.privacy.android.feature.sync.ui.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncPauseCause
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressPhase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilterCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferSpeeds
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import mega.privacy.android.feature.sync.domain.usecase.progress.GetSyncTransferListUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorRetryingSyncTransferOverQuotaUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncProgressBlockersUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncProgressStatusUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncTransferListUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.MonitorSyncTransferSpeedsUseCase
import mega.privacy.android.feature.sync.domain.usecase.progress.band
import mega.privacy.android.feature.sync.ui.mapper.progress.SyncProgressCompletedBodyMapper
import mega.privacy.android.feature.sync.ui.model.progress.SyncActiveListItem
import mega.privacy.android.feature.sync.ui.model.progress.SyncActiveTransferUiModel
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressBody
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressCompletedBody
import mega.privacy.android.feature.sync.ui.model.progress.SyncProgressUiState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
class SyncProgressViewModelTest {

    private lateinit var underTest: SyncProgressViewModel

    private lateinit var statusFlow: MutableStateFlow<SyncProgressStatus>
    private lateinit var blockersFlow: MutableStateFlow<SyncProgressBlockers>
    private lateinit var pendingFlow: MutableSharedFlow<SyncTransferList>
    private lateinit var completedFlow: MutableSharedFlow<SyncTransferList>
    private lateinit var speedsFlow: MutableSharedFlow<SyncTransferSpeeds>
    private lateinit var retryingOverQuotaFlow: MutableSharedFlow<Boolean>

    private lateinit var monitorSyncTransferListUseCase: MonitorSyncTransferListUseCase
    private lateinit var monitorSyncProgressBlockersUseCase: MonitorSyncProgressBlockersUseCase

    @BeforeEach
    fun setUp() {
        statusFlow = MutableStateFlow(status(phase = SyncProgressPhase.UpToDate))
        blockersFlow = MutableStateFlow(blockers())
        pendingFlow = MutableSharedFlow(replay = 1)
        completedFlow = MutableSharedFlow(replay = 1)
        speedsFlow = MutableSharedFlow(replay = 1)
        retryingOverQuotaFlow = MutableSharedFlow(replay = 1)
        monitorSyncTransferListUseCase = mock {
            on { invoke(any(), any()) } doReturn emptyFlow()
            on { invoke(eq(SyncTransferFilter.Pending), any()) } doReturn pendingFlow
            on { invoke(eq(SyncTransferFilter.Completed), any()) } doReturn completedFlow
        }
        monitorSyncProgressBlockersUseCase = mock {
            on { invoke() } doReturn blockersFlow
        }
        underTest = createSyncProgressViewModel(
            monitorSyncProgressStatusUseCase = mock<MonitorSyncProgressStatusUseCase> {
                on { invoke() } doReturn statusFlow
            },
            monitorSyncTransferListUseCase = monitorSyncTransferListUseCase,
            monitorSyncProgressBlockersUseCase = monitorSyncProgressBlockersUseCase,
            monitorSyncTransferSpeedsUseCase = mock<MonitorSyncTransferSpeedsUseCase> {
                on { invoke() } doReturn speedsFlow
            },
            monitorRetryingSyncTransferOverQuotaUseCase =
                mock<MonitorRetryingSyncTransferOverQuotaUseCase> {
                    on { invoke() } doReturn retryingOverQuotaFlow
                },
        )
    }

    @Test
    fun `test that the initial uiState is Loading`() = runTest {
        val state = underTest.uiState.value

        assertThat(state.activeBody).isEqualTo(SyncProgressBody.Loading)
        assertThat(state.completedBody).isEqualTo(SyncProgressCompletedBody.Loading)
    }

    @Test
    fun `test that uiState is loading before the store is seeded`() = runTest {
        statusFlow.value = status(pending = 12)
        pendingFlow.emit(list(listOf(active(entry(tag = 1))), isSeeded = false))
        completedFlow.emit(list(isSeeded = false))

        val state = latestState()

        assertThat(state.activeBody).isEqualTo(SyncProgressBody.Loading)
        assertThat(state.completedBody).isEqualTo(SyncProgressCompletedBody.Loading)
    }

    @Test
    fun `test that uiState stays loading when the status arrives before the lists`() = runTest {
        underTest.uiState.test {
            val states = mutableListOf(awaitItem())
            listOf(1, 2, 3).forEach {
                statusFlow.value = status(pending = it)
                states += awaitItem()
            }

            assertThat(states.map { it.filesLeft }).containsAtLeast(1, 2, 3)
            states.forEach {
                assertThat(it.activeBody).isEqualTo(SyncProgressBody.Loading)
                assertThat(it.completedBody).isEqualTo(SyncProgressCompletedBody.Loading)
            }
        }
    }

    @Test
    fun `test that uiState is loading when the status is not seeded even with rows`() = runTest {
        statusFlow.value = status(isSeeded = false)
        pendingFlow.emit(list(listOf(active(entry(tag = 1)))))

        assertThat(latestState().activeBody).isEqualTo(SyncProgressBody.Loading)
    }

    @Test
    fun `test that uiState shows the active rows in desktop order`() = runTest {
        pendingFlow.emit(list(listOf(5, 2, 9).map { active(entry(tag = it)) }))

        assertThat(latestState().rows().map { it.tag }).containsExactly(5, 2, 9).inOrder()
    }

    @Test
    fun `test that uiState shows the rows whatever the phase`() = runTest {
        statusFlow.value = status(phase = SyncProgressPhase.UpToDate)
        pendingFlow.emit(list(listOf(active(entry(tag = 1)))))

        assertThat(latestState().activeBody).isInstanceOf(SyncProgressBody.Transfers::class.java)
    }

    @Test
    fun `test that uiState shows the completed rows in the list order`() = runTest {
        completedFlow.emit(list(listOf(finished(entry(tag = 3)), finished(entry(tag = 1)))))

        val body = latestState().completedBody

        assertThat(body).isInstanceOf(SyncProgressCompletedBody.Transfers::class.java)
        assertThat((body as SyncProgressCompletedBody.Transfers).items.map { it.tag })
            .containsExactly(3, 1)
            .inOrder()
    }

    @Test
    fun `test that uiState shows an empty completed tab when nothing completed`() = runTest {
        completedFlow.emit(list())

        assertThat(latestState().completedBody).isEqualTo(SyncProgressCompletedBody.Empty)
    }

    @Test
    fun `test that uiState shows the files left from the status`() = runTest {
        statusFlow.value = status(
            phase = SyncProgressPhase.Paused(SyncPauseCause.User),
            interrupted = 40,
        )

        val state = latestState()

        assertThat(state.filesLeft).isEqualTo(40)
        assertThat(state.activeBody).isEqualTo(SyncProgressBody.Loading)
        assertThat(state.completedBody).isEqualTo(SyncProgressCompletedBody.Loading)
    }

    @Test
    fun `test that uiState computes more waiting from pending items minus shown rows`() = runTest {
        statusFlow.value = status(pending = 250)
        pendingFlow.emit(list((1..5).map { active(entry(tag = it)) }))

        val items = (latestState().activeBody as SyncProgressBody.Transfers).items

        assertThat(items.last()).isEqualTo(SyncActiveListItem.MoreWaiting(245))
    }

    @Test
    fun `test that uiState exposes the app wide pause from the blockers`() = runTest {
        blockersFlow.value = blockers(areTransfersPaused = true)

        assertThat(latestState().areTransfersPaused).isTrue()
    }

    @Test
    fun `test that uiState keeps the app wide pause out of the row own pause`() = runTest {
        blockersFlow.value = blockers(areTransfersPaused = true)
        pendingFlow.emit(
            list(listOf(active(entry(tag = 1, state = TransferState.STATE_ACTIVE, transferredBytes = 0L)))),
        )

        val row = latestState().rows().single()

        assertThat(row.status).isEqualTo(SyncTransferStatus.PausedInQueue)
        assertThat(row.isOwnPause).isFalse()
    }

    @Test
    fun `test that uiState takes the row status and own pause from the row state`() = runTest {
        pendingFlow.emit(
            list(
                listOf(
                    active(
                        entry(tag = 1, state = TransferState.STATE_ACTIVE, transferredBytes = 400L),
                        status = SyncTransferStatus.Queued,
                    ),
                    active(entry(tag = 2, state = TransferState.STATE_PAUSED)),
                )
            ),
        )

        val rows = latestState().rows().associateBy { it.tag }

        assertThat(rows.getValue(1).status).isEqualTo(SyncTransferStatus.Transferring)
        assertThat(rows.getValue(2).isOwnPause).isTrue()
    }

    @Test
    fun `test that uiState takes each row band from the list item`() = runTest {
        val transferring = active(entry(tag = 1), status = SyncTransferStatus.Transferring)
        val paused = active(
            entry(tag = 2, state = TransferState.STATE_ACTIVE),
            status = SyncTransferStatus.Paused,
        )
        pendingFlow.emit(list(listOf(transferring, paused)))

        val rows = latestState().rows().associateBy { it.tag }

        assertThat(rows.getValue(1).band).isEqualTo(transferring.band)
        assertThat(rows.getValue(2).band).isEqualTo(paused.band)
        assertThat(transferring.band).isNotEqualTo(paused.band)
        assertThat(rows.getValue(2).status).isEqualTo(SyncTransferStatus.Transferring)
    }

    @Test
    fun `test that uiState caps the row speed at the direction speed`() = runTest {
        speedsFlow.emit(SyncTransferSpeeds(uploadSpeed = 50L, downloadSpeed = null))
        pendingFlow.emit(
            list(
                listOf(
                    active(entry(tag = 1, direction = SyncTransferDirection.Upload, speed = 100L)),
                    active(entry(tag = 2, direction = SyncTransferDirection.Download, speed = 100L)),
                )
            ),
        )

        val rows = latestState().rows().associateBy { it.tag }

        assertThat(rows.getValue(1).speed).isEqualTo(50L)
        assertThat(rows.getValue(2).speed).isEqualTo(100L)
    }

    @Test
    fun `test that uiState shows the row speed before the speeds arrive`() = runTest {
        pendingFlow.emit(list(listOf(active(entry(tag = 1, speed = 100L)))))

        assertThat(latestState().rows().single().speed).isEqualTo(100L)
    }

    @Test
    fun `test that uiState exposes filter counts`() = runTest {
        val counts = SyncTransferFilterCounts(
            pending = 7,
            pendingUploads = 4,
            pendingDownloads = 3,
            completed = 2,
            byMediaType = persistentMapOf(SyncMediaType.Image to 9),
            searchUploads = 0,
            searchDownloads = 0,
        )
        pendingFlow.emit(list(counts = counts))

        assertThat(latestState().filterCounts).isEqualTo(counts)
    }

    @Test
    fun `test that uiState requests the pending and completed projections`() = runTest {
        latestState()

        verify(monitorSyncTransferListUseCase).invoke(
            SyncTransferFilter.Pending,
            GetSyncTransferListUseCase.VISIBLE_ROW_LIMIT,
        )
        verify(monitorSyncTransferListUseCase).invoke(
            SyncTransferFilter.Completed,
            GetSyncTransferListUseCase.VISIBLE_ROW_LIMIT,
        )
    }

    @Test
    fun `test that uiState keeps its last value when an upstream fails`() = runTest {
        val signal = CompletableDeferred<Unit>()
        monitorSyncProgressBlockersUseCase.stub {
            on { invoke() } doReturn flow {
                emit(blockers())
                signal.await()
                throw RuntimeException("upstream failure")
            }
        }
        pendingFlow.emit(list(listOf(active(entry(tag = 1)))))

        underTest.uiState.test {
            advanceUntilIdle()
            val transfers = expectMostRecentItem()
            assertThat(transfers.activeBody).isInstanceOf(SyncProgressBody.Transfers::class.java)

            signal.complete(Unit)
            advanceUntilIdle()

            assertThat(underTest.uiState.value).isEqualTo(transfers)
            expectNoEvents()
        }
    }

    @Test
    fun `test that uiState stops collecting the lists after the stop timeout`() = runTest {
        underTest.uiState.test {
            advanceUntilIdle()
            assertThat(pendingFlow.subscriptionCount.value).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }

        advanceTimeBy(4_999)
        assertThat(pendingFlow.subscriptionCount.value).isEqualTo(1)

        advanceTimeBy(2)

        assertThat(pendingFlow.subscriptionCount.value).isEqualTo(0)
        assertThat(speedsFlow.subscriptionCount.value).isEqualTo(0)
    }

    @Test
    fun `test that uiState feeds the blockers quota flags into the row state`() = runTest {
        blockersFlow.value = blockers(isStorageOverQuota = true)
        pendingFlow.emit(
            list(
                listOf(
                    active(
                        entry(
                            tag = 1,
                            direction = SyncTransferDirection.Upload,
                            state = TransferState.STATE_QUEUED,
                            lastError = SyncTransferError(code = -17, value = 0),
                        )
                    )
                )
            ),
        )

        val row = latestState().rows().single()

        assertThat(row.status).isEqualTo(SyncTransferStatus.OutOfStorageSpace)
        assertThat(row.lastErrorCode).isEqualTo(-17)
    }

    @Test
    fun `test that uiState marks a download row transfer over quota when a retrying row is over transfer quota`() =
        runTest {
            retryingOverQuotaFlow.emit(true)
            pendingFlow.emit(
                list(listOf(active(entry(tag = 1, direction = SyncTransferDirection.Download)))),
            )

            assertThat(latestState().rows().single().isTransferOverQuota).isTrue()
        }

    @Test
    fun `test that uiState does not mark an upload row transfer over quota when a retrying row is over transfer quota`() =
        runTest {
            retryingOverQuotaFlow.emit(true)
            pendingFlow.emit(
                list(listOf(active(entry(tag = 1, direction = SyncTransferDirection.Upload)))),
            )

            assertThat(latestState().rows().single().isTransferOverQuota).isFalse()
        }

    @Test
    fun `test that uiState does not remap the completed body when only the speeds change`() = runTest {
        val completedBodyMapper = mock<SyncProgressCompletedBodyMapper> {
            on { invoke(anyOrNull(), any()) } doReturn SyncProgressCompletedBody.Empty
        }
        underTest = createSyncProgressViewModel(
            monitorSyncProgressStatusUseCase = mock<MonitorSyncProgressStatusUseCase> {
                on { invoke() } doReturn statusFlow
            },
            monitorSyncTransferListUseCase = monitorSyncTransferListUseCase,
            monitorSyncTransferSpeedsUseCase = mock<MonitorSyncTransferSpeedsUseCase> {
                on { invoke() } doReturn speedsFlow
            },
            syncProgressCompletedBodyMapper = completedBodyMapper,
        )
        val completed = list(listOf(finished(entry(tag = 2))))
        pendingFlow.emit(list(listOf(active(entry(tag = 1, speed = 100L)))))
        completedFlow.emit(completed)

        underTest.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem().completedBody).isEqualTo(SyncProgressCompletedBody.Empty)
            verify(completedBodyMapper).invoke(completed, true)
            clearInvocations(completedBodyMapper)

            listOf(50L, 20L).forEach { uploadSpeed ->
                speedsFlow.emit(SyncTransferSpeeds(uploadSpeed = uploadSpeed, downloadSpeed = null))
                advanceUntilIdle()
                assertThat(expectMostRecentItem().rows().single().speed).isEqualTo(uploadSpeed)
            }

            verify(completedBodyMapper, never()).invoke(anyOrNull(), any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState does not remap the completed body when the completed list repeats`() = runTest {
        val completedBodyMapper = mock<SyncProgressCompletedBodyMapper> {
            on { invoke(anyOrNull(), any()) } doReturn SyncProgressCompletedBody.Empty
        }
        underTest = createSyncProgressViewModel(
            monitorSyncTransferListUseCase = monitorSyncTransferListUseCase,
            syncProgressCompletedBodyMapper = completedBodyMapper,
        )
        val completed = list(listOf(finished(entry(tag = 2))))
        completedFlow.emit(completed)

        underTest.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem().completedBody).isEqualTo(SyncProgressCompletedBody.Empty)
            verify(completedBodyMapper).invoke(completed, true)
            clearInvocations(completedBodyMapper)

            completedFlow.emit(list(listOf(finished(entry(tag = 2)))))
            advanceUntilIdle()

            verify(completedBodyMapper, never()).invoke(anyOrNull(), any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState remaps the completed body when the completed list changes`() = runTest {
        underTest.uiState.test {
            completedFlow.emit(list(listOf(finished(entry(tag = 2)))))
            advanceUntilIdle()
            assertThat(expectMostRecentItem().completedTags()).containsExactly(2)

            completedFlow.emit(list(listOf(finished(entry(tag = 3)), finished(entry(tag = 2)))))
            advanceUntilIdle()

            assertThat(expectMostRecentItem().completedTags()).containsExactly(3, 2).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun TestScope.latestState(): SyncProgressUiState {
        var state: SyncProgressUiState? = null
        underTest.uiState.test {
            advanceUntilIdle()
            state = expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }
        return checkNotNull(state)
    }

    private fun SyncProgressUiState.completedTags(): List<Int> =
        (completedBody as SyncProgressCompletedBody.Transfers).items.map { it.tag }

    private fun SyncProgressUiState.rows(): List<SyncActiveTransferUiModel> =
        (activeBody as SyncProgressBody.Transfers).items
            .filterIsInstance<SyncActiveListItem.Row>()
            .map { it.model }
}
