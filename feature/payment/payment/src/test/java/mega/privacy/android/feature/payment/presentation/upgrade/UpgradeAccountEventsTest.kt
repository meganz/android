package mega.privacy.android.feature.payment.presentation.upgrade

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.mobile.analytics.event.FreeUserBuyProIEvent
import mega.privacy.mobile.analytics.event.FreeUserBuyProIIEvent
import mega.privacy.mobile.analytics.event.FreeUserBuyProIIIEvent
import mega.privacy.mobile.analytics.event.FreeUserBuyProLiteEvent
import mega.privacy.mobile.analytics.event.FreeUserUpgradeAccountPlanMonthlyPeriodTogglePressedEvent
import mega.privacy.mobile.analytics.event.FreeUserUpgradeAccountPlanScreenEvent
import mega.privacy.mobile.analytics.event.FreeUserUpgradeAccountPlanYearlyPeriodTogglePressedEvent
import mega.privacy.mobile.analytics.event.PaidUserBuyProIEvent
import mega.privacy.mobile.analytics.event.PaidUserBuyProIIEvent
import mega.privacy.mobile.analytics.event.PaidUserBuyProIIIEvent
import mega.privacy.mobile.analytics.event.PaidUserBuyProLiteEvent
import mega.privacy.mobile.analytics.event.PaidUserUpgradeAccountPlanMonthlyPeriodTogglePressedEvent
import mega.privacy.mobile.analytics.event.PaidUserUpgradeAccountPlanScreenEvent
import mega.privacy.mobile.analytics.event.PaidUserUpgradeAccountPlanYearlyPeriodTogglePressedEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class UpgradeAccountEventsTest {

    @Test
    fun `test that a free plan reports the free user events`() {
        val events = upgradeAccountEvents(AccountType.FREE)

        assertThat(events.screenView).isEqualTo(FreeUserUpgradeAccountPlanScreenEvent)
        assertThat(events.monthlyTogglePressed)
            .isEqualTo(FreeUserUpgradeAccountPlanMonthlyPeriodTogglePressedEvent)
        assertThat(events.yearlyTogglePressed)
            .isEqualTo(FreeUserUpgradeAccountPlanYearlyPeriodTogglePressedEvent)
        assertThat(events.buyPlanPressed).containsExactly(
            AccountType.PRO_LITE, FreeUserBuyProLiteEvent,
            AccountType.PRO_I, FreeUserBuyProIEvent,
            AccountType.PRO_II, FreeUserBuyProIIEvent,
            AccountType.PRO_III, FreeUserBuyProIIIEvent,
        )
    }

    @Test
    fun `test that an unresolved plan reports the free user events`() {
        assertThat(upgradeAccountEvents(null))
            .isEqualTo(upgradeAccountEvents(AccountType.FREE))
    }

    @Test
    fun `test that an unknown plan reports the free user events`() {
        assertThat(upgradeAccountEvents(AccountType.UNKNOWN))
            .isEqualTo(upgradeAccountEvents(AccountType.FREE))
    }

    @Test
    fun `test that a pro plan reports the paid user events`() {
        val events = upgradeAccountEvents(AccountType.PRO_I)

        assertThat(events.screenView).isEqualTo(PaidUserUpgradeAccountPlanScreenEvent)
        assertThat(events.monthlyTogglePressed)
            .isEqualTo(PaidUserUpgradeAccountPlanMonthlyPeriodTogglePressedEvent)
        assertThat(events.yearlyTogglePressed)
            .isEqualTo(PaidUserUpgradeAccountPlanYearlyPeriodTogglePressedEvent)
        assertThat(events.buyPlanPressed).containsExactly(
            AccountType.PRO_LITE, PaidUserBuyProLiteEvent,
            AccountType.PRO_I, PaidUserBuyProIEvent,
            AccountType.PRO_II, PaidUserBuyProIIEvent,
            AccountType.PRO_III, PaidUserBuyProIIIEvent,
        )
    }

    @ParameterizedTest
    @EnumSource(AccountType::class)
    fun `test that every paid plan reports the paid user events`(accountType: AccountType) {
        val events = upgradeAccountEvents(accountType)

        val expected = if (accountType.isPaid) {
            upgradeAccountEvents(AccountType.PRO_I)
        } else {
            upgradeAccountEvents(AccountType.FREE)
        }
        assertThat(events).isEqualTo(expected)
    }
}
