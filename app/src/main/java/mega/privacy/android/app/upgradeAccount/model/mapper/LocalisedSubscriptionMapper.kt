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
    private val formattedSizeMapper: FormattedSizeMapper
) {
    /**
     * Invoke
     * Convert Subscription to LocalisedSubscription
     * @param monthlySubscription [Subscription]
     * @param yearlySubscription [Subscription]
     * @return [LocalisedSubscription]
     */
    internal operator fun invoke(
        monthlySubscription: Subscription,
        yearlySubscription: Subscription,
    ) = LocalisedSubscription(
        accountType = monthlySubscription.accountType,
        storage = monthlySubscription.storage,
        monthlyTransfer = monthlySubscription.transfer,
        yearlyTransfer = yearlySubscription.transfer,
        monthlyAmount = monthlySubscription.amount,
        yearlyAmount = yearlySubscription.amount,
        localisedPrice = localisedPriceStringMapper,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
}