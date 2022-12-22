package mega.privacy.android.data.facade

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingFlowParams.ProrationMode
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

internal class BillingFacade @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaSkuMapper: MegaSkuMapper,
    private val megaPurchaseMapper: MegaPurchaseMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val verifyPurchaseGateway: VerifyPurchaseGateway,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val accountInfoWrapper: AccountInfoWrapper,
    private val productDetailsListCache: Cache<List<ProductDetails>>,
) : BillingGateway, PurchasesUpdatedListener {
    private val mutex = Mutex()
    private val billingEvent = MutableSharedFlow<BillingEvent>()

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .enablePendingPurchases()
            .setListener(this)
            .build()
    }

    private val obfuscatedAccountId by lazy {
        verifyPurchaseGateway.generateObfuscatedAccountId()
    }

    override suspend fun disconnect() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    override fun monitorBillingEvent() = billingEvent.asSharedFlow()

    @Throws(ProductNotFoundException::class)
    override suspend fun launchPurchaseFlow(activity: Activity, productId: String) {
        val activeSubscription = accountInfoWrapper.activeSubscription
        val oldSku = activeSubscription?.sku
        val purchaseToken = activeSubscription?.token
        val skuDetails = accountInfoWrapper.availableSkus.find { it.sku == productId }
            ?: throw ProductNotFoundException()
        Timber.d("oldSku is:%s, new sku is:%s", oldSku, skuDetails)
        Timber.d("Obfuscated account id is:%s", obfuscatedAccountId)
        //if user is upgrading, it take effect immediately otherwise wait until current plan expired
        val prorationMode =
            if (getProductLevel(skuDetails.sku) > getProductLevel(oldSku)) ProrationMode.IMMEDIATE_WITH_TIME_PRORATION else ProrationMode.DEFERRED
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
                        .setReplaceProrationMode(prorationMode)
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
            ensureConnect()
            billingClient.launchBillingFlow(activity, purchaseParamsBuilder.build())
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        applicationScope.launch(ioDispatcher) {
            if (result.responseCode == BillingClient.BillingResponseCode.OK
                && purchases.isNullOrEmpty().not()
            ) {
                val validPurchases = processPurchase(purchases.orEmpty())
                billingEvent.emit(BillingEvent.OnPurchaseUpdate(validPurchases))
            } else {
                Timber.w("onPurchasesUpdated failed, with result code: %s", result.responseCode)
            }
        }
    }

    override suspend fun querySkus(): List<MegaSku> = withContext(ioDispatcher) {
        ensureConnect()
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

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.w("Failed to get SkuDetails, error code is %s",
                result.billingResult.responseCode)
        }
        val productsDetails = result.productDetailsList.orEmpty()
        productDetailsListCache.set(productsDetails)
        val megaSkus = productsDetails
            .mapNotNull {
                megaSkuMapper(it)
            }
        // legacy support, we still need to save into MyAccountInfo, will remove after refactoring
        accountInfoWrapper.availableSkus = megaSkus
        return@withContext megaSkus
    }

    override suspend fun queryPurchase(): List<MegaPurchase> = withContext(ioDispatcher) {
        ensureConnect()
        // check the caller still observer, sometime it takes time to connect to BillingClient
        ensureActive()
        val inAppPurchaseResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP)
                .build())
        val purchases = if (areSubscriptionsSupported()) {
            val subscriptionPurchaseResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS)
                    .build())
            inAppPurchaseResult.purchasesList + subscriptionPurchaseResult.purchasesList
        } else {
            inAppPurchaseResult.purchasesList
        }
        if (inAppPurchaseResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases.isEmpty()) {
            Timber.w("Query of purchases failed, result code is %d, is purchase empty: %s",
                inAppPurchaseResult.billingResult.responseCode,
                purchases.isEmpty())
            return@withContext emptyList()
        }
        return@withContext processPurchase(purchases)
    }

    private suspend fun processPurchase(purchaseList: List<Purchase>): List<MegaPurchase> {
        // Verify all available purchases
        val validPurchases = purchaseList.filter { purchase ->
            purchase.accountIdentifiers?.obfuscatedAccountId == obfuscatedAccountId
                    && verifyValidSignature(purchase.originalJson, purchase.signature)
        }
        validPurchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Acknowledge the purchase if it hasn't already been acknowledged.
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val result = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
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
        return validMegaPurchases
    }

    private fun areSubscriptionsSupported(): Boolean {
        val responseCode: Int =
            billingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode
        Timber.d("areSubscriptionsSupported %s",
            responseCode == BillingClient.BillingResponseCode.OK)
        return responseCode == BillingClient.BillingResponseCode.OK
    }

    // to make thread safe we need to wrap into Mutex for synchronized coroutine
    private suspend fun ensureConnect() {
        mutex.withLock {
            if (billingClient.isReady) return
            suspendCoroutine { continuation ->
                val listener = object : BillingClientStateListener {
                    override fun onBillingServiceDisconnected() {
                    }

                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            continuation.resumeWith(Result.success(Unit))
                        } else {
                            continuation.resumeWith(Result.failure(ConnectBillingServiceException(
                                result.responseCode,
                                result.debugMessage)))
                        }
                    }
                }
                billingClient.startConnection(listener)
            }
        }
    }

    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        return runCatching {
            verifyPurchaseGateway.verifyPurchase(signedData,
                signature)
        }.getOrElse { false }
    }

    private fun updateAccountInfo(purchases: List<MegaPurchase>) {
        val max = purchases.maxByOrNull { getProductLevel(it.sku) }
        accountInfoWrapper.updateActiveSubscription(max, getProductLevel(max?.sku))
    }

    private fun getProductLevel(sku: String?): Int {
        return when (sku) {
            SKU_PRO_LITE_MONTH, SKU_PRO_LITE_YEAR -> 0
            SKU_PRO_I_MONTH, SKU_PRO_I_YEAR -> 1
            SKU_PRO_II_MONTH, SKU_PRO_II_YEAR -> 2
            SKU_PRO_III_MONTH, SKU_PRO_III_YEAR -> 3
            else -> -1
        }
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