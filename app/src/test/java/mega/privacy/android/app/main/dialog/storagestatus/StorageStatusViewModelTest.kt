package mega.privacy.android.app.main.dialog.storagestatus

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.GetAccountTypeUseCase
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabledUseCase
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
    private val getAccountTypeUseCase: GetAccountTypeUseCase = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()

    @BeforeEach
    fun resetMocks() {
        wheneverBlocking { getAccountTypeUseCase() }.thenReturn(AccountType.FREE)
        reset(getPricing, isAchievementsEnabledUseCase, getCurrentUserEmail)
    }

    private fun initTestClass() {
        underTest = StorageStatusViewModel(
            getPricing = getPricing,
            isAchievementsEnabledUseCase = isAchievementsEnabledUseCase,
            getAccountTypeUseCase = getAccountTypeUseCase,
            getCurrentUserEmail = getCurrentUserEmail
        )
    }

    @Test
    fun `test that isAchievementsEnabled updated correctly when calling isAchievementsEnabled return true`() =
        runTest {
            whenever(isAchievementsEnabledUseCase()).thenReturn(true)
            initTestClass()
            underTest.state.test {
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
                val state = awaitItem()
                Truth.assertThat(state.isAchievementsEnabled).isFalse()
            }
        }


    @Test
    fun `test that accountType updated correctly when calling getAccountTypeUseCase returns`() =
        runTest {
            whenever(getAccountTypeUseCase()).thenReturn(AccountType.PRO_I)
            initTestClass()
            underTest.state.test {
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
                val state = awaitItem()
                Truth.assertThat(state.product).isEqualTo(product)
            }
        }
}