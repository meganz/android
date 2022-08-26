package mega.privacy.android.app.utils.billing

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.Product
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.middlelayer.iab.MegaPurchase
import mega.privacy.android.app.middlelayer.iab.MegaSku
import mega.privacy.android.app.service.iab.BillingManagerImpl.PAYMENT_GATEWAY
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_III_MONTH
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_III_YEAR
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_II_MONTH
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_II_YEAR
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_I_MONTH
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_I_YEAR
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_LITE_MONTH
import mega.privacy.android.app.service.iab.BillingManagerImpl.SKU_PRO_LITE_YEAR
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Constants.UPDATE_GET_PRICING
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import timber.log.Timber

object PaymentUtils {
    /**
     * Get the level of a certain sku.
     *
     * @param sku The id of the sku item.
     * @return The level of the sku.
     */
    @JvmStatic
    fun getProductLevel(sku: String?): Int {
        return when (sku) {
            SKU_PRO_LITE_MONTH, SKU_PRO_LITE_YEAR -> 0
            SKU_PRO_I_MONTH, SKU_PRO_I_YEAR -> 1
            SKU_PRO_II_MONTH, SKU_PRO_II_YEAR -> 2
            SKU_PRO_III_MONTH, SKU_PRO_III_YEAR -> 3
            else -> INVALID_VALUE
        }
    }

    /**
     * Get renewal type of a certain sku item.
     *
     * @param sku The id of the sku item.
     * @return The renewal type of the sku item, Monthly or Yearly.
     */
    @JvmStatic
    fun getSubscriptionRenewalType(sku: String?): String? {
        return when (sku) {
            SKU_PRO_LITE_MONTH, SKU_PRO_I_MONTH, SKU_PRO_II_MONTH, SKU_PRO_III_MONTH ->
                getString(R.string.subscription_type_monthly)

            SKU_PRO_LITE_YEAR, SKU_PRO_I_YEAR, SKU_PRO_II_YEAR, SKU_PRO_III_YEAR ->
                getString(R.string.subscription_type_yearly)

            else -> ""
        }
    }

    /**
     * Get type name of a certain sku item.
     *
     * @param sku The id of the sku item.
     * @return The type name of the sku.
     */
    @JvmStatic
    fun getSubscriptionType(sku: String?): String? {
        return when (sku) {
            SKU_PRO_LITE_MONTH, SKU_PRO_LITE_YEAR -> getString(R.string.prolite_account)
            SKU_PRO_I_MONTH, SKU_PRO_I_YEAR -> getString(R.string.pro1_account)
            SKU_PRO_II_MONTH, SKU_PRO_II_YEAR -> getString(R.string.pro2_account)
            SKU_PRO_III_MONTH, SKU_PRO_III_YEAR -> getString(R.string.pro3_account)
            else -> ""
        }
    }

    /**
     * Gets the Google Play SKU associated to a product.
     *
     * @param product Product to get the SKU.
     * @return SKU of the product
     */
    @JvmStatic
    fun getSku(product: Product?): String {
        return when (product?.level) {
            PRO_LITE -> if (product.months == 1) SKU_PRO_LITE_MONTH else SKU_PRO_LITE_YEAR
            PRO_I -> if (product.months == 1) SKU_PRO_I_MONTH else SKU_PRO_I_YEAR
            PRO_II -> if (product.months == 1) SKU_PRO_II_MONTH else SKU_PRO_II_YEAR
            PRO_III -> if (product.months == 1) SKU_PRO_III_MONTH else SKU_PRO_III_YEAR
            else -> ""
        }
    }

    /**
     * Gets the details of a SKU from current platform(Google play/Huawei app gallery).
     *
     * @param list List of available products in current platform.
     * @param key Key of the product to get the details.
     * @return Details of the product corresponding to the provided key.
     */
    @JvmStatic
    fun getSkuDetails(list: List<MegaSku>?, key: String): MegaSku? {
        if (list == null || list.isEmpty()) {
            return null
        }

        for (details in list) {
            if (details.sku == key) {
                return details
            }
        }

        return null
    }

    /**
     * Updates subscription level.
     *
     * @param myAccountInfo MyAccountInfo to check active subscription
     * @param dbH           DatabaseHandler to get attributes
     * @param megaApi       MegaApiAndroid to submit purchase receipt
     */
    @JvmStatic
    fun updateSubscriptionLevel(
        myAccountInfo: MyAccountInfo,
        dbH: DatabaseHandler,
        megaApi: MegaApiAndroid,
    ) {
        val highestGooglePlaySubscription = myAccountInfo.activeSubscription

        if (!myAccountInfo.isAccountDetailsFinished || highestGooglePlaySubscription == null) {
            return
        }

        val json = highestGooglePlaySubscription.receipt
        Timber.d("ORIGINAL JSON:$json") //Print JSON in logs to help debug possible payments issues

        val attributes: MegaAttributes? = dbH.attributes

        val lastPublicHandle: Long = attributes?.lastPublicHandle ?: -1
        val listener = OptionalMegaRequestListenerInterface(
            onRequestFinish = { _, error ->
                if (error.errorCode != MegaError.API_OK) {
                    Timber.e("PURCHASE WRONG: ${error.errorString} (${error.errorCode})")
                }
            }
        )

        if (myAccountInfo.levelInventory > myAccountInfo.levelAccountDetails) {
            Timber.d("megaApi.submitPurchaseReceipt is invoked")
            if (lastPublicHandle == MegaApiJava.INVALID_HANDLE) {
                megaApi.submitPurchaseReceipt(PAYMENT_GATEWAY, json, listener)
            } else {
                attributes?.run {
                    megaApi.submitPurchaseReceipt(PAYMENT_GATEWAY, json, lastPublicHandle,
                        lastPublicHandleType, lastPublicHandleTimeStamp, listener
                    )
                }

            }
        }
    }

    /**
     * Updates the account info after a purchase finished.
     *
     * @param context       Current Context.
     * @param purchases     List of purchases
     * @param myAccountInfo MyAccountInfo object.
     */
    @JvmStatic
    fun updateAccountInfo(
        context: Context,
        purchases: List<MegaPurchase>,
        myAccountInfo: MyAccountInfo,
    ) {
        var highest = INVALID_VALUE
        var temp = INVALID_VALUE
        var max: MegaPurchase? = null

        for (purchase in purchases) {
            when (purchase.sku) {
                SKU_PRO_LITE_MONTH, SKU_PRO_LITE_YEAR -> temp = 0
                SKU_PRO_I_MONTH, SKU_PRO_I_YEAR -> temp = 1
                SKU_PRO_II_MONTH, SKU_PRO_II_YEAR -> temp = 2
                SKU_PRO_III_MONTH, SKU_PRO_III_YEAR -> temp = 3
            }

            if (temp >= highest) {
                highest = temp
                max = purchase
            }
        }

        if (max != null) {
            Timber.d("Set current max subscription: $max")
            myAccountInfo.activeSubscription = max
        } else {
            myAccountInfo.activeSubscription = null
        }

        myAccountInfo.levelInventory = highest
        myAccountInfo.isInventoryFinished = true
        updatePricing(context)
    }

    /**
     * Launches a broadcast to update pricing info.
     *
     * @param context Current Context.
     */
    @JvmStatic
    fun updatePricing(context: Context) {
        context.sendBroadcast(
            Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
                .putExtra(ACTION_TYPE, UPDATE_GET_PRICING)
        )
    }
}