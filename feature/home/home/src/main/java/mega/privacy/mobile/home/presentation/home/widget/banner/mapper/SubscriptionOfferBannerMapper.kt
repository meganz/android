package mega.privacy.mobile.home.presentation.home.widget.banner.mapper

import androidx.annotation.StringRes
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.mobile.home.presentation.home.widget.banner.model.SubscriptionOfferBannerUiModel
import mega.privacy.android.shared.resources.R as sharedR
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Maps a discounted [Subscription] to the [SubscriptionOfferBannerUiModel] advertising the offer on
 * the Home banner carousel. The offer banner is not delivered by the banners API, so it is built
 * locally. The model carries unresolved [LocalizedText]/[StringRes] values so the banner strings are
 * localised in the UI layer, keeping this mapper a pure Kotlin function.
 */
class SubscriptionOfferBannerMapper @Inject constructor() {

    /**
     * Invoke
     *
     * @param subscription the discounted subscription backing the offer
     * @param locale the locale used to format the discounted monthly price
     * @return the offer banner model, or null when the subscription carries no renderable offer
     */
    operator fun invoke(
        subscription: Subscription,
        locale: Locale,
    ): SubscriptionOfferBannerUiModel? {
        val planNameRes = subscription.accountType.toPlanNameRes() ?: return null
        val discountedAmountMonthly = subscription.discountedAmountMonthly ?: return null
        val discountPercentage = subscription.discountedPercentage?.takeIf { it > 0 }
            ?: return null
        val campaignName = subscription.discountName?.takeUnless { it.isBlank() }
            ?.let { LocalizedText.Literal(it) }
            ?: LocalizedText.StringRes(sharedR.string.subscription_offer_special_offer_badge)
        val currencyFormatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(discountedAmountMonthly.currency.code)
        }
        return SubscriptionOfferBannerUiModel(
            campaignName = campaignName,
            discountPercentage = discountPercentage,
            formattedPrice = currencyFormatter.format(discountedAmountMonthly.value),
            planNameRes = planNameRes,
            validUntil = subscription.offerValidUntil ?: 0L,
            campaignId = subscription.offerCampaignId ?: Subscription.NO_OFFER_CAMPAIGN_ID,
        )
    }

    @StringRes
    private fun AccountType.toPlanNameRes(): Int? = when (this) {
        AccountType.PRO_LITE -> sharedR.string.prolite_account
        AccountType.PRO_I -> sharedR.string.pro1_account
        AccountType.PRO_II -> sharedR.string.pro2_account
        AccountType.PRO_III -> sharedR.string.pro3_account
        AccountType.STARTER -> sharedR.string.starter_account
        AccountType.BASIC -> sharedR.string.basic_account
        AccountType.ESSENTIAL -> sharedR.string.essential_account
        else -> null
    }

    companion object {
        /**
         * Reserved id for the subscription offer banner; server banner ids are positive
         */
        const val SUBSCRIPTION_OFFER_BANNER_ID = -100

        /**
         * Marker url routing the offer banner click to the upgrade screen
         */
        const val SUBSCRIPTION_OFFER_BANNER_URL = "mega://upgradeAccount"
    }
}
