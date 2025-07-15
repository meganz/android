package mega.privacy.android.domain.usecase.transfers.overquota

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorStorageOverQuotaUseCaseTest {

    private lateinit var underTest: MonitorStorageOverQuotaUseCase

    private val transferRepository = mock<TransferRepository>()
    private val storageStateEvent = mock<StorageStateEvent> {
        on { storageState }.thenReturn(StorageState.Unknown)
    }
    private val monitorStorageStateEventUseCase = mock<MonitorStorageStateEventUseCase> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(storageStateEvent)
        )
    }

    @BeforeAll
    fun setup() {
        underTest = MonitorStorageOverQuotaUseCase(
            transferRepository = transferRepository,
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
        whenever(monitorStorageStateEventUseCase()) doReturn MutableStateFlow(storageStateEvent)
    }

    @ParameterizedTest(name = " if initial value of storageState is {0}")
    @EnumSource(StorageState::class)
    fun `test that invoke returns correctly`(
        storageState: StorageState,
    ) = runTest {
        val expected = storageState == StorageState.Red || storageState == StorageState.PayWall
        val storageStateEvent = StorageStateEvent(
            handle = 1L,
            storageState = storageState
        )
        val storageStateFlow = MutableStateFlow(storageStateEvent)

        whenever(transferRepository.monitorStorageOverQuota())
            .thenReturn(flowOf(false))
        whenever(monitorStorageStateEventUseCase()).thenReturn(storageStateFlow)

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
//            assertThat(awaitItem()).isTrue()

            StorageState.entries.forEach { newStorageState ->
                if (newStorageState != storageState) {
                    val newExpected =
                        newStorageState == StorageState.Red || newStorageState == StorageState.PayWall
                    storageStateFlow.emit(storageStateEvent.copy(storageState = newStorageState))
                    assertThat(awaitItem()).isEqualTo(newExpected)
                }
            }
        }

        whenever(transferRepository.monitorStorageOverQuota())
            .thenReturn(flowOf(true))

        underTest().test {
            assertThat(awaitItem()).isTrue()
            cancelAndConsumeRemainingEvents()
        }
    }
}