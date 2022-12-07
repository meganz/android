package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.LocalPricing
import mega.privacy.android.domain.entity.account.CurrencyPoint
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_I_MONTH
import org.junit.Assert.*
import org.junit.Test

class LocalPricingMapperTest {

    private val sku = SKU_PRO_I_MONTH
    private val currency = Currency("EUR")
    private val amount = CurrencyPoint.LocalCurrencyPoint(9.99.toLong())

    private val megaSku =
        MegaSku(sku = SKU_PRO_I_MONTH, priceAmountMicros = 9.99.toLong(), priceCurrencyCode = "EUR")

    @Test
    fun `test that local pricing is mapped correctly`() {
        val expectedResult = LocalPricing(amount, currency, sku)

        assertEquals(expectedResult, toLocalPricing(megaSku))
    }
}