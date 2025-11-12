package mega.privacy.android.feature.payment.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

data class BillingUIState(
    val generalError: StateEvent = consumed,
    val onExternalPurchaseClick: StateEventWithContent<String> = consumed(),
)
