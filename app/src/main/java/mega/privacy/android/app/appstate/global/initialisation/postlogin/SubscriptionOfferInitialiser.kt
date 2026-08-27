package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.domain.usecase.billing.SetSubscriptionOfferLastShownTimeUseCase
import mega.privacy.android.domain.usecase.billing.ShouldShowSubscriptionOfferUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.SubscriptionOfferNavKey
import mega.privacy.android.navigation.payment.SubscriptionOfferSource
import timber.log.Timber
import javax.inject.Inject

/**
 * Opens the subscription offer landing screen after login when a plan carries a mobile offer, then
 * records the launch so the screen is not shown again until the offer's reshow interval
 * has elapsed.
 */
class SubscriptionOfferInitialiser @Inject constructor(
    shouldShowSubscriptionOfferUseCase: ShouldShowSubscriptionOfferUseCase,
    setSubscriptionOfferLastShownTimeUseCase: SetSubscriptionOfferLastShownTimeUseCase,
    navigationEventQueue: NavigationEventQueue,
) : PostLoginInitialiserAction(
    action = { _, isFastLogin ->
        if (isFastLogin) {
            runCatching {
                if (shouldShowSubscriptionOfferUseCase()) {
                    setSubscriptionOfferLastShownTimeUseCase()
                    navigationEventQueue.emit(
                        SubscriptionOfferNavKey(SubscriptionOfferSource.AutoOpen)
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Error checking the subscription offer")
            }
        }
    }
)
