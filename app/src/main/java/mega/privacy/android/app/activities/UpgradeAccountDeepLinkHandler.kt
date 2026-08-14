package mega.privacy.android.app.activities

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionWithOfferUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.SubscriptionOfferNavKey
import mega.privacy.android.navigation.destination.UpgradeAccountNavKey
import mega.privacy.android.navigation.payment.SubscriptionOfferSource
import javax.inject.Inject

/**
 * Deep link handler for upgrade account link
 */
class UpgradeAccountDeepLinkHandler @Inject constructor(
    private val getRecommendedSubscriptionWithOfferUseCase: GetRecommendedSubscriptionWithOfferUseCase,
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? =
        when {
            uri.isMegaUpgradeLink -> listOf(megaUpgradeLinkNavKey(uri, isLoggedIn))

            regexPatternType == RegexPatternType.UPGRADE_PAGE_LINK
                    || regexPatternType == RegexPatternType.UPGRADE_LINK -> {
                listOf(UpgradeAccountNavKey())
            }

            else -> {
                null
            }
        }

    /**
     * Campaigns with their own landing screen are sent as `mega://upgrade?offer=1`; the offer is
     * only worth showing while it is still running, so an offer that is no longer available lands on
     * the upgrade screen instead, as does plain `mega://upgrade`.
     *
     * A logged out user is told to log in rather than taken to either screen, so the offer is not
     * looked up at all in that case.
     */
    private suspend fun megaUpgradeLinkNavKey(uri: Uri, isLoggedIn: Boolean): NavKey {
        val hasOffer = isLoggedIn
                && uri.getQueryParameter(OFFER_QUERY_PARAMETER) == OFFER_QUERY_VALUE
        return if (hasOffer && getRecommendedSubscriptionOffer() != null) {
            SubscriptionOfferNavKey(SubscriptionOfferSource.Notification)
        } else {
            UpgradeAccountNavKey()
        }
    }

    private suspend fun getRecommendedSubscriptionOffer() =
        runCatching { getRecommendedSubscriptionWithOfferUseCase() }
            .logAndSwallowExceptions()
            .getOrNull()

    private val Uri.isMegaUpgradeLink: Boolean
        get() = scheme == MEGA_SCHEME && host == UPGRADE_HOST

    private companion object {
        const val MEGA_SCHEME = "mega"
        const val UPGRADE_HOST = "upgrade"
        const val OFFER_QUERY_PARAMETER = "offer"
        const val OFFER_QUERY_VALUE = "1"
    }
}
