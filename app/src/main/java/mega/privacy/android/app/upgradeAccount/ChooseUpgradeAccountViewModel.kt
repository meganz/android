package mega.privacy.android.app.upgradeAccount

import android.content.Context
import android.text.Spanned
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.middlelayer.iab.BillingConstant
import mega.privacy.android.app.utils.ColorUtils.getColorHexString
import mega.privacy.android.app.utils.Constants.FREE
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.convertToBitSet
import mega.privacy.android.app.utils.Util.getSizeStringGBBased
import mega.privacy.android.app.utils.billing.PaymentUtils.getSku
import mega.privacy.android.app.utils.billing.PaymentUtils.getSkuDetails
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.data.mapper.PaymentMethodTypeMapper
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
internal class ChooseUpgradeAccountViewModel @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    private val getPaymentMethod: GetPaymentMethod,
    private val getPricing: GetPricing,
    private val paymentMethodTypeMapper: PaymentMethodTypeMapper,
) : ViewModel() {

    companion object {
        const val TYPE_STORAGE_LABEL = 0
        const val TYPE_TRANSFER_LABEL = 1

        const val NOT_SUBSCRIBED = 0
        const val MONTHLY_SUBSCRIBED = 1
        const val YEARLY_SUBSCRIBED = 2
    }

    private val upgradeClick = SingleLiveEvent<Int>()
    private val currentUpgradeClickedAndSubscription =
        MutableLiveData<Pair<Int, PaymentMethod>?>()

    private val _state = MutableStateFlow(ChooseUpgradeAccountState())

    /**
     * Payment method
     */
    val state = _state.asStateFlow()

    fun onUpgradeClick(): LiveData<Int> = upgradeClick
    fun onUpgradeClickWithSubscription(): LiveData<Pair<Int, PaymentMethod>?> =
        currentUpgradeClickedAndSubscription

    init {
        getPaymentMethod()
        refreshPricing()
    }

    /**
     * Get payment method
     *
     */
    fun getPaymentMethod() {
        viewModelScope.launch {
            val paymentMethod = getPaymentMethod(false)
            _state.update {
                it.copy(paymentBitSet = convertToBitSet(paymentMethod.flag))
            }
        }
    }

    /**
     * Check the current subscription
     * @param upgradeType upgrade type
     */
    fun subscriptionCheck(upgradeType: Int) {
        PaymentMethod.values().firstOrNull {
            // Determines the current account if has the subscription and the current subscription
            // platform if is same as current payment platform.
            if (BillingConstant.PAYMENT_GATEWAY == MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET) {
                it.methodId != PaymentMethodType.GOOGLE_WALLET
                        && it.methodId == paymentMethodTypeMapper(myAccountInfo.subscriptionMethodId)
            } else {
                it.methodId != PaymentMethodType.HUAWEI_WALLET
                        && it.methodId == paymentMethodTypeMapper(myAccountInfo.subscriptionMethodId)
            }
        }.run {
            if (this == null) {
                upgradeClick.value = upgradeType
            } else {
                currentUpgradeClickedAndSubscription.value = Pair(upgradeType, this)
            }
        }
    }

    /**
     * Resets the currentUpgradeClickedAndSubscription ensuring the subscription warning
     * is not shown again.
     */
    fun dismissSubscriptionWarningClicked() {
        currentUpgradeClickedAndSubscription.value = null
    }

    fun isGettingInfo(): Boolean =
        myAccountInfo.accountType < FREE || myAccountInfo.accountType > PRO_LITE

    fun getAccountType(): Int = myAccountInfo.accountType

    fun isPurchasedAlready(sku: String): Boolean = myAccountInfo.isPurchasedAlready(sku)

    fun getProductAccounts(): List<Product> = state.value.product

    fun isBillingAvailable(): Boolean = myAccountInfo.availableSkus.isNotEmpty()

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
     * @param monthlyBasePrice True if the plan is monthly based, false otherwise.
     * @return The formatted string.
     */
    fun getPriceString(
        context: Context,
        product: Product,
        monthlyBasePrice: Boolean,
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
        var textToShow = storageOrTransferLabel(gb, labelType)
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
    private fun storageOrTransferLabel(gb: Long, labelType: Int): String {
        return when (labelType) {
            TYPE_STORAGE_LABEL -> getString(R.string.account_upgrade_storage_label,
                getSizeStringGBBased(gb))
            TYPE_TRANSFER_LABEL -> getString(R.string.account_upgrade_transfer_quota_label,
                getSizeStringGBBased(gb))
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
    fun getSubscription(upgradeType: Int): Int {
        val skus = when (upgradeType) {
            PRO_I -> Skus.SKU_PRO_I_MONTH to Skus.SKU_PRO_I_YEAR
            PRO_II -> Skus.SKU_PRO_II_MONTH to Skus.SKU_PRO_II_YEAR
            PRO_III -> Skus.SKU_PRO_III_MONTH to Skus.SKU_PRO_III_YEAR
            PRO_LITE -> Skus.SKU_PRO_LITE_MONTH to Skus.SKU_PRO_LITE_MONTH
            else -> "" to ""
        }
        return when {
            skus.first.isNotEmpty() && isPurchasedAlready(skus.first) -> MONTHLY_SUBSCRIBED
            skus.second.isNotEmpty() && isPurchasedAlready(skus.second) -> YEARLY_SUBSCRIBED
            else -> NOT_SUBSCRIBED
        }
    }
}