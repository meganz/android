package mega.privacy.android.data.mapper

import com.android.billingclient.api.Purchase
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

internal class MegaPurchaseMapperTest {
    private val underTest = ::toMegaPurchase

    @Test
    fun `test that mapper returns correct value`() {
        val expectedSku = "sku"
        val expectedRecipe = "expectedRecipe"
        val expectedState = 1
        val expectedToken = "expectedToken"
        val purchase = mock<Purchase> {
            on { products }.thenReturn(listOf(expectedSku))
            on { originalJson }.thenReturn(expectedRecipe)
            on { purchaseState }.thenReturn(expectedState)
            on { purchaseToken }.thenReturn(expectedToken)
        }
        val megaPurchase = underTest(purchase)
        assertEquals(megaPurchase.sku, expectedSku)
        assertEquals(megaPurchase.receipt, expectedRecipe)
        assertEquals(megaPurchase.state, expectedState)
        assertEquals(megaPurchase.token, expectedToken)
    }
}