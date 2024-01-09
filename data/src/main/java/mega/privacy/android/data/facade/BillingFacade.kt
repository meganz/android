package mega.privacy.android.data.facade

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.gateway.BillingGateway
import mega.privacy.android.data.gateway.VerifyPurchaseGateway
import mega.privacy.android.data.mapper.MegaPurchaseMapper
import mega.privacy.android.data.mapper.MegaSkuMapper
import mega.privacy.android.domain.entity.account.MegaSku
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_III_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_III_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_II_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_II_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_I_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_I_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_YEAR
import mega.privacy.android.domain.entity.billing.BillingEvent
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.exception.ConnectBillingServiceException
import mega.privacy.android.domain.exception.ProductNotFoundException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Billing facade
 * Sharing same billingClient instance across the application, we don't need to create every activity launch
 * It manage connect and disconnect by process lifecycle
 *
 * https://developer.android.com/reference/com/android/billingclient/api/BillingClient
 * It provides convenience methods for in-app billing. You can create one instance of this class for your application and use it to process in-app billing operations.
 * It provides synchronous (blocking) and asynchronous (non-blocking) methods for many common in-app billing operations.
 *
 * It's strongly recommended that you instantiate only one BillingClient instance at one time to avoid multiple PurchasesUpdatedListener.onPurchasesUpdated(BillingResult, List) callbacks for a single event
 */
internal class BillingFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaSkuMapper: MegaSkuMapper,
    private val megaPurchaseMapper: MegaPurchaseMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val verifyPurchaseGateway: VerifyPurchaseGateway,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val skusCache: Cache<List<MegaSku>>,
    private val accountInfoWrapper: AccountInfoWrapper,
    private val productDetailsListCache: Cache<List<ProductDetails>>,
    private val activeSubscription: Cache<MegaPurchase>,
) : BillingGateway, PurchasesUpdatedListener, DefaultLifecycleObserver {
    private val mutex = Mutex()
    private val billingEvent = MutableSharedFlow<BillingEvent>()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
    }

    init {
        applicationScope.launch(mainDispatcher) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this@BillingFacade)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        applicationScope.launch(exceptionHandler) {
            ensureConnect()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        applicationScope.launch {
            disconnect()
        }
    }

    private val billingClientRef: AtomicReference<BillingClient?> = AtomicReference(null)

    private val obfuscatedAccountId: String?
        get() = verifyPurchaseGateway.generateObfuscatedAccountId()

    private suspend fun disconnect() {
        mutex.withLock {
            val client = billingClientRef.get()
            if (client?.isReady == true) {
                client.endConnection()
            }
            billingClientRef.set(null)
        }
    }

    override fun monitorBillingEvent() = billingEvent.asSharedFlow()

    @Throws(ProductNotFoundException::class)
    override suspend fun launchPurchaseFlow(activity: Activity, productId: String) {
        val oldSubscription = activeSubscription.get()
        val oldSku = oldSubscription?.sku
        val purchaseToken = oldSubscription?.token
        val skuDetails = skusCache.get()?.find { it.sku == productId }
            ?: run {
                Timber.w("Can't find sku with id: %s", productId)
                throw ProductNotFoundException()
            }
        Timber.d("oldSku is:%s, new sku is:%s", oldSku, skuDetails)
        Timber.d("Obfuscated account id is:%s", obfuscatedAccountId)
        //if user is upgrading, it take effect immediately otherwise wait until current plan expired
        val replacementMode =
            if (MegaPurchase(skuDetails.sku).level > MegaPurchase(oldSku).level) SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION else SubscriptionUpdateParams.ReplacementMode.DEFERRED
        val productDetails: ProductDetails =
            productDetailsListCache.get().orEmpty().find { it.productId == productId }
                ?: throw ProductNotFoundException()
        productDetails.subscriptionOfferDetails?.let { offerDetailsList ->
            val offerToken = offerDetailsList.firstOrNull()?.offerToken.orEmpty()
            val productDetailsParams = listOf(
                ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
            val purchaseParamsBuilder = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParams)
                .setObfuscatedAccountId(obfuscatedAccountId.orEmpty())

            // setSubscriptionUpdateParams asks that have to include the old sku information,
            // otherwise throw an exception
            if (oldSku != null && purchaseToken != null) {
                val builder =
                    SubscriptionUpdateParams.newBuilder()
                        .setSubscriptionReplacementMode(replacementMode)
                        .setOldPurchaseToken(purchaseToken)
                purchaseParamsBuilder.setSubscriptionUpdateParams(builder.build())
            }

            /*
              If do a full login, ManagerActivity's mIntent will be set as null.
              Work around, check the intent's nullity first, if null, set an empty Intent, as we don't use "PROXY_PACKAGE",
              otherwise billing library crashes internally.
              @see com.android.billingclient.api.BillingClientImpl -> var1.getIntent().getStringExtra("PROXY_PACKAGE")
            */
            if (activity.intent == null) {
                activity.intent = Intent()
            }
            val client = ensureConnect()
            client.launchBillingFlow(activity, purchaseParamsBuilder.build())
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        applicationScope.launch(ioDispatcher + exceptionHandler) {
            if (result.responseCode == BillingClient.BillingResponseCode.OK
                && purchases.isNullOrEmpty().not()
            ) {
                val client = ensureConnect()
                val validPurchases = processPurchase(client, purchases.orEmpty())
                billingEvent.emit(
                    BillingEvent.OnPurchaseUpdate(
                        validPurchases,
                        activeSubscription.get()
                    )
                )
            } else {
                Timber.w("onPurchasesUpdated failed, with result code: %s", result.responseCode)
            }
        }
    }

    override suspend fun querySkus(): List<MegaSku> = withContext(ioDispatcher) {
        val client = ensureConnect()
        // check the caller still observer, sometime it takes time to connect to BillingClient
        ensureActive()
        val productList = IN_APP_SKUS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w(
                "Failed to get SkuDetails, error code is %s",
                result.billingResult.responseCode
            )
        }
        val productsDetails = result.productDetailsList.orEmpty()
        productDetailsListCache.set(productsDetails)
        val megaSkus = productsDetails
            .mapNotNull {
                megaSkuMapper(it)
            }
        skusCache.set(megaSkus)
        return@withContext megaSkus
    }

    override suspend fun queryPurchase(): List<MegaPurchase> = withContext(ioDispatcher) {
        val client = ensureConnect()
        // check the caller still observer, sometime it takes time to connect to BillingClient
        ensureActive()
        val inAppPurchaseResult = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val purchases = if (areSubscriptionsSupported(client)) {
            val subscriptionPurchaseResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            inAppPurchaseResult.purchasesList + subscriptionPurchaseResult.purchasesList
        } else {
            inAppPurchaseResult.purchasesList
        }
        if (inAppPurchaseResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w("Query of purchases failed, result code is ${inAppPurchaseResult.billingResult.responseCode}")
            return@withContext emptyList()
        } else {
            Timber.w("Query of purchases success, purchase size ${purchases.size}")
        }
        return@withContext processPurchase(client, purchases)
    }

    private suspend fun processPurchase(
        client: BillingClient,
        purchaseList: List<Purchase>,
    ): List<MegaPurchase> {
        // Verify all available purchases
        val validPurchases = purchaseList.filter { purchase ->
            purchase.accountIdentifiers?.obfuscatedAccountId == obfuscatedAccountId
                    && verifyValidSignature(purchase.originalJson, purchase.signature)
        }
        validPurchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Acknowledge the purchase if it hasn't already been acknowledged.
                if (!purchase.isAcknowledged) {
                    Timber.d("new purchase added, %s", purchase.originalJson)
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val result = client.acknowledgePurchase(acknowledgePurchaseParams)
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Timber.i("purchase acknowledged")
                    } else {
                        Timber.w("purchase acknowledge failed, %s", result.debugMessage)
                    }
                }
            }
        }
        val validMegaPurchases = validPurchases.map { megaPurchaseMapper(it) }
        // legacy support, we still need to save into MyAccountInfo, will remove after refactoring
        updateAccountInfo(validMegaPurchases)
        Timber.d("total purchased are: %d", validMegaPurchases.size)
        return validMegaPurchases
    }

    private fun areSubscriptionsSupported(client: BillingClient): Boolean {
        val responseCode: Int =
            client.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode
        Timber.d(
            "areSubscriptionsSupported %s",
            responseCode == BillingClient.BillingResponseCode.OK
        )
        return responseCode == BillingClient.BillingResponseCode.OK
    }

    // to make thread safe we need to wrap into Mutex for synchronized coroutine
    private suspend fun ensureConnect(): BillingClient {
        return mutex.withLock {
            val oldClient = billingClientRef.get()
            if (oldClient?.isReady == true) return@withLock oldClient
            val newClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(this)
                .build()
            return@withLock suspendCancellableCoroutine { continuation ->
                val listener = object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                    }

                    override fun onBillingSetupFinished(result: BillingResult) {
                        Timber.d("Response code is: %s", result.responseCode)
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (continuation.isActive)
                                continuation.resumeWith(Result.success(newClient))
                        } else {
                            if (continuation.isActive)
                                continuation.resumeWith(
                                    Result.failure(
                                        ConnectBillingServiceException(
                                            result.responseCode,
                                            result.debugMessage
                                        )
                                    )
                                )
                        }
                    }
                }
                newClient.startConnection(listener)
            }
        }
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return runCatching {
            verifyPurchaseGateway.verifyPurchase(
                signedData,
                signature
            )
        }.getOrElse { false }
    }

    private fun updateAccountInfo(purchases: List<MegaPurchase>) {
        val max = purchases.maxByOrNull { it.level }
        activeSubscription.set(max)
        accountInfoWrapper.updateActiveSubscription(max)
    }

    companion object {
        private val IN_APP_SKUS = listOf(
            SKU_PRO_I_MONTH,
            SKU_PRO_I_YEAR,
            SKU_PRO_II_MONTH,
            SKU_PRO_II_YEAR,
            SKU_PRO_III_MONTH,
            SKU_PRO_III_YEAR,
            SKU_PRO_LITE_MONTH,
            SKU_PRO_LITE_YEAR,
        )
    }
}