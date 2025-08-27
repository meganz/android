package mega.privacy.android.app.presentation.transfers.transferoverquota

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorBandwidthOverQuotaDelayUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.UpdateTransferOverQuotaTimestampUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransferOverQuotaViewModelTest {

    private lateinit var underTest: TransferOverQuotaViewModel

    private val hasCredentialsUseCase = mock<HasCredentialsUseCase>()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock {
        onBlocking { invoke() } doReturn emptyFlow()
    }
    private val monitorBandwidthOverQuotaDelayUseCase: MonitorBandwidthOverQuotaDelayUseCase =
        mock {
            onBlocking { invoke() } doReturn emptyFlow()
        }
    private val updateTransferOverQuotaTimestampUseCase =
        mock<UpdateTransferOverQuotaTimestampUseCase>()

    private suspend fun initTest(
        hasCredentials: Boolean = true,
        accountDetail: AccountDetail = mock(),
        overQuotaDelay: Duration? = null,
    ) {
        whenever(hasCredentialsUseCase()) doReturn hasCredentials
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
        whenever(monitorBandwidthOverQuotaDelayUseCase()) doReturn flowOf(overQuotaDelay)

        underTest = TransferOverQuotaViewModel(
            hasCredentialsUseCase = hasCredentialsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorBandwidthOverQuotaDelayUseCase = monitorBandwidthOverQuotaDelayUseCase,
            updateTransferOverQuotaTimestampUseCase = updateTransferOverQuotaTimestampUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(hasCredentialsUseCase, updateTransferOverQuotaTimestampUseCase)

        wheneverBlocking { monitorAccountDetailUseCase() } doReturn emptyFlow()
        wheneverBlocking { monitorBandwidthOverQuotaDelayUseCase() } doReturn emptyFlow()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that isLoggedIn is updated when checking if user has credentials`(
        hasCredentials: Boolean,
    ) = runTest {
        initTest(hasCredentials = hasCredentials)

        underTest.uiState.map { it.isLoggedIn }.test {
            assertThat(awaitItem()).isEqualTo(hasCredentials)
        }
    }

    @ParameterizedTest
    @EnumSource(AccountType::class)
    fun `test that isFreeAccount is updated when checking account details`(
        accountType: AccountType,
    ) = runTest {
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { this.accountType } doReturn accountType
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        val isFreeAccount = (accountDetail.levelDetail?.accountType)
            ?.let { it == AccountType.FREE } ?: true

        initTest(accountDetail = accountDetail)

        underTest.uiState.map { it.isFreeAccount }.test {
            assertThat(awaitItem()).isEqualTo(isFreeAccount)
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = [1, 100, 1000, 10000])
    fun `test that bandwidthOverQuotaDelay is updated when monitoring bandwidth over quota dealy`(
        overQuotaDelay: Long?,
    ) = runTest {
        val bandwidthOverQuotaDelay = overQuotaDelay?.seconds

        initTest(overQuotaDelay = bandwidthOverQuotaDelay)

        underTest.uiState.map { it.bandwidthOverQuotaDelay }.test {
            assertThat(awaitItem()).isEqualTo(bandwidthOverQuotaDelay)
        }
    }

    @Test
    fun `test that bandwidthOverQuotaDelay is set to null when it is consumed and updateTransferOverQuotaTimestampUseCase is invoked`() =
        runTest {
            val overQuotaDelay = 100L.seconds

            initTest(overQuotaDelay = overQuotaDelay)

            underTest.bandwidthOverQuotaDelayConsumed()

            underTest.uiState.map { it.bandwidthOverQuotaDelay }.test {
                assertThat(awaitItem()).isNull()
            }
            verify(updateTransferOverQuotaTimestampUseCase).invoke()
        }

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}