package mega.privacy.android.feature.payment.presentation.cancelaccountplan.model.mapper

import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.model.CancellationInstructionsType
import javax.inject.Inject

/**
 * Mapper for PaymentMethod to map with Cancellation Instructions Type
 */
internal class CancellationInstructionsTypeMapper @Inject constructor() {
    internal operator fun invoke(
        currentPaymentType: PaymentMethod,
    ): CancellationInstructionsType? = when (currentPaymentType) {
        PaymentMethod.ITUNES -> CancellationInstructionsType.AppStore
        PaymentMethod.GOOGLE_WALLET -> CancellationInstructionsType.PlayStore
        PaymentMethod.STRIPE -> CancellationInstructionsType.WebClient
        PaymentMethod.ECP -> CancellationInstructionsType.WebClient
        PaymentMethod.STRIPE2 -> CancellationInstructionsType.WebClient
        else -> null
    }
}