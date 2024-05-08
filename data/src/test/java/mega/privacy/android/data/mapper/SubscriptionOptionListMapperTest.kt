package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaPricing
import nz.mega.sdk.MegaRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SubscriptionOptionListMapperTest {
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

    private val accountTypeMapper = mock<AccountTypeMapper>()
    private val subscriptionOption = SubscriptionOption(
        accountType = AccountType.PRO_I,
        months = 1,
        handle = 1560943707714440503,
        storage = 450,
        transfer = 450,
        amount = CurrencyPoint.SystemCurrencyPoint(13),
        currency = currencyMapper("EUR"),
    )

    private val underTest = SubscriptionOptionListMapper(
        currencyMapper,
        accountTypeMapper,
    )

    @Test
    fun `test that subscription option is mapped correctly to the list of subscription options`() {
        whenever(accountTypeMapper(1)).thenReturn(subscriptionOption.accountType)
        val actual = underTest(request)
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual).isEqualTo(listOf(subscriptionOption))
    }
}