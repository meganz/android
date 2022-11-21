package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.repository.DefaultBillingRepository
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.app.utils.wrapper.PaymentUtilsWrapper
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

    private val myAccountInfo = mock<MyAccountInfo>()
    private val paymentUtilsWrapper = mock<PaymentUtilsWrapper>()
    private val megaSkuObject = MegaSku("mega.android.pro1.onemonth", 9990000, "EUR")
    private val skuString = "mega.android.pro1.onemonth"
    private val megaSkuList = listOf(MegaSku("mega.android.pro1.onemonth", 9990000, "EUR"))

    @Before
    fun setUp() {
        underTest = DefaultBillingRepository(
            myAccountInfo = myAccountInfo,
            paymentUtilsWrapper = paymentUtilsWrapper,
        )
    }

    @Test
    fun `test that get local pricing returns successfully`() =
        runTest {

            whenever(paymentUtilsWrapper.getSkuDetails(megaSkuList, skuString)).thenReturn(
                megaSkuObject)
            whenever(myAccountInfo.availableSkus).thenReturn(megaSkuList)

            val actual = underTest.getLocalPricing(skuString)

            Truth.assertThat(actual).isEqualTo(megaSkuObject)
        }
}