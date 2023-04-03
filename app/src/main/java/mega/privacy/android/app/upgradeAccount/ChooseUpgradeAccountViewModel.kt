package mega.privacy.android.app.upgradeAccount

import android.content.Context
import android.text.Spanned
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.Util.getSizeStringGBBased
import mega.privacy.android.app.utils.billing.PaymentUtils.getSku
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.usecase.GetLocalPricing
import mega.privacy.android.domain.usecase.GetPricing
import timber.log.Timber
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
internal class ChooseUpgradeAccountViewModel @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    private val getPricing: GetPricing,
    private val getLocalPricing: GetLocalPricing,
) : ViewModel() {

    companion object {
        const val TYPE_STORAGE_LABEL = 0
        const val TYPE_TRANSFER_LABEL = 1
    }

    private val _state = MutableStateFlow(ChooseUpgradeAccountState())

    /**
     * Payment method
     */
    val state = _state.asStateFlow()

    init {
        refreshPricing()
    }

    fun getAccountType(): Int = myAccountInfo.accountType

    fun getProductAccounts(): List<Product> = state.value.product

    /**
     * Asks for pricing if needed.
     */
    fun refreshPricing() {
        viewModelScope.launch {
            val pricing = getPricing(false)
            _state.update {
                it.copy(product = pricing.products)
            }
        }
    }

    /**
     * Gets a formatted string to show in a payment plan.
     *
     * @param context          Current context.
     * @param product          Selected product plan.
     * @return The formatted string.
     */
    fun getPriceString(
        context: Context,
        product: Product,
    ): Spanned {
        // First get the "default" pricing details from the MEGA server
        var price = product.amount / 100.00
        var currency = product.currency

        // Try get the local pricing details from the store if available
        val details = getLocalPricing(getSku(product))

        if (details != null) {
            price = details.amount.value / 1000000.00
            currency = details.currency.currency
        }

        val format = NumberFormat.getCurrencyInstance()
        format.currency = Currency.getInstance(currency)

        var stringPrice = format.format(price)
        var color = getColorHexString(context, R.color.grey_087_white_087)

        if (product.months != 1) {
            return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        when (product.level) {
            PRO_I, PRO_II, PRO_III -> color =
                ContextCompat.getColor(context, R.color.red_600_red_300).toString()
            PRO_LITE -> color =
                ContextCompat.getColor(context, R.color.orange_400_orange_300).toString()
        }

        stringPrice = context.getString(R.string.type_month, stringPrice)

        try {
            stringPrice = stringPrice.replace("[A]", "<font color='$color'>")
            stringPrice = stringPrice.replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
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
        var textToShow = storageOrTransferLabel(gb, labelType, context)
        try {
            textToShow = textToShow.replace(
                "[A]", "<font color='"
                        + getColorHexString(context, R.color.grey_900_grey_100)
                        + "'>"
            )
            textToShow = textToShow.replace("[/A]", "</font>")
        } catch (e: java.lang.Exception) {
            Timber.e(e, "Exception formatting string")
        }

        return HtmlCompat.fromHtml(textToShow, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    /**
     * Gets the label to show depending on labelType.
     *
     * @param gb        Size to show in the string.
     * @param labelType TYPE_STORAGE_LABEL or TYPE_TRANSFER_LABEL.
     */
    private fun storageOrTransferLabel(gb: Long, labelType: Int, context: Context): String {
        return when (labelType) {
            TYPE_STORAGE_LABEL -> context.getString(
                R.string.account_upgrade_storage_label,
                getSizeStringGBBased(gb)
            )
            TYPE_TRANSFER_LABEL -> context.getString(
                R.string.account_upgrade_transfer_quota_label,
                getSizeStringGBBased(gb)
            )
            else -> ""
        }
    }
}