package mega.privacy.android.app.upgradeAccount.payment

import android.content.Context
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.lifecycle.SavedStateHandle
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
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.convertToBitSet
import mega.privacy.android.app.utils.billing.PaymentUtils.getSku
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.usecase.GetLocalPricing
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetActiveSubscription
import mega.privacy.android.domain.usecase.billing.IsBillingAvailable
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
internal class PaymentViewModel @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    private val getPaymentMethod: GetPaymentMethod,
    private val getPricing: GetPricing,
    private val getLocalPricing: GetLocalPricing,
    private val isBillingAvailable: IsBillingAvailable,
    private val getActiveSubscription: GetActiveSubscription,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val upgradeType =
        savedStateHandle[PaymentActivity.UPGRADE_TYPE] ?: Constants.INVALID_VALUE

    companion object {
        const val NOT_SUBSCRIBED = 0
        const val MONTHLY_SUBSCRIBED = 1
        const val YEARLY_SUBSCRIBED = 2
    }

    private val _state = MutableStateFlow(PaymentUiState())

    /**
     * Payment method
     */
    val state = _state.asStateFlow()

    init {
        getPaymentMethod()
        refreshPricing()
    }

    /**
     * Get payment method
     *
     */
    private fun getPaymentMethod() {
        viewModelScope.launch {
            val paymentMethod = getPaymentMethod(false)
            val paymentBitSet = convertToBitSet(paymentMethod.flag)
            if (paymentBitSet.isEmpty) {
                Timber.w("Not payment bit set received!!!")
            }
            val isBillingAvailable = isBillingAvailable()
                    || !Util.isPaymentMethodAvailable(
                paymentBitSet,
                MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET
            )
            _state.update {
                it.copy(isPaymentMethodAvailable = isBillingAvailable)
            }
        }
    }

    fun getAccountType(): Int = myAccountInfo.accountType

    private fun isPurchasedAlready(sku: String): Boolean = getActiveSubscription()?.sku == sku

    /**
     * Asks for pricing if needed.
     */
    private fun refreshPricing() {
        viewModelScope.launch {
            val pricing = getPricing(false)
            _state.update {
                it.copy(product = pricing.products.filter { product -> product.level == upgradeType })
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
        val color = getColorHexString(context, R.color.grey_087_white_087)

        stringPrice = getString(
            if (product.months == 12) R.string.billed_yearly_text else R.string.billed_monthly_text,
            stringPrice
        )

        try {
            stringPrice = stringPrice.replace("[A]", "<font color='$color'>")
            stringPrice = stringPrice.replace("[/A]", "</font>")
        } catch (e: Exception) {
            Timber.e(e, "Exception formatting string")
        }

        return HtmlCompat.fromHtml(stringPrice, HtmlCompat.FROM_HTML_MODE_LEGACY)
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