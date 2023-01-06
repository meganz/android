package mega.privacy.android.app.utils.billing

import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.middlelayer.iab.BillingConstant.PAYMENT_GATEWAY
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_III_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_III_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_II_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_II_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_I_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_I_YEAR
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_MONTH
import mega.privacy.android.domain.entity.account.Skus.SKU_PRO_LITE_YEAR
import mega.privacy.android.domain.entity.billing.MegaPurchase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import timber.log.Timber

object PaymentUtils {
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
     * Updates subscription level.
     *
     * @param myAccountInfo MyAccountInfo to check active subscription
     * @param dbH           DatabaseHandler to get attributes
     * @param megaApi       MegaApiAndroid to submit purchase receipt
     */
    @JvmStatic
    fun updateSubscriptionLevel(
        myAccountInfo: MyAccountInfo,
        activeSubscription: MegaPurchase?,
        dbH: DatabaseHandler,
        megaApi: MegaApiAndroid,
    ) {

        if (!myAccountInfo.isAccountDetailsFinished || activeSubscription == null) {
            return
        }

        val json = activeSubscription.receipt
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

        if (activeSubscription.level > myAccountInfo.levelAccountDetails) {
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
}