package mega.privacy.android.app.presentation.twofactorauthentication.model

/**
 * UI State for enabling the two factor authentication
 */
enum class AuthenticationState {
    /**
     * @property Passed UI state for successful authentication via valid pin codes
     * @property Failed UI state for failed authentication via invalid pin codes
     * @property Checking UI state for checking on the authentication state after submitting the pin
     * @property Fixed UI state for fixed pin code
     * @property Error UI state for failed authentication for unknown server error
     */
    Passed,
    Failed,
    Checking,
    Fixed,
    Error
}