package mega.privacy.android.app.presentation.twofactorauthentication.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed


/**
 * UI State for Two Factor authentication activity
 * @param is2FAFetchCompleted UI state to check if fetching the 2FA code was completed or not
 * @param isRkExportedSuccessfullyEvent UI state to check if exporting the RK was exported successfully or not
 * @param writePermissionDeniedEvent Triggered if the write permission was denied by the user
 * @param seedCopiedToClipboardEvent Triggered if the seed is copied to clipboard
 * @param seed The seed that show in the UI as 13 unique codes
 * @param twoFAPin Typed 2FA pin (0..6 digits) provided by the authentication App from the user.
 * @param userEmail The current user email required to generate the 2FA url to be copied to clipboard
 * @param isPinSubmitted UI state to determine if the pins got submitted or not
 * @param authenticationState UI state for enabling the two factor authentication
 * @param isMasterKeyExported UI state to change the visibility of some related views in the activity
 * @param twoFactorAuthUrl The 2FA url that gets parsed to the available authentication App
 */
data class TwoFactorAuthenticationUIState(
    val is2FAFetchCompleted: Boolean = false,
    val isRkExportedSuccessfullyEvent: StateEventWithContent<Boolean> = consumed(),
    val writePermissionDeniedEvent: StateEvent = consumed,
    val seedCopiedToClipboardEvent: StateEvent = consumed,
    val seed: String? = null,
    val twoFAPin: String = "",
    val userEmail: String? = null,
    val isPinSubmitted: Boolean = false,
    val authenticationState: AuthenticationState? = AuthenticationState.Fixed,
    val isMasterKeyExported: Boolean = false,
    val twoFactorAuthUrl: String = "",
)




