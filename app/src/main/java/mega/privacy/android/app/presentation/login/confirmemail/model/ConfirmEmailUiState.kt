package mega.privacy.android.app.presentation.login.confirmemail.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.domain.entity.ThemeMode

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property isLoading is loading
 * @property isPendingToShowFragment [LoginFragmentType] if pending, null otherwise.
 * @property registeredEmail The new registered email.
 * @property firstName The name of the user.
 * @property shouldShowSuccessMessage True if we should show a success message, false otherwise.
 * @property message The message that should be displayed to the user.
 * @property isCreatingAccountCancelled True if the account creation was cancelled, false otherwise.
 * @property isAccountConfirmed True if the account has been confirmed, false otherwise.
 * @property themeMode The current theme mode.
 * @property accountExistEvent Event triggered when account already exists.
 * @property generalErrorEvent Event triggered when a general error occurs.
 */
data class ConfirmEmailUiState(
    val isLoading: Boolean = false,
    val isPendingToShowFragment: LoginFragmentType? = null,
    val registeredEmail: String? = null,
    val firstName: String? = null,
    val shouldShowSuccessMessage: Boolean = false,
    val message: String? = null,
    val isCreatingAccountCancelled: Boolean = false,
    val isAccountConfirmed: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val accountExistEvent: StateEvent = consumed,
    val generalErrorEvent: StateEvent = consumed
)
