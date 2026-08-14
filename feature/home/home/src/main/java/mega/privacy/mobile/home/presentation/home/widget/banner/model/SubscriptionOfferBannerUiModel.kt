package mega.privacy.mobile.home.presentation.home.widget.banner.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

/**
 * Display data for the locally-built subscription offer banner shown on the Home carousel.
 *
 * Holds unresolved values ([LocalizedText] and a [StringRes]) rather than resolved strings, so the
 * banner text is localised in the UI layer instead of at mapping time. This keeps the mapper a pure
 * Kotlin function and lets the copy follow a locale change.
 *
 * @property campaignName Campaign label — the discount name, or a "Special offer" fallback.
 * @property discountPercentage Discount percentage shown in the headline (e.g. 50 for "50% off").
 * @property formattedPrice Locale-formatted discounted monthly price (e.g. "€4.99").
 * @property planNameRes Plan name string resource (e.g. "Pro I").
 * @property validUntil Offer expiry as epoch seconds, driving the countdown on banners that show
 * one; 0 when the offer carries no expiry, which hides the countdown.
 * @property campaignId Campaign the offer belongs to, used as the dismissal key so hiding the banner
 * hides every offer of the campaign and no offer of the next one. Offers belonging to no campaign
 * group carry the no-campaign id, so their dismissal is still persisted.
 */
@Immutable
data class SubscriptionOfferBannerUiModel(
    val campaignName: LocalizedText,
    val discountPercentage: Int,
    val formattedPrice: String,
    @StringRes val planNameRes: Int,
    val validUntil: Long,
    val campaignId: Long,
)
