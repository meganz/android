package mega.privacy.android.feature.payment.model.mapper

import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.feature.payment.model.LocalisedSubscription
import javax.inject.Inject

/**
 * Mapper for Subscription to convert to LocalisedSubscription
 */
class LocalisedSubscriptionMapper @Inject constructor(
    private val localisedPriceCurrencyCodeStringMapper: LocalisedPriceCurrencyCodeStringMapper,
    private val formattedSizeMapper: FormattedSizeMapper,
) {
    /**
     * Invoke
     * Convert Subscription to LocalisedSubscription
     * @param monthlySubscription [mega.privacy.android.domain.entity.Subscription]
     * @param yearlySubscription [mega.privacy.android.domain.entity.Subscription]
     * @return [mega.privacy.android.feature.payment.model.LocalisedSubscription]
     */
    operator fun invoke(
        monthlySubscription: Subscription,
        yearlySubscription: Subscription,
    ) = LocalisedSubscription(
        monthlySubscription = monthlySubscription,
        yearlySubscription = yearlySubscription,
        localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
        formattedSize = formattedSizeMapper,
    )
}