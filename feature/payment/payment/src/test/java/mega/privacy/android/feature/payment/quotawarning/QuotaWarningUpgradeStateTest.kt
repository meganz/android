package mega.privacy.android.feature.payment.quotawarning

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaMetric
import mega.privacy.android.feature.payment.presentation.quotawarning.QuotaWarningUpgradeState
import org.junit.jupiter.api.Test

class QuotaWarningUpgradeStateTest {

    @Test
    fun `test that showQuotaDetails is true for a pro user on both metrics`() {
        val underTest = QuotaWarningUpgradeState(
            currentPlan = AccountType.PRO_I,
            isLoggedIn = true,
        )

        assertThat(underTest.showQuotaDetails(QuotaMetric.Storage)).isTrue()
        assertThat(underTest.showQuotaDetails(QuotaMetric.Transfer)).isTrue()
    }

    @Test
    fun `test that showQuotaDetails is true for a free user on storage`() {
        val underTest = QuotaWarningUpgradeState(
            currentPlan = AccountType.FREE,
            isLoggedIn = true,
        )

        assertThat(underTest.showQuotaDetails(QuotaMetric.Storage)).isTrue()
    }

    @Test
    fun `test that showQuotaDetails is false for a free user on transfer`() {
        val underTest = QuotaWarningUpgradeState(
            currentPlan = AccountType.FREE,
            isLoggedIn = true,
        )

        assertThat(underTest.showQuotaDetails(QuotaMetric.Transfer)).isFalse()
    }

    @Test
    fun `test that showQuotaDetails is false on both metrics when the user is not logged in`() {
        val underTest = QuotaWarningUpgradeState(
            currentPlan = null,
            isLoggedIn = false,
        )

        assertThat(underTest.showQuotaDetails(QuotaMetric.Storage)).isFalse()
        assertThat(underTest.showQuotaDetails(QuotaMetric.Transfer)).isFalse()
    }

    @Test
    fun `test that showQuotaDetails is true for a free user on storage when the plan is unknown`() {
        val underTest = QuotaWarningUpgradeState(
            currentPlan = null,
            isLoggedIn = true,
        )

        assertThat(underTest.showQuotaDetails(QuotaMetric.Storage)).isTrue()
        assertThat(underTest.showQuotaDetails(QuotaMetric.Transfer)).isFalse()
    }
}
