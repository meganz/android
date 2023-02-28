package mega.privacy.android.app.upgradeAccount.payment

import mega.privacy.android.domain.entity.Product

internal data class PaymentUiState(
    val isPaymentMethodAvailable: Boolean = true,
    val product: List<Product> = emptyList(),
)