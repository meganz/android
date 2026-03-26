package mega.privacy.android.shared.account.overquota

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.usecase.MonitorAlmostFullStorageBannerVisibilityUseCase
import mega.privacy.android.domain.usecase.SetAlmostFullStorageBannerClosingTimestampUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.shared.account.overquota.mapper.OverQuotaStatusMapper
import mega.privacy.android.shared.account.overquota.model.OverQuotaIssue
import mega.privacy.android.shared.account.overquota.model.OverQuotaStatusUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
class OverQuotaStatusViewModelTest {

    private lateinit var underTest: OverQuotaStatusViewModel

    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase>()
    private val monitorTransferOverQuotaUseCase = mock<MonitorTransferOverQuotaUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val monitorAlmostFullStorageBannerVisibilityUseCase =
        mock<MonitorAlmostFullStorageBannerVisibilityUseCase>()
    private val setAlmostFullStorageBannerClosingTimestampUseCase =
        mock<SetAlmostFullStorageBannerClosingTimestampUseCase>()
    private val overQuotaStatusMapper = OverQuotaStatusMapper()

    @BeforeEach
    fun setUp() {
        underTest = OverQuotaStatusViewModel(
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorAlmostFullStorageBannerVisibilityUseCase = monitorAlmostFullStorageBannerVisibilityUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase = setAlmostFullStorageBannerClosingTimestampUseCase,
            overQuotaStatusMapper = overQuotaStatusMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorStorageStateUseCase,
            monitorTransferOverQuotaUseCase,
            monitorAccountDetailUseCase,
            monitorAlmostFullStorageBannerVisibilityUseCase,
            setAlmostFullStorageBannerClosingTimestampUseCase,
        )
    }

    @Test
    fun `test that initial state is Loading`() = runTest {
        assertThat(underTest.uiState.value).isEqualTo(OverQuotaStatusUiState.Loading)
    }

    @Test
    fun `test that uiState emits storage full when storage state is Red`() = runTest {
        stubFlows(storageState = StorageState.Red)

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.overQuotaStatus.storage).isEqualTo(OverQuotaIssue.Storage.Full)
        }
    }

    @Test
    fun `test that uiState emits storage almost full when storage state is Orange`() = runTest {
        stubFlows(storageState = StorageState.Orange)

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.overQuotaStatus.storage).isEqualTo(OverQuotaIssue.Storage.AlmostFull)
        }
    }

    @Test
    fun `test that uiState emits transfer over quota when transfer over quota is true and account is paid`() =
        runTest {
            stubFlows(transferOverQuota = true, isPaid = true)

            underTest.uiState.test {
                val state = awaitDataState()
                assertThat(state.overQuotaStatus.transfer)
                    .isEqualTo(OverQuotaIssue.Transfer.TransferOverQuota)
            }
        }

    @Test
    fun `test that uiState emits transfer over quota free user when transfer over quota is true and account is free`() =
        runTest {
            stubFlows(transferOverQuota = true, isPaid = false)

            underTest.uiState.test {
                val state = awaitDataState()
                assertThat(state.overQuotaStatus.transfer)
                    .isEqualTo(OverQuotaIssue.Transfer.TransferOverQuotaFreeUser)
            }
        }

    @Test
    fun `test that uiState emits shouldShowWarning from banner visibility use case`() = runTest {
        stubFlows(shouldShowWarning = false)

        underTest.uiState.test {
            val state = awaitDataState()
            assertThat(state.shouldShowWarning).isFalse()
        }
    }

    @Test
    fun `test that dismissWarning calls setAlmostFullStorageBannerClosingTimestampUseCase`() =
        runTest {
            stubFlows()

            underTest.uiState.test {
                awaitDataState()
                underTest.dismissWarning()
                verify(setAlmostFullStorageBannerClosingTimestampUseCase).invoke()
            }
        }

    private fun stubFlows(
        storageState: StorageState = StorageState.Green,
        transferOverQuota: Boolean = false,
        isPaid: Boolean = true,
        shouldShowWarning: Boolean = true,
    ) {
        monitorStorageStateUseCase.stub {
            on { invoke() } doReturn flow {
                emit(storageState)
                awaitCancellation()
            }
        }
        monitorTransferOverQuotaUseCase.stub {
            on { invoke() } doReturn flow {
                emit(transferOverQuota)
                awaitCancellation()
            }
        }

        val accountType = if (isPaid) AccountType.PRO_I else AccountType.FREE
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        monitorAccountDetailUseCase.stub {
            on { invoke() } doReturn flow {
                emit(
                    accountDetail
                )
                awaitCancellation()
            }
        }
        monitorAlmostFullStorageBannerVisibilityUseCase.stub {
            on { invoke() } doReturn flow {
                emit(shouldShowWarning)
                awaitCancellation()
            }
        }
    }

    private suspend fun ReceiveTurbine<OverQuotaStatusUiState>.awaitDataState(): OverQuotaStatusUiState.Data {
        var item = awaitItem()
        while (item !is OverQuotaStatusUiState.Data) {
            item = awaitItem()
        }
        return item
    }
}
