package mega.privacy.android.app.presentation.login.confirmemail.model

import mega.privacy.android.app.presentation.login.model.LoginFragmentType

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property isLoading is loading
 * @property isPendingToShowFragment [LoginFragmentType] if pending, null otherwise.
 * @property registeredEmail The new registered email.
 * @property isOnline Is connected to the network.
 * @property shouldShowSuccessMessage True if we should show a success message, false otherwise.
 * @property errorMessage The error message that should be displayed to the user.
 * @property isNewRegistrationUiEnabled True if the new registration UI is enabled, false otherwise.
 */
data class ConfirmEmailUiState(
    val isLoading: Boolean = false,
    val isPendingToShowFragment: LoginFragmentType? = null,
    val registeredEmail: String? = null,
    val isOnline: Boolean = false,
    val shouldShowSuccessMessage: Boolean = false,
    val errorMessage: String? = null,
    val isNewRegistrationUiEnabled: Boolean? = null,
)
