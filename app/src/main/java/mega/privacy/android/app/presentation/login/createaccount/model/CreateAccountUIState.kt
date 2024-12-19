package mega.privacy.android.app.presentation.login.createaccount.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

/**
 * UI State for [CreateAccountScreen]
 */
data class CreateAccountUIState(
    /**
     * Account creation in progress
     */
    val isAccountCreationInProgress: Boolean = false,
    /**
     * The status after a user signs up
     */
    val createAccountStatusEvent: StateEventWithContent<CreateAccountStatus> = consumed(),
    /**
     * Input state of first name
     */
    val isFirstNameValid: Boolean? = null,
    /**
     * Input state of last name
     */
    val isLastNameValid: Boolean? = null,
    /**
     * Input state of email
     */
    val isEmailValid: Boolean? = null,

    /**
     * Input state of password
     */
    val isPasswordValid: Boolean? = null,

    /**
     * Check if password matches confirm password
     */
    val isConfirmPasswordMatched: Boolean? = null,
    /**
     * Password strength based on MEGA SDK checks
     */
    val passwordStrength: PasswordStrength = PasswordStrength.INVALID,

    /**
     * Input state of terms of service if it is checked
     */
    val isTermsOfServiceAgreed: Boolean? = null,
    /**
     * Input state of End to End Encryption if it is checked
     */
    val isE2EEAgreed: Boolean? = null,

    /**
     * Whether to inform user to agree to terms
     */
    val showAgreeToTermsEvent: StateEvent = consumed,

    /**
     * Whether connected to Internet
     */
    val isConnected: Boolean = false,

    /**
     * Show No network warning
     */
    val showNoNetworkWarning: Boolean = false,
)