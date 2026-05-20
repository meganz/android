package mega.privacy.android.app.myAccount

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyAccountUsageUiStateTest {

    @Test
    fun `test that isFreeAccount is true when accountType is FREE`() {
        val state = MyAccountUsageUiState(accountType = AccountType.FREE)
        assertThat(state.isFreeAccount).isTrue()
    }

    @Test
    fun `test that isFreeAccount is true when accountType is UNKNOWN`() {
        val state = MyAccountUsageUiState(accountType = AccountType.UNKNOWN)
        assertThat(state.isFreeAccount).isTrue()
    }

    @Test
    fun `test that isFreeAccount is false when accountType is PRO_I`() {
        val state = MyAccountUsageUiState(accountType = AccountType.PRO_I)
        assertThat(state.isFreeAccount).isFalse()
    }

    @Test
    fun `test that isFreeAccount is false when accountType is PRO_LITE`() {
        val state = MyAccountUsageUiState(accountType = AccountType.PRO_LITE)
        assertThat(state.isFreeAccount).isFalse()
    }

    @Test
    fun `test that isFreeAccount is false when accountType is BUSINESS`() {
        val state = MyAccountUsageUiState(accountType = AccountType.BUSINESS)
        assertThat(state.isFreeAccount).isFalse()
    }

    @Test
    fun `test that showUpgradeButton is true when account is not business and not pro flexi`() {
        val state = MyAccountUsageUiState(isBusinessAccount = false, isProFlexiAccount = false)
        assertThat(state.showUpgradeButton).isTrue()
    }

    @Test
    fun `test that showUpgradeButton is false when account is business`() {
        val state = MyAccountUsageUiState(isBusinessAccount = true, isProFlexiAccount = false)
        assertThat(state.showUpgradeButton).isFalse()
    }

    @Test
    fun `test that showUpgradeButton is false when account is pro flexi`() {
        val state = MyAccountUsageUiState(isBusinessAccount = false, isProFlexiAccount = true)
        assertThat(state.showUpgradeButton).isFalse()
    }

    @Test
    fun `test that showUpgradeButton is false when account is both business and pro flexi`() {
        val state = MyAccountUsageUiState(isBusinessAccount = true, isProFlexiAccount = true)
        assertThat(state.showUpgradeButton).isFalse()
    }

    @Test
    fun `test that showPaymentAlert is true when account is pro flexi`() {
        val state = MyAccountUsageUiState(isProFlexiAccount = true)
        assertThat(state.showPaymentAlert).isTrue()
    }

    @Test
    fun `test that showPaymentAlert is true when account is master business`() {
        val state = MyAccountUsageUiState(isBusinessAccount = true, isMasterBusinessAccount = true)
        assertThat(state.showPaymentAlert).isTrue()
    }

    @Test
    fun `test that showPaymentAlert is false when account is business sub-account`() {
        val state = MyAccountUsageUiState(isBusinessAccount = true, isMasterBusinessAccount = false)
        assertThat(state.showPaymentAlert).isFalse()
    }

    @Test
    fun `test that showPaymentAlert is false when account is free`() {
        val state = MyAccountUsageUiState(accountType = AccountType.FREE)
        assertThat(state.showPaymentAlert).isFalse()
    }

    @Test
    fun `test that showPaymentAlert is true when account is paid with renewable subscription`() {
        val state = MyAccountUsageUiState(
            accountType = AccountType.PRO_I,
            hasRenewableSubscription = true,
        )
        assertThat(state.showPaymentAlert).isTrue()
    }

    @Test
    fun `test that showPaymentAlert is true when account is paid with expirable subscription`() {
        val state = MyAccountUsageUiState(
            accountType = AccountType.PRO_I,
            hasExpirableSubscription = true,
        )
        assertThat(state.showPaymentAlert).isTrue()
    }
}
