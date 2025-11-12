package mega.privacy.android.feature.payment.model.extensions

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import org.junit.jupiter.api.Test

internal class UIAccountTypeExtTest {

    @Test
    fun `test that PRO_LITE maps to propay_101`() {
        assertThat(AccountType.PRO_LITE.toWebClientProductId()).isEqualTo("propay_101")
    }

    @Test
    fun `test that PRO_I maps to propay_1`() {
        assertThat(AccountType.PRO_I.toWebClientProductId()).isEqualTo("propay_1")
    }

    @Test
    fun `test that PRO_II maps to propay_2`() {
        assertThat(AccountType.PRO_II.toWebClientProductId()).isEqualTo("propay_2")
    }

    @Test
    fun `test that PRO_III maps to propay_3`() {
        assertThat(AccountType.PRO_III.toWebClientProductId()).isEqualTo("propay_3")
    }

    @Test
    fun `test that BUSINESS maps to registerb`() {
        assertThat(AccountType.BUSINESS.toWebClientProductId()).isEqualTo("registerb")
    }

    @Test
    fun `test that PRO_FLEXI maps to propay_4`() {
        assertThat(AccountType.PRO_FLEXI.toWebClientProductId()).isEqualTo("propay_4")
    }

    @Test
    fun `test that FREE maps to empty string`() {
        assertThat(AccountType.FREE.toWebClientProductId()).isEmpty()
    }

    @Test
    fun `test that other AccountTypes map to empty string`() {
        // Test all other AccountType enum values
        val otherTypes = listOf(
            AccountType.STARTER,
            AccountType.BASIC,
            AccountType.ESSENTIAL
        )

        otherTypes.forEach { accountType ->
            assertThat(accountType.toWebClientProductId()).isEmpty()
        }
    }
}

