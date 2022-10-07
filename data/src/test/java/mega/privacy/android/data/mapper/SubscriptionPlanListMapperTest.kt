package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SubscriptionPlan
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaPricing
import nz.mega.sdk.MegaRequest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

class SubscriptionPlanListMapperTest {

    private val subscriptionPlanMapper = ::SubscriptionPlan
    private val currencyMapper = mock<CurrencyMapper>()
    private val skuMapper = mock<SkuMapper>()

    private val pricing = mock<MegaPricing> {
        on { numProducts }.thenReturn(1)
        on { getHandle(0) }.thenReturn(1560943707714440503)
        on { getProLevel(0) }.thenReturn(1)
        on { getMonths(0) }.thenReturn(1)
        on { getGBStorage(0) }.thenReturn(450)
        on { getGBTransfer(0) }.thenReturn(450)
        on { getAmount(0) }.thenReturn(13)
    }

    private val currency = mock<MegaCurrency> {
        on { currencyName }.thenReturn("EUR")
    }

    private val request = mock<MegaRequest> {
        on { pricing }.thenReturn(pricing)
        on { currency }.thenReturn(currency)
    }

    private val subscriptionPlan = SubscriptionPlan(
        handle = 1560943707714440503,
        level = toAccountType(1),
        months = 1,
        storage = 450,
        transfer = 450,
        amount = 13,
        currency = currencyMapper("EUR"),
        sku = skuMapper(toAccountType(1), 1)
    )

    @Test
    fun `test that subscription plan is mapped correctly to the list of subscription plans`() {
        val actual = toSubscriptionPlanList(request,
            subscriptionPlanMapper,
            currencyMapper,
            skuMapper)
        assertEquals(subscriptionPlan, actual[0])
    }
}