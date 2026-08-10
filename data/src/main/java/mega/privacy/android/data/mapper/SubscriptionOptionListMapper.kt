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
            sku = request.pricing.getAndroidID(it),
            accountType = accountTypeMapper(request.pricing.getProLevel(it)),
            months = request.pricing.getMonths(it),
            handle = request.pricing.getHandle(it),
            storage = request.pricing.getGBStorage(it),
            transfer = request.pricing.getGBTransfer(it),
            amount = CurrencyPoint.SystemCurrencyPoint(request.pricing.getAmount(it).toLong()),
            currency = currencyMapper(request.currency.currencyName.orEmpty()),
            hasOffer = request.pricing.hasMobileOffers(it),
            discountName = request.pricing.getMobileOfferLabel(it),
            offerValidUntil = request.pricing.getMobileOfferExpiryTimestamp(it)
                .takeIf { expiry -> expiry > 0 },
            offerFlags = request.pricing.getMobileOfferFlags(it)
                .takeIf { flags -> flags > 0 },
        )
    }
}