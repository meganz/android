package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaPricing
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

internal class PricingMapperTest {
    @Test
    fun `test that mapper returns correct value `() {
        val megaPricing = mock<MegaPricing>()
        val megaCurrency = mock<MegaCurrency>()
        val expectedNumProducts = 3
        val expectedCurrency = "USD"
        val baseHandle = 1000L
        val baseMonth = 12
        val baseStorage = 1212
        val baseTransfer = 12134
        whenever(megaPricing.numProducts).thenReturn(expectedNumProducts)
        whenever(megaCurrency.currencyName).thenReturn(expectedCurrency)
        (0 until expectedNumProducts).forEach { i ->
            whenever(megaPricing.getHandle(i)).thenReturn(baseHandle + i)
            whenever(megaPricing.getProLevel(i)).thenReturn(i)
            whenever(megaPricing.getMonths(i)).thenReturn(baseMonth + i)
            whenever(megaPricing.getGBStorage(i)).thenReturn(baseStorage + i)
            whenever(megaPricing.getGBTransfer(i)).thenReturn(baseTransfer + i)
        }
        val pricing = toPricing(megaPricing, megaCurrency)
        assertEquals(pricing.products.size, expectedNumProducts)
        pricing.products.forEachIndexed { index, item ->
            assertEquals(item.handle, baseHandle + index)
            assertEquals(item.level, index)
            assertEquals(item.months, baseMonth + index)
            assertEquals(item.storage, baseStorage + index)
            assertEquals(item.transfer, baseTransfer + index)
        }
    }
}