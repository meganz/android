package mega.privacy.android.data.repository

import com.google.common.truth.Truth
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.LocalPricingMapper
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.BillingRepository
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@kotlinx.coroutines.ExperimentalCoroutinesApi
class DefaultBillingRepositoryTest {
    private lateinit var underTest: BillingRepository

    private val accountInfoWrapper = mock<AccountInfoWrapper>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val paymentMethodFlagsCache = mock<Cache<PaymentMethodFlags>>()
    private val megaSkuObject1 = MegaSku("mega.android.pro1.onemonth", 9990000, "EUR")
    private val megaSkuObject2 = MegaSku("mega.android.pro2.onemonth", 9990000, "EUR")
    private val skuString = "mega.android.pro1.onemonth"
    private val megaSkuList = listOf(megaSkuObject2, megaSkuObject1)
    private val localPricing =
        LocalPricing(CurrencyPoint.LocalCurrencyPoint(9990000), Currency("EUR"), skuString)
    private val localPricingMapper = mock<LocalPricingMapper>()

    @Before
    fun setUp() {
        underTest = DefaultBillingRepository(
            accountInfoWrapper = accountInfoWrapper,
            megaApiGateway = megaApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            paymentMethodFlagsCache = paymentMethodFlagsCache,
            localPricingMapper = localPricingMapper,
        )
    }

    @Test
    fun `test that get local pricing returns successfully`() =
        runTest {
            whenever(accountInfoWrapper.availableSkus).thenReturn(megaSkuList)
            whenever(localPricingMapper(megaSkuObject1)).thenReturn(localPricing)

            val actual = underTest.getLocalPricing(skuString)

            Truth.assertThat(actual).isEqualTo(localPricing)
        }

    @Test
    fun `test that getPaymentMethods returns success when MegaApi returns ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_PAYMENT_METHODS)
            }

            whenever(megaApiGateway.getPaymentMethods(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.getPaymentMethod(true)
        }

    @Test(expected = MegaException::class)
    fun `test that getPaymentMethods finishes with general MegaException when MegaChatApi returns errors other than ERROR_ACCESS or ERROR_OK`() =
        runTest {
            val megaError = mock<MegaError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_NOENT)
            }

            val megaRequest = mock<MegaRequest> {
                on { type }.thenReturn(MegaRequest.TYPE_GET_PAYMENT_METHODS)
            }

            whenever(megaApiGateway.getPaymentMethods(listener = any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    megaRequest,
                    megaError,
                )
            }

            underTest.getPaymentMethod(true)
        }
}