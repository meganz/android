package mega.privacy.android.data.repository

import android.app.Activity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.gateway.BillingGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.LocalPricingMapper
import mega.privacy.android.data.mapper.PaymentMethodTypeMapper
import mega.privacy.android.data.mapper.PricingMapper
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.BillingRepository
import nz.mega.sdk.MegaError
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [BillingRepository]
 *
 * @property accountInfoWrapper
 * @property megaApiGateway
 * @property ioDispatcher
 * @property paymentMethodFlagsCache
 * @property localPricingMapper
 */

internal class DefaultBillingRepository @Inject constructor(
    private val accountInfoWrapper: AccountInfoWrapper,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val paymentMethodFlagsCache: Cache<PaymentMethodFlags>,
    private val pricingCache: Cache<Pricing>,
    private val numberOfSubscriptionCache: Cache<Long>,
    private val pricingMapper: PricingMapper,
    private val localPricingMapper: LocalPricingMapper,
    private val billingGateway: BillingGateway,
    private val paymentMethodTypeMapper: PaymentMethodTypeMapper,
) : BillingRepository, AndroidBillingRepository {

    override suspend fun getLocalPricing(sku: String) =
        accountInfoWrapper.availableSkus.firstOrNull { megaSku ->
            megaSku.sku == sku
        }?.let { localPricingMapper(it) }

    override suspend fun getPricing(clearCache: Boolean): Pricing =
        pricingCache.get()?.takeUnless { clearCache }
            ?: fetchPricing().also { pricingCache.set(it) }

    override suspend fun getPaymentMethod(clearCache: Boolean): PaymentMethodFlags =
        paymentMethodFlagsCache.get()?.takeUnless { clearCache }
            ?: fetchPaymentMethodFlags().also { paymentMethodFlagsCache.set(it) }

    override suspend fun getNumberOfSubscription(clearCache: Boolean): Long =
        numberOfSubscriptionCache.get()?.takeUnless { clearCache }
            ?: fetchNumberOfSubscription().also {
                numberOfSubscriptionCache.set(it)
            }

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

    private suspend fun fetchPricing(): Pricing = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener {
                pricingMapper(it.pricing, it.currency)
            }
            megaApiGateway.getPricing(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    private suspend fun fetchNumberOfSubscription(): Long = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener {
                it.number
            }
            megaApiGateway.creditCardQuerySubscriptions(listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun queryPurchase(): List<MegaPurchase> = billingGateway.queryPurchase()

    override suspend fun querySkus(): List<MegaSku> = billingGateway.querySkus()

    override fun monitorBillingEvent(): Flow<BillingEvent> = billingGateway.monitorBillingEvent()

    override suspend fun launchPurchaseFlow(activity: Activity, productId: String) =
        billingGateway.launchPurchaseFlow(activity, productId)

    override suspend fun getCurrentPaymentMethod(): PaymentMethod? =
        PaymentMethod.values().firstOrNull {
            it.methodId == paymentMethodTypeMapper(accountInfoWrapper.subscriptionMethodId)
        }

    override suspend fun isBillingAvailable(): Boolean =
        accountInfoWrapper.availableSkus.isNotEmpty()
}
