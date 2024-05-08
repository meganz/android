package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SubscriptionOption
import mega.privacy.android.domain.entity.account.CurrencyPoint
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Subscription Option List Mapper
 */
internal class SubscriptionOptionListMapper @Inject constructor(
    private val currencyMapper: CurrencyMapper,
    private val accountTypeMapper: AccountTypeMapper,
) {
    /**
     * Invoke
     * @param request [MegaRequest]
     * @return [List<SubscriptionOption>]
     */
    operator fun invoke(
        request: MegaRequest
    ) = (0 until request.pricing.numProducts).map {
        SubscriptionOption(
            accountTypeMapper(request.pricing.getProLevel(it)),
            request.pricing.getMonths(it),
            request.pricing.getHandle(it),
            request.pricing.getGBStorage(it),
            request.pricing.getGBTransfer(it),
            CurrencyPoint.SystemCurrencyPoint(request.pricing.getAmount(it).toLong()),
            currencyMapper(request.currency.currencyName),
        )
    }
}