package mega.privacy.android.app.presentation.login.confirmemail.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.domain.entity.ThemeMode

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.login.LoginFragment].
 *
 * @property isLoading is loading
 * @property isPendingToShowScreen event effect with content of type [LoginScreen] if pending, consumed otherwise.
 * @property registeredEmail The new registered email.
 * @property firstName The name of the user.
 * @property shouldShowSuccessMessage True if we should show a success message, false otherwise.
 * @property message The message that should be displayed to the user.
 * @property isCreatingAccountCancelled True if the account creation was cancelled, false otherwise.
 * @property isAccountConfirmed True if the account has been confirmed, false otherwise.
 * @property themeMode The current theme mode.
 * @property resendSignUpLinkError [ResendSignUpLinkError].
 * @property resendCountdownSeconds Remaining seconds before the user can resend the confirmation
 * email again. Reflects the API's 1-minute cooldown between resend attempts.
 */
data class ConfirmEmailUiState(
    val isLoading: Boolean = false,
    val isPendingToShowScreen: LoginScreen? = null,
    val registeredEmail: String? = null,
    val firstName: String? = null,
    val shouldShowSuccessMessage: Boolean = false,
    val message: String? = null,
    val isCreatingAccountCancelled: Boolean = false,
    val isAccountConfirmed: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val resendSignUpLinkError: StateEventWithContent<ResendSignUpLinkError> = consumed(),
    val resendCountdownSeconds: Int = RESEND_EMAIL_COUNTDOWN_SECONDS,
) {
    /**
     * Whether the user is allowed to resend the confirmation email, i.e. the cooldown elapsed.
     */
    val canResend: Boolean get() = resendCountdownSeconds <= 0
}

/**
 * Duration in seconds of the resend cooldown enforced by the API between resend attempts.
 */
const val RESEND_EMAIL_COUNTDOWN_SECONDS = 60
