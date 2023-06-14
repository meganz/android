package mega.privacy.android.app.upgradeAccount.payment

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.UserSubscription
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.PRO_I
import mega.privacy.android.app.utils.Constants.PRO_II
import mega.privacy.android.app.utils.Constants.PRO_III
import mega.privacy.android.app.utils.Constants.PRO_LITE
import mega.privacy.android.app.utils.billing.PaymentUtils.getSku
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.account.Skus
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.billing.Pricing
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetActiveSubscription
import mega.privacy.android.domain.usecase.billing.GetLocalPricingUseCase
import mega.privacy.android.domain.usecase.billing.IsBillingAvailableUseCase
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
internal class PaymentViewModel @Inject constructor(
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase,
    private val getPricing: GetPricing,
    private val getLocalPricingUseCase: GetLocalPricingUseCase,
    private val isBillingAvailableUseCase: IsBillingAvailableUseCase,
    private val getActiveSubscription: GetActiveSubscription,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val upgradeType =
        savedStateHandle[PaymentActivity.UPGRADE_TYPE] ?: Constants.INVALID_VALUE

    private val _state = MutableStateFlow(
        PaymentUiState(
            upgradeType = upgradeType,
            title = getUpdateTypeText(upgradeType),
            titleColor = getUpdateTypeTextColor(upgradeType),
            userSubscription = getSubscription(upgradeType)
        )
    )

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
            val paymentMethod =
                runCatching { getPaymentMethodUseCase(false) }.getOrElse { PaymentMethodFlags(0L) }
            if (paymentMethod.flag == 0L) {
                Timber.w("Not payment bit set received!!!")
            }
            val isBillingAvailable = isBillingAvailableUseCase()
                    && ((paymentMethod.flag and (1L shl MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET)) != 0L) // check bit enable
            _state.update {
                it.copy(isPaymentMethodAvailable = isBillingAvailable)
            }
        }
    }

    private fun isPurchasedAlready(sku: String): Boolean = getActiveSubscription()?.sku == sku

    /**
     * Asks for pricing if needed.
     */
    private fun refreshPricing() {
        viewModelScope.launch {
            val pricing = runCatching { getPricing(false) }.getOrElse {
                Timber.w("Returning empty pricing as get pricing failed.", it)
                Pricing(emptyList())
            }
            val currentProducts =
                pricing.products.filter { product -> product.level == _state.value.upgradeType }
            val monthlyPrice =
                currentProducts.find { it.isMonthly }?.let { getPrice(context, it) }.orEmpty()
            val yearlyPrice =
                currentProducts.find { it.isYearly }?.let { getPrice(context, it) }.orEmpty()
            _state.update {
                it.copy(
                    monthlyPrice = monthlyPrice,
                    yearlyPrice = yearlyPrice
                )
            }
        }
    }

    private fun getPrice(
        context: Context,
        product: Product,
    ): String {
        // Try get the local pricing details from the store if available otherwise use "default" from Mega
        val details = getLocalPricingUseCase(getSku(product))
        val (price, currency) = if (details != null) {
            details.amount.value / 1000000.00 to details.currency.code
        } else {
            product.amount / 100.00 to product.currency
        }
        val format = NumberFormat.getCurrencyInstance().apply {
            this.currency = Currency.getInstance(currency)
        }
        return context.getString(
            if (product.isYearly) R.string.billed_yearly_text else R.string.billed_monthly_text,
            format.format(price)
        )
    }

    /**
     * Gets the subscription depending on the upgrade type.
     *
     * @param upgradeType Type of upgrade.
     * @return The [UserSubscription]
     */
    private fun getSubscription(upgradeType: Int): UserSubscription {
        val skus = getSkus(upgradeType)
        return when {
            skus.first.isNotEmpty() && isPurchasedAlready(skus.first) -> UserSubscription.MONTHLY_SUBSCRIBED
            skus.second.isNotEmpty() && isPurchasedAlready(skus.second) -> UserSubscription.YEARLY_SUBSCRIBED
            else -> UserSubscription.NOT_SUBSCRIBED
        }
    }

    /**
     * Get product id
     *
     */
    fun getProductId(isMonthly: Boolean, upgradeType: Int): String {
        val skus = getSkus(upgradeType)
        return if (isMonthly) skus.first else skus.second
    }

    private fun getSkus(upgradeType: Int) = when (upgradeType) {
        PRO_I -> Skus.SKU_PRO_I_MONTH to Skus.SKU_PRO_I_YEAR
        PRO_II -> Skus.SKU_PRO_II_MONTH to Skus.SKU_PRO_II_YEAR
        PRO_III -> Skus.SKU_PRO_III_MONTH to Skus.SKU_PRO_III_YEAR
        PRO_LITE -> Skus.SKU_PRO_LITE_MONTH to Skus.SKU_PRO_LITE_YEAR
        else -> "" to ""
    }

    /**
     * On select change
     *
     * @param isMonthly
     */
    fun onSelectChange(isMonthly: Boolean) {
        _state.update {
            it.copy(isMonthlySelected = isMonthly)
        }
    }

    @StringRes
    private fun getUpdateTypeText(upgradeType: Int) = when (upgradeType) {
        PRO_LITE -> R.string.prolite_account
        PRO_I -> R.string.pro1_account
        PRO_II -> R.string.pro2_account
        PRO_III -> R.string.pro3_account
        else -> 0
    }

    @ColorRes
    private fun getUpdateTypeTextColor(upgradeType: Int) = when (upgradeType) {
        PRO_LITE -> R.color.orange_400_orange_300
        PRO_I, PRO_II, PRO_III -> R.color.red_600_red_300
        else -> 0
    }
}