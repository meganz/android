package test.mega.privacy.android.app.presentation.account

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.account.AccountStorageViewModel
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.achievement.AchievementsOverview
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
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
    private val getStringFromStringResMapper: GetStringFromStringResMapper = mock()
    private val getAccountAchievements: GetAccountAchievements = mock()


    @BeforeAll
    internal fun setUp() {
        init()
    }

    private fun init() {
        underTest = AccountStorageViewModel(
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getStringFromStringResMapper = getStringFromStringResMapper,
            getAccountAchievements = getAccountAchievements
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(
            getStringFromStringResMapper,
        )
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(accountDetailsFlow)
    }

    @Test
    fun `test that monitorAccountDetail works correctly`() =
        runTest {
            val accountStorageDetail = mock<AccountStorageDetail> {
                on { totalStorage } doReturn totalStorage
            }
            val accountDetail = mock<AccountDetail> {
                on { storageDetail } doReturn accountStorageDetail
            }
            whenever(monitorAccountDetailUseCase()).thenReturn(
                flowOf(accountDetail)
            )
            underTest.monitorAccountDetail()
            underTest.state.test {
                assertThat(awaitItem().totalStorage).isEqualTo(totalStorage)
            }
        }
}