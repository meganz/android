package mega.privacy.android.app.presentation.passcode.model

/**
 * Passcode unlock state
 *
 * @property failedAttempts
 * @property logoutWarning
 */
data class PasscodeUnlockState(
    val failedAttempts: Int,
    val logoutWarning: Boolean,
)
