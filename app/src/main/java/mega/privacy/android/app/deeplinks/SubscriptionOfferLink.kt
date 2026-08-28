package mega.privacy.android.app.deeplinks

import android.net.Uri

private const val MEGA_SCHEME = "mega"
private const val UPGRADE_HOST = "upgrade"
private const val OFFER_QUERY_PARAMETER = "offer"
private const val OFFER_QUERY_VALUE = "1"

/**
 * True for `mega://upgrade`, the link the upgrade campaigns are sent with.
 */
internal val Uri.isMegaUpgradeLink: Boolean
    get() = scheme == MEGA_SCHEME && host == UPGRADE_HOST

/**
 * True for `mega://upgrade?offer=1`, the link a subscription offer campaign is sent with. It is the
 * only thing that tells the subscription offer notification apart from any other promo push, so
 * both the push worker and the deep link handler match on it to report the notification analytics.
 */
internal val Uri.isSubscriptionOfferLink: Boolean
    get() = isMegaUpgradeLink && getQueryParameter(OFFER_QUERY_PARAMETER) == OFFER_QUERY_VALUE

/**
 * [isSubscriptionOfferLink] for a link that is still unparsed, as it arrives in a push message.
 */
internal fun isSubscriptionOfferLink(link: String): Boolean =
    Uri.parse(link).isSubscriptionOfferLink
