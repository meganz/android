package mega.privacy.android.app.presentation.account.model

/**
 * Events related to account switching
 */
sealed interface QAAccountSwitchEvent {

    /**
     * Account switch completed successfully
     * @property email The email of the account that was switched to
     */
    data class Success(val email: String?) : QAAccountSwitchEvent

    /**
     * Account switch failed
     * @property error The error that occurred during account switch
     */
    data class Failure(val error: Throwable) : QAAccountSwitchEvent
}
