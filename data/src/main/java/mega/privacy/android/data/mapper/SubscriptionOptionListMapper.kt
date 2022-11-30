package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import nz.mega.sdk.MegaRequest

/**
 * Map [MegaRequest], [CurrencyMapper] to [List<SubscriptionOption>]
 */
typealias SubscriptionOptionListMapper = (@JvmSuppressWildcards MegaRequest, @JvmSuppressWildcards CurrencyMapper) -> @JvmSuppressWildcards List<@JvmSuppressWildcards SubscriptionOption>

internal fun toSubscriptionOptionList(
    request: MegaRequest,
    currencyMapper: CurrencyMapper,
): List<SubscriptionOption> {
    val currency = request.currency
    val pricing = request.pricing

    return (0 until request.pricing.numProducts).map {
        SubscriptionOption(
            toAccountType(pricing.getProLevel(it)),
            pricing.getMonths(it),
            pricing.getHandle(it),
            pricing.getGBStorage(it),
            pricing.getGBTransfer(it),
            CurrencyPoint.SystemCurrencyPoint(pricing.getAmount(it).toLong()),
            currencyMapper(currency.currencyName),
        )
    }
}
