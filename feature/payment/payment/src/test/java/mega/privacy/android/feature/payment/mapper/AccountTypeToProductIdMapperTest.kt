package mega.privacy.android.feature.payment.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.feature.payment.model.mapper.AccountTypeToProductIdMapper
import org.junit.Test

class AccountTypeToProductIdMapperTest {
    private val underTest = AccountTypeToProductIdMapper()

    @Test
    fun `test that PRO_I monthly returns correct product id`() {
        val result = underTest(AccountType.PRO_I, isMonthly = true)
        assertThat(result).isEqualTo(Skus.SKU_PRO_I_MONTH)
    }

    @Test
    fun `test that PRO_I yearly returns correct product id`() {
        val result = underTest(AccountType.PRO_I, isMonthly = false)
        assertThat(result).isEqualTo(Skus.SKU_PRO_I_YEAR)
    }

    @Test
    fun `test that PRO_II monthly returns correct product id`() {
        val result = underTest(AccountType.PRO_II, isMonthly = true)
        assertThat(result).isEqualTo(Skus.SKU_PRO_II_MONTH)
    }

    @Test
    fun `test that PRO_II yearly returns correct product id`() {
        val result = underTest(AccountType.PRO_II, isMonthly = false)
        assertThat(result).isEqualTo(Skus.SKU_PRO_II_YEAR)
    }

    @Test
    fun `test that PRO_III monthly returns correct product id`() {
        val result = underTest(AccountType.PRO_III, isMonthly = true)
        assertThat(result).isEqualTo(Skus.SKU_PRO_III_MONTH)
    }

    @Test
    fun `test that PRO_III yearly returns correct product id`() {
        val result = underTest(AccountType.PRO_III, isMonthly = false)
        assertThat(result).isEqualTo(Skus.SKU_PRO_III_YEAR)
    }

    @Test
    fun `test that PRO_LITE monthly returns correct product id`() {
        val result = underTest(AccountType.PRO_LITE, isMonthly = true)
        assertThat(result).isEqualTo(Skus.SKU_PRO_LITE_MONTH)
    }

    @Test
    fun `test that PRO_LITE yearly returns correct product id`() {
        val result = underTest(AccountType.PRO_LITE, isMonthly = false)
        assertThat(result).isEqualTo(Skus.SKU_PRO_LITE_YEAR)
    }

    @Test
    fun `test that FREE account type returns empty string for monthly`() {
        val result = underTest(AccountType.FREE, isMonthly = true)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that FREE account type returns empty string for yearly`() {
        val result = underTest(AccountType.FREE, isMonthly = false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that BUSINESS account type returns empty string for monthly`() {
        val result = underTest(AccountType.BUSINESS, isMonthly = true)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that BUSINESS account type returns empty string for yearly`() {
        val result = underTest(AccountType.BUSINESS, isMonthly = false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that PRO_FLEXI account type returns empty string for monthly`() {
        val result = underTest(AccountType.PRO_FLEXI, isMonthly = true)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that PRO_FLEXI account type returns empty string for yearly`() {
        val result = underTest(AccountType.PRO_FLEXI, isMonthly = false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that STARTER account type returns empty string for monthly`() {
        val result = underTest(AccountType.STARTER, isMonthly = true)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that STARTER account type returns empty string for yearly`() {
        val result = underTest(AccountType.STARTER, isMonthly = false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that BASIC account type returns empty string for monthly`() {
        val result = underTest(AccountType.BASIC, isMonthly = true)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that BASIC account type returns empty string for yearly`() {
        val result = underTest(AccountType.BASIC, isMonthly = false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that ESSENTIAL account type returns empty string for monthly`() {
        val result = underTest(AccountType.ESSENTIAL, isMonthly = true)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that ESSENTIAL account type returns empty string for yearly`() {
        val result = underTest(AccountType.ESSENTIAL, isMonthly = false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that UNKNOWN account type returns empty string for monthly`() {
        val result = underTest(AccountType.UNKNOWN, isMonthly = true)
        assertThat(result).isEmpty()
    }

    @Test
    fun `test that UNKNOWN account type returns empty string for yearly`() {
        val result = underTest(AccountType.UNKNOWN, isMonthly = false)
        assertThat(result).isEmpty()
    }
}

