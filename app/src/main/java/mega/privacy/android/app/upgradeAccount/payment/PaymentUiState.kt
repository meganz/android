package mega.privacy.android.app.upgradeAccount.payment

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import mega.privacy.android.app.upgradeAccount.model.UserSubscription
import mega.privacy.android.app.utils.Constants

internal data class PaymentUiState(
    val isPaymentMethodAvailable: Boolean = true,
    val monthlyPrice: String = "",
    val yearlyPrice: String = "",
    val userSubscription: UserSubscription = UserSubscription.NOT_SUBSCRIBED,
    @StringRes val title: Int = 0,
    @ColorRes val titleColor: Int = 0,
    val isMonthlySelected: Boolean = false,
    val upgradeType: Int = Constants.INVALID_VALUE,
)