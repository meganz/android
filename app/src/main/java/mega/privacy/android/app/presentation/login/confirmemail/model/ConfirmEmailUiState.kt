package mega.privacy.android.app.presentation.login.confirmemail.model

import mega.privacy.android.app.presentation.login.model.LoginFragmentType

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property isPendingToShowFragment [LoginFragmentType] if pending, null otherwise.
 * @property registeredEmail The new registered email.
 * @property isOnline Is connected to the network.
 * @property shouldShowSuccessMessage True if we should show a success message, false otherwise.
 * @property errorMessage The error message that should be displayed to the user.
 */
data class ConfirmEmailUiState(
    val isPendingToShowFragment: LoginFragmentType? = null,
    val registeredEmail: String? = null,
    val isOnline: Boolean = false,
    val shouldShowSuccessMessage: Boolean = false,
    val errorMessage: String? = null
)
