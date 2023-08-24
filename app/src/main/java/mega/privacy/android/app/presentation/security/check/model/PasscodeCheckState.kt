package mega.privacy.android.app.presentation.security.check.model

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