package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaPricing
import nz.mega.sdk.MegaRequest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

class SubscriptionOptionListMapperTest {

    private val currencyMapper = ::Currency

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

    private val subscriptionOption = SubscriptionOption(
        accountType = toAccountType(1),
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(13),
        currency = currencyMapper("EUR"),
    )

    @Test
    fun `test that subscription option is mapped correctly to the list of subscription options`() {
        val actual = toSubscriptionOptionList(request, currencyMapper)
        print(actual[0])
        print(subscriptionOption)
        assertEquals(subscriptionOption, actual[0])
    }
}