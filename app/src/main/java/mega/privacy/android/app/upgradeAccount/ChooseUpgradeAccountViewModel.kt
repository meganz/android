package mega.privacy.android.app.upgradeAccount

import android.content.Context
import android.text.Spanned
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.Product
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.service.iab.BillingManagerImpl
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.DBUtil.callToPaymentMethods
import mega.privacy.android.app.utils.DBUtil.callToPricing
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.getSizeStringGBBased
import mega.privacy.android.app.utils.billing.PaymentUtils.getSku
import mega.privacy.android.app.utils.billing.PaymentUtils.getSkuDetails
import nz.mega.sdk.MegaApiJava
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChooseUpgradeAccountViewModel @Inject constructor(
    private val myAccountInfo: MyAccountInfo
) : BaseRxViewModel() {

    companion object {
        const val TYPE_STORAGE_LABEL = 0
        const val TYPE_TRANSFER_LABEL = 1

        const val NOT_SUBSCRIBED = 0
        const val MONTHLY_SUBSCRIBED = 1
        const val YEARLY_SUBSCRIBED = 2
    }

    private val _currentSubscription = MutableLiveData<Pair<Int, SubscriptionMethod?>>()
    val currentSubscription: LiveData<Pair<Int, SubscriptionMethod?>>
        get() = _currentSubscription

    /**
     * Check the current subscription
     * @param upgradeType upgrade type
     */
    fun subscriptionCheck(upgradeType: Int){
        SubscriptionMethod.values().firstOrNull {
            // Determines the current account if has the subscription and the current subscription
            // platform if is same as current payment platform.
            if (BillingManagerImpl.PAYMENT_GATEWAY == MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET) {
                it.methodId != MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET
                        && it.methodId == myAccountInfo.subscriptionMethodId
            } else {
                it.methodId != MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET
                        && it.methodId == myAccountInfo.subscriptionMethodId
            }
        }.run {
            _currentSubscription.value = Pair(upgradeType, this)
        }
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

    /**
     * Asks for account info if needed.
     */
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

    /**
     * Gets a formatted string to show in a payment plan.
     *
     * @param context          Current context.
     * @param product          Selected product plan.
     * @param monthlyBasePrice True if the plan is monthly based, false otherwise.
     * @return The formatted string.
     */
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
        var color = getColorHexString(context, R.color.grey_087_white_087)

        if (monthlyBasePrice) {
            if (product.months != 1) {
                return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }

            when (product.level) {
                PRO_I, PRO_II, PRO_III -> color =
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

    /**
     * Gets the string to show as storage or transfer size label.
     *
     * @param context   Current context.
     * @param gb        Size to show in the string.
     * @param labelType TYPE_STORAGE_LABEL or TYPE_TRANSFER_LABEL.
     * @return The formatted string.
     */
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

    /**
     * Gets the label to show depending on labelType.
     *
     * @param labelType TYPE_STORAGE_LABEL or TYPE_TRANSFER_LABEL.
     */
    private fun storageOrTransferLabel(labelType: Int): String? {
        return when (labelType) {
            TYPE_STORAGE_LABEL -> getString(R.string.label_storage_upgrade_account)
            TYPE_TRANSFER_LABEL -> getString(R.string.label_transfer_quota_upgrade_account)
            else -> ""
        }
    }

    /**
     * Gets the subscription depending on the upgrade type.
     *
     * @param upgradeType Type of upgrade.
     * @return The subscription type:
     *          - MONTHLY_SUBSCRIBED if already subscribed to the monthly plan
     *          - YEARLY_SUBSCRIBED if already subscribed to the yearly plan
     *          - NOT_SUBSCRIBED if not subscribed.
     */
    fun getSubscription(upgradeType: Int): Int =
        when (upgradeType) {
            PRO_I -> {
                when {
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_I_MONTH) -> MONTHLY_SUBSCRIBED
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_I_YEAR) -> YEARLY_SUBSCRIBED
                    else -> NOT_SUBSCRIBED
                }
            }
            PRO_II -> {
                when {
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_II_MONTH) -> MONTHLY_SUBSCRIBED
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_II_YEAR) -> YEARLY_SUBSCRIBED
                    else -> NOT_SUBSCRIBED
                }
            }
            PRO_III -> {
                when {
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_III_MONTH) -> MONTHLY_SUBSCRIBED
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_III_YEAR) -> YEARLY_SUBSCRIBED
                    else -> NOT_SUBSCRIBED
                }
            }
            PRO_LITE -> {
                when {
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_LITE_MONTH) -> MONTHLY_SUBSCRIBED
                    isPurchasedAlready(BillingManagerImpl.SKU_PRO_LITE_YEAR) -> YEARLY_SUBSCRIBED
                    else -> NOT_SUBSCRIBED
                }
            }
            else -> NOT_SUBSCRIBED
        }
}