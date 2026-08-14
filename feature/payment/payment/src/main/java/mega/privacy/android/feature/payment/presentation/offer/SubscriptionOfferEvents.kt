package mega.privacy.android.feature.payment.presentation.offer

import mega.privacy.android.navigation.payment.SubscriptionOfferSource
import mega.privacy.mobile.analytics.core.event.identifier.ButtonPressedEventIdentifier
import mega.privacy.mobile.analytics.core.event.identifier.ScreenViewEventIdentifier
import mega.privacy.mobile.analytics.event.SubscriptionOfferAutoOpenCtaButtonPressedEvent
import mega.privacy.mobile.analytics.event.SubscriptionOfferAutoOpenDismissButtonPressedEvent
import mega.privacy.mobile.analytics.event.SubscriptionOfferAutoOpenScreenEvent
import mega.privacy.mobile.analytics.event.SubscriptionOfferAutoOpenViewAllPlansButtonPressedEvent
import mega.privacy.mobile.analytics.event.SubscriptionOfferTriggeredCtaButtonPressedEvent
import mega.privacy.mobile.analytics.event.SubscriptionOfferTriggeredDismissButtonPressedEvent
import mega.privacy.mobile.analytics.event.SubscriptionOfferTriggeredScreenEvent
import mega.privacy.mobile.analytics.event.SubscriptionOfferTriggeredViewAllPlansButtonPressedEvent

/**
 * Analytics events for the subscription offer landing screen, for one way of opening it.
 *
 * @property screenView screen-view event
 * @property ctaPressed event for the buy CTA
 * @property dismissPressed event for the dismiss (X) icon
 * @property viewAllPlansPressed event for the "View all plans" button
 */
internal data class SubscriptionOfferEvents(
    val screenView: ScreenViewEventIdentifier,
    val ctaPressed: ButtonPressedEventIdentifier,
    val dismissPressed: ButtonPressedEventIdentifier,
    val viewAllPlansPressed: ButtonPressedEventIdentifier,
)

/**
 * The events to report for a screen opened via [source]. The tracking spec separates the automatic
 * post-login launch from any launch the user triggered, so every banner and notification entry
 * point shares the "triggered" set.
 */
internal fun subscriptionOfferEvents(source: SubscriptionOfferSource): SubscriptionOfferEvents =
    when (source) {
        SubscriptionOfferSource.AutoOpen -> SubscriptionOfferEvents(
            screenView = SubscriptionOfferAutoOpenScreenEvent,
            ctaPressed = SubscriptionOfferAutoOpenCtaButtonPressedEvent,
            dismissPressed = SubscriptionOfferAutoOpenDismissButtonPressedEvent,
            viewAllPlansPressed = SubscriptionOfferAutoOpenViewAllPlansButtonPressedEvent,
        )

        SubscriptionOfferSource.HomeBanner,
        SubscriptionOfferSource.MenuBanner,
        SubscriptionOfferSource.Notification,
            -> SubscriptionOfferEvents(
            screenView = SubscriptionOfferTriggeredScreenEvent,
            ctaPressed = SubscriptionOfferTriggeredCtaButtonPressedEvent,
            dismissPressed = SubscriptionOfferTriggeredDismissButtonPressedEvent,
            viewAllPlansPressed = SubscriptionOfferTriggeredViewAllPlansButtonPressedEvent,
        )
    }
