package mega.privacy.android.app.presentation.login.createaccount.model

import mega.privacy.android.domain.entity.login.EphemeralCredentials

/**
 * Status of creating an account
 */
sealed class CreateAccountStatus {
    /**
     * Success
     * @property credentials Ephemeral credentials
     */
    class Success(val credentials: EphemeralCredentials) : CreateAccountStatus()

    /**
     * Account already exists
     */
    data object AccountAlreadyExists : CreateAccountStatus()

    /**
     *  Unknown error
     *  @property message Error message
     */
    class UnknownError(val message: String) : CreateAccountStatus()
}