package mega.privacy.android.core.passcode.check.model

/**
 * Passcode check state
 */
sealed interface PasscodeCheckState {

    /**
     * Loading
     */
    object Loading : PasscodeCheckState

    /**
     * UnLocked
     */
    object UnLocked : PasscodeCheckState

    /**
     * Locked
     */
    object Locked : PasscodeCheckState
}
