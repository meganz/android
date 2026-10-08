package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferCounts
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferFilter
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferList
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferListItem
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRowContext
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStatus
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncTransferListUseCaseTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(UnconfinedTestDispatcher())
    }

    private val monitorSyncTransferStoreUseCase = mock<MonitorSyncTransferStoreUseCase>()
    private val monitorPausedTransfersUseCase = mock<MonitorPausedTransfersUseCase>()
    private val monitorSyncProgressBlockersUseCase = mock<MonitorSyncProgressBlockersUseCase>()
    private val getSyncTransferRowStateUseCase = spy(GetSyncTransferRowStateUseCase())

    private lateinit var underTest: MonitorSyncTransferListUseCase

    @BeforeAll
    fun setUp() {
        underTest = MonitorSyncTransferListUseCase(
            monitorSyncTransferStoreUseCase = monitorSyncTransferStoreUseCase,
            monitorPausedTransfersUseCase = monitorPausedTransfersUseCase,
            monitorSyncProgressBlockersUseCase = monitorSyncProgressBlockersUseCase,
            getSyncTransferListUseCase = GetSyncTransferListUseCase(
                getSyncTransferRowStateUseCase = getSyncTransferRowStateUseCase,
                getSyncTransferFilterCountsUseCase =
                    GetSyncTransferFilterCountsUseCase(IsSyncTransferSearchMatchUseCase()),
                isSyncTransferSearchMatchUseCase = IsSyncTransferSearchMatchUseCase(),
            ),
            defaultDispatcher = extension.testDispatcher,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorSyncTransferStoreUseCase,
            monitorPausedTransfersUseCase,
            monitorSyncProgressBlockersUseCase,
        )
        clearInvocations(getSyncTransferRowStateUseCase)
    }

    @Test
    fun `test that invoke emits the projection of the store for the filter`() = runTest {
        val transferQuotaError =
            SyncTransferError(code = SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE, value = 3_600)
        val state = storeState(
            listOf(
                row(1, lastError = transferQuotaError),
                row(2, state = TransferState.STATE_ACTIVE),
            )
        )
        stubInputs(
            store = MutableStateFlow(state),
            blockers = MutableStateFlow(blockers(isTransferOverQuota = true)),
        )
        val expectedContext = SyncTransferRowContext(
            areTransfersPaused = false,
            isStorageOverQuota = false,
            isTransferOverQuota = true,
        )

        underTest(SyncTransferFilter.Pending).test {
            runCurrent()
            val actual = awaitItem()

            assertThat(actual).isEqualTo(
                GetSyncTransferListUseCase(
                    GetSyncTransferRowStateUseCase(),
                    GetSyncTransferFilterCountsUseCase(IsSyncTransferSearchMatchUseCase()),
                    IsSyncTransferSearchMatchUseCase(),
                )(state, SyncTransferFilter.Pending, expectedContext)
            )
            assertThat(actual.firstStatus).isEqualTo(SyncTransferStatus.OutOfTransferQuota)
        }
        verify(getSyncTransferRowStateUseCase, times(2))
            .invoke(any(), eq(expectedContext), anyOrNull())
    }

    @Test
    fun `test that invoke re-projects when the paused flag changes`() = runTest {
        val paused = MutableStateFlow(false)
        stubInputs(store = MutableStateFlow(storeState(listOf(row(1)))), paused = paused)

        underTest(SyncTransferFilter.Pending).test {
            runCurrent()
            assertThat(awaitItem().firstStatus).isEqualTo(SyncTransferStatus.Queued)

            paused.value = true

            assertThat(awaitItem().firstStatus).isEqualTo(SyncTransferStatus.PausedInQueue)
        }
    }

    @Test
    fun `test that invoke re-projects when the storage quota blocker changes`() = runTest {
        val blockers = MutableStateFlow(blockers())
        val overQuota = SyncTransferError(code = SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE, value = 0)
        stubInputs(
            store = MutableStateFlow(storeState(listOf(row(1, lastError = overQuota)))),
            blockers = blockers,
        )

        underTest(SyncTransferFilter.Pending).test {
            runCurrent()
            assertThat(awaitItem().firstStatus).isEqualTo(SyncTransferStatus.Queued)

            blockers.value = blockers(isStorageOverQuota = true)

            assertThat(awaitItem().firstStatus).isEqualTo(SyncTransferStatus.OutOfStorageSpace)
        }
    }

    @Test
    fun `test that invoke does not re-project when only the stalled issue count changes`() =
        runTest {
            val blockers = MutableStateFlow(blockers())
            stubInputs(store = MutableStateFlow(storeState(listOf(row(1)))), blockers = blockers)

            underTest(SyncTransferFilter.Pending).test {
                runCurrent()
                awaitItem()

                blockers.value = blockers(stalledIssueCount = 3)
                runCurrent()

                expectNoEvents()
            }
            verify(getSyncTransferRowStateUseCase, times(1)).invoke(any(), any(), anyOrNull())
        }

    @Test
    fun `test that invoke does not emit an unchanged list`() = runTest {
        val store = MutableSharedFlow<SyncTransferStoreState>()
        val state = storeState(listOf(row(1)))
        stubInputs(store = store)

        underTest(SyncTransferFilter.Pending).test {
            runCurrent()
            store.emit(state)
            awaitItem()

            store.emit(state)
            runCurrent()

            expectNoEvents()
        }
    }

    @Test
    fun `test that invoke returns every row when the limit is raised`() = runTest {
        stubInputs(store = MutableStateFlow(storeState((1..600).map { row(it) })))

        underTest(SyncTransferFilter.Pending, limit = Int.MAX_VALUE).test {
            runCurrent()
            val actual = awaitItem()

            assertThat(actual.items).hasSize(600)
            assertThat(actual.totalCount).isEqualTo(600)
        }
    }

    private fun stubInputs(
        store: Flow<SyncTransferStoreState>,
        paused: MutableStateFlow<Boolean> = MutableStateFlow(false),
        blockers: MutableStateFlow<SyncProgressBlockers> = MutableStateFlow(blockers()),
    ) {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(store)
        whenever(monitorPausedTransfersUseCase()).thenReturn(paused)
        whenever(monitorSyncProgressBlockersUseCase()).thenReturn(blockers)
    }

    private val SyncTransferList.firstStatus
        get() = (items.first() as SyncTransferListItem.Active).status

    private fun blockers(
        isStorageOverQuota: Boolean = false,
        isTransferOverQuota: Boolean = false,
        stalledIssueCount: Int = 0,
    ) = SyncProgressBlockers(
        isOffline = false,
        isStorageOverQuota = isStorageOverQuota,
        isTransferOverQuota = isTransferOverQuota,
        areTransfersPaused = false,
        stalledIssueCount = stalledIssueCount,
    )

    private fun row(
        tag: Int,
        state: TransferState = TransferState.STATE_QUEUED,
        lastError: SyncTransferError? = null,
    ) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = tag,
            direction = SyncTransferDirection.Upload,
            state = state,
            fileName = "f$tag.txt",
            totalBytes = 100,
            transferredBytes = 0,
            speed = 0,
            meanSpeed = 0,
            priority = tag.toLong(),
            notificationNumber = tag.toLong(),
            lastError = lastError,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Document,
        optimisticPause = null,
    )

    private fun storeState(active: List<SyncTransferRow>) = SyncTransferStoreState(
        isSeeded = true,
        active = active.associateBy { it.entry.tag }.toPersistentHashMap(),
        overflow = persistentHashMapOf(),
        completed = persistentListOf(),
        counts = SyncTransferCounts.Empty,
        reseedRemoved = persistentHashSetOf(),
    )
}
