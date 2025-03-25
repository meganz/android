package mega.privacy.android.app.presentation.login.confirmemail.changeemail

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

/**
 * UI State for ChangeEmailAddressScreen
 */
internal data class ChangeEmailAddressUIState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isEmailValid: Boolean? = null,
    val changeEmailAddressSuccessEvent: StateEvent = consumed,
    val accountExistEvent: StateEvent = consumed,
    val generalErrorEvent: StateEvent = consumed
)
