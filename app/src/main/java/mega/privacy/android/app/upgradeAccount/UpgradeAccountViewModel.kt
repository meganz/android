package mega.privacy.android.app.upgradeAccount

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.text.Spanned
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.*
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.middlelayer.iab.BillingManager
import mega.privacy.android.app.middlelayer.iab.BillingManager.RequestCode
import mega.privacy.android.app.middlelayer.iab.BillingUpdatesListener
import mega.privacy.android.app.middlelayer.iab.MegaPurchase
import mega.privacy.android.app.middlelayer.iab.MegaSku
import mega.privacy.android.app.service.iab.BillingManagerImpl
import mega.privacy.android.app.service.iab.BillingManagerImpl.*
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.DBUtil.callToPaymentMethods
import mega.privacy.android.app.utils.DBUtil.callToPricing
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.getSizeStringGBBased
import mega.privacy.android.app.utils.billing.PaymentUtils.getSku
import mega.privacy.android.app.utils.billing.PaymentUtils.getSubscriptionRenewalType
import mega.privacy.android.app.utils.billing.PaymentUtils.getSubscriptionType
import mega.privacy.android.app.utils.billing.PaymentUtils.updateSubscriptionLevel
import nz.mega.sdk.MegaApiAndroid
import java.text.NumberFormat
import java.util.*

class UpgradeAccountViewModel @ViewModelInject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler
) : BaseRxViewModel(), BillingUpdatesListener {

    companion object {
        const val TYPE_STORAGE_LABEL = 0
        const val TYPE_TRANSFER_LABEL = 1
    }

    private val queryPurchasesMessage: MutableLiveData<String?> = MutableLiveData()
    private val updatePricing: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var billingManager: BillingManager
    private lateinit var skuDetailsList: List<MegaSku>

    fun getQueryPurchasesMessage(): LiveData<String?> = queryPurchasesMessage
    fun onUpdatePricing(): LiveData<Boolean> = updatePricing

    fun resetQueryPurchasesMessage() {
        queryPurchasesMessage.value = null
    }

    fun resetUpdatePricing() {
        updatePricing.value = false
    }

    fun isGettingInfo(): Boolean =
        myAccountInfo.accountType < FREE || myAccountInfo.accountType > PRO_LITE

    fun getAccountType(): Int = myAccountInfo.accountType

    fun getPaymentBitSet(): BitSet? = myAccountInfo.paymentBitSet

    fun isInventoryFinished(): Boolean = myAccountInfo.isInventoryFinished

    fun isPurchasedAlready(sku: String): Boolean = myAccountInfo.isPurchasedAlready(sku)

    fun getProductAccounts(): ArrayList<Product>? = myAccountInfo.productAccounts

    fun checkProductAccounts(): ArrayList<Product>? {
        val productAccounts = getProductAccounts()

        return if (productAccounts == null) {
            MegaApplication.getInstance().askForPricing()
            null
        } else productAccounts
    }

    fun refreshAccountInfo() {
        logDebug("Check the last call to callToPricing")
        if (callToPricing()) {
            logDebug("megaApi.getPricing SEND")
            MegaApplication.getInstance().askForPricing()
        }

        logDebug("Check the last call to callToPaymentMethods")
        if (callToPaymentMethods()) {
            logDebug("megaApi.getPaymentMethods SEND")
            MegaApplication.getInstance().askForPaymentMethods()
        }
    }

    fun getPriceString(
        context: Context,
        product: Product,
        monthlyBasePrice: Boolean
    ): Spanned {
        // First get the "default" pricing details from the MEGA server
        var price = product.amount / 100.00
        var currency = product.currency

        // Try get the local pricing details from the store if available
        val details = getSkuDetails(myAccountInfo.availableSkus, getSku(product))

        if (details != null) {
            price = details.priceAmountMicros / 1000000.00
            currency = details.priceCurrencyCode
        }

        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(currency)

        var stringPrice = format.format(price)
        var color = getColorHexString(context, R.color.grey_900_grey_100)

        if (monthlyBasePrice) {
            if (product.months != 1) {
                return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }

            when (product.level) {
                Constants.PRO_I, Constants.PRO_II, Constants.PRO_III -> color =
                    ContextCompat.getColor(context, R.color.red_600_red_300).toString()
                PRO_LITE -> color =
                    ContextCompat.getColor(context, R.color.orange_400_orange_300).toString()
            }

            stringPrice = getString(R.string.type_month, stringPrice)
        } else {
            stringPrice = getString(
                if (product.months == 12) R.string.billed_yearly_text else R.string.billed_monthly_text,
                stringPrice
            )
        }

        try {
            stringPrice = stringPrice.replace("[A]", "<font color='$color'>")
            stringPrice = stringPrice.replace("[/A]", "</font>")
        } catch (e: Exception) {
            logError("Exception formatting string", e)
        }

        return HtmlCompat.fromHtml(stringPrice, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    fun generateByteString(context: Context, gb: Long, labelType: Int): Spanned {
        var textToShow =
            "[A] " + getSizeStringGBBased(gb) + " [/A] " + storageOrTransferLabel(labelType)
        try {
            textToShow = textToShow.replace(
                "[A]", "<font color='"
                        + getColorHexString(context, R.color.grey_900_grey_100)
                        + "'>"
            )
            textToShow = textToShow.replace("[/A]", "</font>")
        } catch (e: java.lang.Exception) {
            logError("Exception formatting string", e)
        }
        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun storageOrTransferLabel(labelType: Int): String? {
        return when (labelType) {
            TYPE_STORAGE_LABEL -> getString(R.string.label_storage_upgrade_account)
            TYPE_TRANSFER_LABEL -> getString(R.string.label_transfer_quota_upgrade_account)
            else -> ""
        }
    }

    fun initPayments(activity: Activity) {
        billingManager = BillingManagerImpl(activity, this)
    }

    fun destroyPayments() {
        if (this::billingManager.isInitialized) {
            billingManager.destroy()
        }
    }

    /**
     * Starts purchase/subscription flow
     *
     * @param productId Selected subscription.
     */
    fun launchPayment(productId: String) {
        val skuDetails = getSkuDetails(skuDetailsList, productId) ?: return
        val purchase = myAccountInfo.activeSubscription
        val oldSku = purchase?.sku
        val token = purchase?.token

        if (this::billingManager.isInitialized) {
            billingManager.initiatePurchaseFlow(oldSku, token, skuDetails)
        }
    }

    private fun getSkuDetails(list: List<MegaSku>?, key: String): MegaSku? {
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

    override fun onBillingClientSetupFinished() {
        billingManager.getInventory { skuList ->
            skuDetailsList = skuList
            myAccountInfo.availableSkus = skuList
        }
    }

    override fun onPurchasesUpdated(
        isFailed: Boolean,
        resultCode: Int,
        purchases: MutableList<MegaPurchase>?
    ) {
        if (isFailed) {
            logWarning("Update purchase failed, with result code: $resultCode")
            return
        }

        val message: String

        if (purchases != null && purchases.isNotEmpty()) {
            val purchase = purchases[0]
            //payment may take time to process, we will not give privilege until it has been fully processed
            val sku = purchase.sku
            val subscriptionType = getSubscriptionType(sku)
            val subscriptionRenewalType = getSubscriptionRenewalType(sku)

            if (billingManager.isPurchased(purchase)) {
                //payment has been processed
                updateAccountInfo(purchases)
                logDebug("Purchase $sku successfully, subscription type is: $subscriptionType, subscription renewal type is: $subscriptionRenewalType")

                message = getString(
                    R.string.message_user_purchased_subscription,
                    subscriptionType,
                    subscriptionRenewalType
                )

                updateSubscriptionLevel(myAccountInfo, dbH, megaApi)
            } else {
                //payment is being processed or in unknown state
                logDebug("Purchase $sku is being processed or in unknown state.")
                message = getString(R.string.message_user_payment_pending)
            }
        } else {
            //down grade case
            logDebug("Downgrade, the new subscription takes effect when the old one expires.")
            message = getString(R.string.message_user_purchased_subscription_down_grade)
        }

        queryPurchasesMessage.value = message
    }

    override fun onQueryPurchasesFinished(
        isFailed: Boolean,
        resultCode: Int,
        purchases: MutableList<MegaPurchase>?
    ) {
        if (isFailed || purchases == null) {
            logWarning("Query of purchases failed, result code is " + resultCode + ", is purchase null: " + (purchases == null))
            return
        }

        updateAccountInfo(purchases)
        updateSubscriptionLevel(myAccountInfo, dbH, megaApi)
    }

    private fun updateAccountInfo(purchases: List<MegaPurchase>) {
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
            logDebug("Set current max subscription: $max")
            myAccountInfo.activeSubscription = max
        } else {
            myAccountInfo.activeSubscription = null
        }

        myAccountInfo.levelInventory = highest
        myAccountInfo.isInventoryFinished = true
        updatePricing.value = true
    }

    fun manageActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        if (requestCode == RequestCode.REQ_CODE_BUY) {
            // For HMS purchase only
            if (resultCode != RESULT_OK) {
                logWarning("Cancel subscribe")
                return
            }

            val purchaseResult = billingManager.getPurchaseResult(intent)

            if (BillingManager.ORDER_STATE_SUCCESS == purchaseResult) {
                billingManager.updatePurchase()
            } else {
                logWarning("Purchase failed, error code: $purchaseResult")
            }
        }
    }
}