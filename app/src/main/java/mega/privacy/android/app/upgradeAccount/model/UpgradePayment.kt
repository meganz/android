package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.PaymentMethod

data class UpgradePayment(
    val upgradeType: Int = Constants.INVALID_VALUE,
    val currentPayment: PaymentMethod? = null,
)
