package mega.privacy.android.feature.payment

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.advertisements.MonitorAdsClosingTimestampUseCase
import mega.privacy.android.feature.payment.presentation.storage.AccountStorageViewModel
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountStorageViewModelTest {

    private lateinit var underTest: AccountStorageViewModel

    private val totalStorage: Long = 21474836480
    private val accountDetailsFlow = flowOf(AccountDetail())

    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase> {
        on {
            invoke()
        }.thenReturn(accountDetailsFlow)
    }
    private val getAccountAchievements: GetAccountAchievements = mock()
    private val monitorAdsClosingTimestampUseCase: MonitorAdsClosingTimestampUseCase = mock {
        on { invoke() }.thenReturn(flowOf(0L))
    }

    @BeforeAll
    internal fun setUp() {
        init()
    }

    private fun init() {
        underTest = AccountStorageViewModel(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getAccountAchievements = getAccountAchievements,
            monitorAdsClosingTimestampUseCase = monitorAdsClosingTimestampUseCase
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(accountDetailsFlow)
    }

    @Test
    fun `test that monitorAccountDetail works correctly`() =
        runTest {
            val accountStorageDetail = mock<AccountStorageDetail> {
                on { totalStorage } doReturn totalStorage
                on { usedPercentage } doReturn 50
            }
            val accountDetail = mock<AccountDetail> {
                on { storageDetail } doReturn accountStorageDetail
            }
            whenever(monitorAccountDetailUseCase()).thenReturn(
                flowOf(accountDetail)
            )
            underTest.monitorAccountDetail()
            underTest.state.test {
                val item = awaitItem()
                assertThat(item.totalStorage).isEqualTo(totalStorage)
                assertThat(item.storageUsedPercentage).isEqualTo(50)
            }
        }

    @Test
    fun `test that last ads closing timestamp is updated correctly`() =
        runTest {
            val timestamp = 123456789L
            whenever(monitorAdsClosingTimestampUseCase()).thenReturn(
                flowOf(timestamp)
            )
            init()
            underTest.state.test {
                assertThat(awaitItem().lastAdsClosingTimestamp).isEqualTo(timestamp)
            }
        }
}