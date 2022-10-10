package test.mega.privacy.android.app.data.mapper

import mega.privacy.android.app.data.mapper.toSkuMapper
import mega.privacy.android.app.service.iab.BillingManagerImpl
import mega.privacy.android.domain.entity.AccountType
import org.junit.Assert.*
import org.junit.Test

class SkuMapperTest {
    @Test
    fun `test that sku type for monthly subscription can be mapped correctly`() {
        val expectedResults = HashMap<AccountType?, String?>().apply {
            put(AccountType.PRO_LITE, BillingManagerImpl.SKU_PRO_LITE_MONTH)
            put(AccountType.PRO_I, BillingManagerImpl.SKU_PRO_I_MONTH)
            put(AccountType.PRO_II, BillingManagerImpl.SKU_PRO_II_MONTH)
            put(AccountType.PRO_III, BillingManagerImpl.SKU_PRO_III_MONTH)
            put(null, null)
        }
        expectedResults.forEach { (key, value) ->
            val actual = toSkuMapper(key, 1)
            assertEquals(value, actual)
        }
    }

    @Test
    fun `test that sku type for yearly subscription can be mapped correctly`() {
        val expectedResults = HashMap<AccountType?, String?>().apply {
            put(AccountType.PRO_LITE, BillingManagerImpl.SKU_PRO_LITE_YEAR)
            put(AccountType.PRO_I, BillingManagerImpl.SKU_PRO_I_YEAR)
            put(AccountType.PRO_II, BillingManagerImpl.SKU_PRO_II_YEAR)
            put(AccountType.PRO_III, BillingManagerImpl.SKU_PRO_III_YEAR)
            put(null, null)
        }
        expectedResults.forEach { (key, value) ->
            val actual = toSkuMapper(key, 12)
            assertEquals(value, actual)
        }
    }
}