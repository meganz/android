package mega.privacy.android.app.presentation.login.confirmemail.model

import mega.privacy.android.app.presentation.login.model.LoginFragmentType

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property isLoading is loading
 * @property isPendingToShowFragment [LoginFragmentType] if pending, null otherwise.
 * @property registeredEmail The new registered email.
 * @property firstName The name of the user.
 * @property isOnline Is connected to the network.
 * @property shouldShowSuccessMessage True if we should show a success message, false otherwise.
 * @property message The message that should be displayed to the user.
 * @property isNewRegistrationUiEnabled True if the new registration UI is enabled, false otherwise.
 * @property isCreatingAccountCancelled True if the account creation was cancelled, false otherwise.
 * @property isAccountConfirmed True if the account has been confirmed, false otherwise.
 */
data class ConfirmEmailUiState(
    val isLoading: Boolean = false,
    val isPendingToShowFragment: LoginFragmentType? = null,
    val registeredEmail: String? = null,
    val firstName: String? = null,
    val isOnline: Boolean = false,
    val shouldShowSuccessMessage: Boolean = false,
    val message: String? = null,
    val isNewRegistrationUiEnabled: Boolean? = null,
    val isCreatingAccountCancelled: Boolean = false,
    val isAccountConfirmed: Boolean = false,
)
