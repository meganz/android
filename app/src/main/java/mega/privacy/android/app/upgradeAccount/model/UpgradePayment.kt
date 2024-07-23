package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.PaymentMethod

data class UpgradePayment(
    val upgradeType: AccountType = AccountType.UNKNOWN,
    val currentPayment: PaymentMethod? = null,
)
