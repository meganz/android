package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.sync.domain.entity.progress.SyncMediaType
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferDirection
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferEntry
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferError
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferRow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorRetryingSyncTransferOverQuotaUseCaseTest {

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(UnconfinedTestDispatcher())
    }

    private val monitorSyncTransferStoreUseCase = mock<MonitorSyncTransferStoreUseCase>()
    private val getSyncTransferRowStateUseCase = spy(GetSyncTransferRowStateUseCase())

    private lateinit var underTest: MonitorRetryingSyncTransferOverQuotaUseCase

    @BeforeAll
    fun setUp() {
        underTest = MonitorRetryingSyncTransferOverQuotaUseCase(
            monitorSyncTransferStoreUseCase = monitorSyncTransferStoreUseCase,
            getSyncTransferRowStateUseCase = getSyncTransferRowStateUseCase,
            defaultDispatcher = extension.testDispatcher,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(monitorSyncTransferStoreUseCase)
        clearInvocations(getSyncTransferRowStateUseCase)
    }

    @Test
    fun `test that invoke emits false when the store is not seeded`() = runTest {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(flowOf(SyncTransferStoreState.NotSeeded))

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits false when the store has no rows`() = runTest {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(flowOf(store()))

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits true when a retrying row has the transfer quota error`() = runTest {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(
            flowOf(store(row(tag = 1), row(tag = 2, state = TransferState.STATE_RETRYING, error = transferQuotaError))),
        )

        underTest().test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits false when a retrying row has the storage quota error`() = runTest {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(
            flowOf(store(row(tag = 1, state = TransferState.STATE_RETRYING, error = storageQuotaError))),
        )

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits false when a queued row has the transfer quota error`() = runTest {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(
            flowOf(store(row(tag = 1, state = TransferState.STATE_QUEUED, error = transferQuotaError))),
        )

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke emits false when a retrying row has another error code`() = runTest {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(
            flowOf(
                store(
                    row(
                        tag = 1,
                        state = TransferState.STATE_RETRYING,
                        error = SyncTransferError(code = -3, value = 1),
                    )
                )
            ),
        )

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke does not emit again while the result is unchanged`() = runTest {
        val storeFlow = MutableStateFlow(store(row(tag = 1)))
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(storeFlow)

        underTest().test {
            assertThat(awaitItem()).isFalse()
            storeFlow.value = store(row(tag = 1), row(tag = 2))
            storeFlow.value = store(row(tag = 2, state = TransferState.STATE_RETRYING, error = storageQuotaError))
            expectNoEvents()
        }
    }

    @Test
    fun `test that invoke emits false when the over quota row leaves the store`() = runTest {
        val overQuotaRow = row(tag = 1, state = TransferState.STATE_RETRYING, error = transferQuotaError)
        val storeFlow = MutableStateFlow(store(overQuotaRow, row(tag = 2)))
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(storeFlow)

        underTest().test {
            assertThat(awaitItem()).isTrue()
            storeFlow.value = store(row(tag = 2))
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that invoke emits true when a retrying row over transfer quota has an unconfirmed pause`() =
        runTest {
            whenever(monitorSyncTransferStoreUseCase()).thenReturn(
                flowOf(
                    store(
                        row(
                            tag = 1,
                            state = TransferState.STATE_RETRYING,
                            error = transferQuotaError,
                            optimisticPause = true,
                        )
                    )
                ),
            )

            underTest().test {
                assertThat(awaitItem()).isTrue()
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke computes the row state only for retrying rows`() = runTest {
        whenever(monitorSyncTransferStoreUseCase()).thenReturn(
            flowOf(
                store(
                    row(tag = 1),
                    row(tag = 2, state = TransferState.STATE_QUEUED, error = transferQuotaError),
                    row(tag = 3, state = TransferState.STATE_RETRYING, error = storageQuotaError),
                )
            ),
        )

        underTest().test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }

        verify(getSyncTransferRowStateUseCase, times(1)).invoke(any(), any(), anyOrNull())
    }

    private fun store(vararg rows: SyncTransferRow) = SyncTransferStoreState.NotSeeded.copy(
        isSeeded = true,
        active = rows.associateBy { it.entry.tag }.toPersistentHashMap(),
    )

    private fun row(
        tag: Int,
        state: TransferState = TransferState.STATE_ACTIVE,
        error: SyncTransferError? = null,
        optimisticPause: Boolean? = null,
    ) = SyncTransferRow(
        entry = SyncTransferEntry(
            tag = tag,
            direction = SyncTransferDirection.Download,
            state = state,
            fileName = "file_$tag.jpg",
            totalBytes = 1_000L,
            transferredBytes = 400L,
            speed = 100L,
            meanSpeed = 0L,
            priority = tag.toLong(),
            notificationNumber = tag.toLong(),
            lastError = error,
            isForeignOverQuota = false,
            path = null,
        ),
        mediaType = SyncMediaType.Image,
        optimisticPause = optimisticPause,
    )

    private val transferQuotaError = SyncTransferError(code = SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE, value = 1)
    private val storageQuotaError = SyncTransferError(code = SYNC_TRANSFER_OVER_QUOTA_ERROR_CODE, value = 0)
}
