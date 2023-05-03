package mega.privacy.android.app.presentation.login.model

/**
 * Enum class for defining the possible states of 2FA in login.
 */
enum class MultiFactorAuthState {
    /**
     * Failed state.
     */
    Failed,

    /**
     * Checking state.
     */
    Checking,

    /**
     * Fixed state.
     */
    Fixed
}