package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.domain.entity.Subscription
import javax.inject.Inject

/**
 * Mapper for Subscription to convert to LocalisedSubscription
 */
class LocalisedSubscriptionMapper @Inject constructor(
    private val localisedPriceStringMapper: LocalisedPriceStringMapper,
    private val localisedPriceCurrencyCodeStringMapper: LocalisedPriceCurrencyCodeStringMapper,
) {
    /**
     * Invoke
     * Convert Subscription to LocalisedSubscription
     * @param subscription [Subscription]
     * @return LocalisedSubscription
     */
    internal operator fun invoke(
        subscription: Subscription,
    ) = LocalisedSubscription(
        accountType = subscription.accountType,
        handle = subscription.handle,
        storage = subscription.storage,
        transfer = subscription.transfer,
        amount = subscription.amount,
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper
    )
}