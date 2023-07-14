package mega.privacy.android.app.presentation.twofactorauthentication.model

import android.graphics.Bitmap
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.triggered


/**
 * UI State for Two Factor authentication activity
 * @param is2FAFetchCompleted UI state to check if fetching the 2FA code was completed or not
 * @param seed The seed that show in the UI as 13 unique codes
 * @param twoFAPin Typed 2FA pin provided by the authentication App from the user
 * @param isFirstTime2FA True if it is the first time the 2FA is requested.
 * @param userEmail The current user email required to generate the 2FA url to be copied to clipboard
 * @param isQRCodeGenerationCompleted UI state to check if generating the QR code process was completed or not
 * @param qrBitmap the qr code bitmap generated from the 2FA codes to display in the view
 * @param isPinSubmitted UI state to determine if the pins got submitted or not
 * @param authenticationState UI state for enabling the two factor authentication
 * @param isMasterKeyExported UI state to change the visibility of some related views in the activity
 * @param twoFactorAuthUrl The 2FA url that gets parsed to the available authentication App
 */
data class TwoFactorAuthenticationUIState(
    val is2FAFetchCompleted: Boolean = false,
    val seed: String? = null,
    val twoFAPin: List<String> = listOf("", "", "", "", "", ""),
    val isFirstTime2FA: StateEvent = triggered,
    val userEmail: String? = null,
    val isQRCodeGenerationCompleted: Boolean = false,
    val qrBitmap: Bitmap? = null,
    val isPinSubmitted: Boolean = false,
    val authenticationState: AuthenticationState? = AuthenticationState.Fixed,
    val isMasterKeyExported: Boolean = false,
    val twoFactorAuthUrl: String = "",
)




