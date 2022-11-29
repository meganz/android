package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.repository.BillingRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@kotlinx.coroutines.ExperimentalCoroutinesApi
class DefaultBillingRepositoryTest {
    private lateinit var underTest: BillingRepository

    private val accountInfoWrapper = mock<AccountInfoWrapper>()
    private val megaSkuObject1 = MegaSku("mega.android.pro1.onemonth", 9990000, "EUR")
    private val megaSkuObject2 = MegaSku("mega.android.pro2.onemonth", 9990000, "EUR")
    private val skuString = "mega.android.pro1.onemonth"
    private val megaSkuList = listOf(megaSkuObject2, megaSkuObject1)

    @Before
    fun setUp() {
        underTest = DefaultBillingRepository(
            accountInfoWrapper = accountInfoWrapper,
        )
    }

    @Test
    fun `test that get local pricing returns successfully`() =
        runTest {
            whenever(accountInfoWrapper.availableSkus).thenReturn(megaSkuList)

            val actual = underTest.getLocalPricing(skuString)

            Truth.assertThat(actual).isEqualTo(megaSkuObject1)
        }
}