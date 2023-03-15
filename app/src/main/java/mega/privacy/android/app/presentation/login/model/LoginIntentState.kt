package mega.privacy.android.app.presentation.login.model

/**
 * Enum class for defining the login intent state.
 */
enum class LoginIntentState {

    /**
     * The initial setup can be done.
     */
    ReadyForInitialSetup,

    /**
     * Intent already set.
     */
    AlreadySet,

    /**
     * The final setup can be done.
     */
    ReadyForFinalSetup
}