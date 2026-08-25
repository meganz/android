package mega.privacy.android.feature.payment.presentation.upgrade

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.mobile.analytics.core.event.identifier.ButtonPressedEventIdentifier
import mega.privacy.mobile.analytics.core.event.identifier.ScreenViewEventIdentifier
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

/**
 * Analytics events for the subscription page, for one kind of viewer.
 *
 * @property screenView screen-view event
 * @property monthlyTogglePressed event for the Monthly segment of the billing period selector
 * @property yearlyTogglePressed event for the Yearly segment of the billing period selector
 * @property buyPlanPressed buy CTA event of each Pro plan, keyed by plan
 */
internal data class UpgradeAccountEvents(
    val screenView: ScreenViewEventIdentifier,
    val monthlyTogglePressed: ButtonPressedEventIdentifier,
    val yearlyTogglePressed: ButtonPressedEventIdentifier,
    val buyPlanPressed: Map<AccountType, ButtonPressedEventIdentifier>,
)

/**
 * The events to report for the subscription page, chosen by the viewer's [currentSubscriptionPlan].
 * The tracking spec counts free and paid users separately, using the same event set as iOS.
 *
 * A plan that is not resolved yet (null, only monitored for the upgrade flow) reports as a free
 * user, which is what the onboarding flow always shows.
 */
internal fun upgradeAccountEvents(currentSubscriptionPlan: AccountType?): UpgradeAccountEvents =
    if (currentSubscriptionPlan?.isPaid == true) paidUserEvents else freeUserEvents

private val freeUserEvents = UpgradeAccountEvents(
    screenView = FreeUserUpgradeAccountPlanScreenEvent,
    monthlyTogglePressed = FreeUserUpgradeAccountPlanMonthlyPeriodTogglePressedEvent,
    yearlyTogglePressed = FreeUserUpgradeAccountPlanYearlyPeriodTogglePressedEvent,
    buyPlanPressed = mapOf(
        AccountType.PRO_LITE to FreeUserBuyProLiteEvent,
        AccountType.PRO_I to FreeUserBuyProIEvent,
        AccountType.PRO_II to FreeUserBuyProIIEvent,
        AccountType.PRO_III to FreeUserBuyProIIIEvent,
    ),
)

private val paidUserEvents = UpgradeAccountEvents(
    screenView = PaidUserUpgradeAccountPlanScreenEvent,
    monthlyTogglePressed = PaidUserUpgradeAccountPlanMonthlyPeriodTogglePressedEvent,
    yearlyTogglePressed = PaidUserUpgradeAccountPlanYearlyPeriodTogglePressedEvent,
    buyPlanPressed = mapOf(
        AccountType.PRO_LITE to PaidUserBuyProLiteEvent,
        AccountType.PRO_I to PaidUserBuyProIEvent,
        AccountType.PRO_II to PaidUserBuyProIIEvent,
        AccountType.PRO_III to PaidUserBuyProIIIEvent,
    ),
)
