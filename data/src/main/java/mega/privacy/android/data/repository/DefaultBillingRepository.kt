package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.BillingRepository
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [BillingRepository]
 *
 */

internal class DefaultBillingRepository @Inject constructor(
    private val accountInfoWrapper: AccountInfoWrapper,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val paymentMethodFlagsCache: Cache<PaymentMethodFlags>,
) : BillingRepository {

    override suspend fun getLocalPricing(sku: String): MegaSku? =
        accountInfoWrapper.availableSkus.firstOrNull { megaSku ->
            megaSku.sku == sku
        }

    override suspend fun getPaymentMethod(clearCache: Boolean): PaymentMethodFlags =
        paymentMethodFlagsCache.get()?.takeUnless { clearCache }
            ?: fetchPaymentMethodFlags().also { paymentMethodFlagsCache.set(it) }

    private suspend fun fetchPaymentMethodFlags(): PaymentMethodFlags = withContext(ioDispatcher) {
        Timber.d("getPaymentMethod")
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(PaymentMethodFlags(request.number)))
                    } else {
                        continuation.failWithError(error)
                    }
                },
            )
            megaApiGateway.getPaymentMethods(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }
}
