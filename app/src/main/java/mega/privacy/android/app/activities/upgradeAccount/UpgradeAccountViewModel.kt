package mega.privacy.android.app.activities.upgradeAccount

import android.content.Context
import android.text.Spanned
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.Product
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.FREE
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.billing.PaymentUtils
import java.text.NumberFormat
import java.util.*

class UpgradeAccountViewModel @ViewModelInject constructor(
    private val myAccountInfo: MyAccountInfo
) : BaseRxViewModel() {

    companion object {
        const val TYPE_STORAGE_LABEL = 0
        const val TYPE_TRANSFER_LABEL = 1
    }

    fun isGettingInfo(): Boolean =
        myAccountInfo.accountType < FREE || myAccountInfo.accountType > PRO_LITE

    fun getAccountType(): Int = myAccountInfo.accountType

    fun getProductAccounts(): ArrayList<Product>? = myAccountInfo.productAccounts

    fun refreshAccountInfo() {
        logDebug("Check the last call to callToPricing")
        if (DBUtil.callToPricing()) {
            logDebug("megaApi.getPricing SEND")
            MegaApplication.getInstance().askForPricing()
        }

        logDebug("Check the last call to callToPaymentMethods")
        if (DBUtil.callToPaymentMethods()) {
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
        val details =
            PaymentUtils.getSkuDetails(myAccountInfo.availableSkus, PaymentUtils.getSku(product))

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
            "[A] " + Util.getSizeStringGBBased(gb) + " [/A] " + storageOrTransferLabel(labelType)
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
}