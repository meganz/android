package mega.privacy.android.app.main.dialog.storagestatus

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.feature.payment.model.AccountTypeInt
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StorageStatusViewModelTest {
    private lateinit var underTest: StorageStatusViewModel
    private val getPricing: GetPricing = mock()
    private val isAchievementsEnabledUseCase: IsAchievementsEnabledUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val accountDetailFlow = MutableSharedFlow<AccountDetail>()

    @BeforeEach
    fun resetMocks() {
        reset(getPricing, isAchievementsEnabledUseCase, getCurrentUserEmail)
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)
        wheneverBlocking { isAchievementsEnabledUseCase() }.thenReturn(false)
        wheneverBlocking { getPricing(false) }.thenReturn(Pricing(emptyList()))
    }

    private fun initTestClass() {
        underTest = StorageStatusViewModel(
            getPricing = getPricing,
            isAchievementsEnabledUseCase = isAchievementsEnabledUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getCurrentUserEmail = getCurrentUserEmail
        )
    }

    @Test
    fun `test that isAchievementsEnabled updated correctly when calling isAchievementsEnabled return true`() =
        runTest {
            whenever(isAchievementsEnabledUseCase()).thenReturn(true)
            initTestClass()
            underTest.state.test {
                awaitItem()
                accountDetailFlow.emit(AccountDetail())
                val state = awaitItem()
                Truth.assertThat(state.isAchievementsEnabled).isTrue()
            }
        }

    @Test
    fun `test that isAchievementsEnabled updated correctly when calling isAchievementsEnabled return false`() =
        runTest {
            whenever(isAchievementsEnabledUseCase()).thenReturn(false)
            initTestClass()
            underTest.state.test {
                awaitItem()
                accountDetailFlow.emit(AccountDetail())
                val state = awaitItem()
                Truth.assertThat(state.isAchievementsEnabled).isFalse()
            }
        }

    @Test
    fun `test that accountType updated correctly when monitorAccountDetailUseCase emits`() =
        runTest {
            initTestClass()
            underTest.state.test {
                awaitItem()
                val accountLevelDetail = mock<AccountLevelDetail> {
                    on { accountType }.thenReturn(AccountType.PRO_I)
                }
                accountDetailFlow.emit(AccountDetail(levelDetail = accountLevelDetail))
                val state = awaitItem()
                Truth.assertThat(state.accountType).isEqualTo(AccountType.PRO_I)
            }
        }

    @Test
    fun `test that product updated correctly when calling getPricing returns PRO_III product`() =
        runTest {
            val product = mock<Product> {
                on { level }.thenReturn(AccountTypeInt.PRO_III)
                on { months }.thenReturn(1)
            }
            val pricing = mock<Pricing> {
                on { products }.thenReturn(listOf(product))
            }
            whenever(getPricing(false)).thenReturn(pricing)
            initTestClass()
            underTest.state.test {
                awaitItem()
                accountDetailFlow.emit(AccountDetail())
                val state = awaitItem()
                Truth.assertThat(state.product).isEqualTo(product)
            }
        }

    @Test
    fun `test that isLoading is true initially`() =
        runTest {
            initTestClass()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isLoading).isTrue()
            }
        }

    @Test
    fun `test that isLoading is false after data is loaded`() =
        runTest {
            initTestClass()
            underTest.state.test {
                awaitItem()
                accountDetailFlow.emit(AccountDetail())
                val state = awaitItem()
                Truth.assertThat(state.isLoading).isFalse()
            }
        }
}
