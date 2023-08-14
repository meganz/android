package mega.privacy.android.app.presentation.passcode.model


/**
 * Passcode unlock state
 */
sealed interface PasscodeUnlockState {

    /**
     * Loading
     */
    object Loading : PasscodeUnlockState

    /**
     * Data
     *
     * @property passcodeType
     * @property failedAttempts
     * @property logoutWarning
     */
    data class Data(
        val passcodeType: PasscodeUIType,
        val failedAttempts: Int,
        val logoutWarning: Boolean,
    ) : PasscodeUnlockState
}
