package mega.privacy.android.app.presentation.contact.authenticitycredendials.model

import mega.privacy.android.domain.entity.contacts.AccountCredentials

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsViewModel]
 *
 * @property contactCredentials                         [AccountCredentials.ContactCredentials].
 * @property areCredentialsVerified                     True if credentials are verified, false otherwise.
 * @property isVerifyingCredentials                     True if is already verifying credentials, false otherwise.
 * @property myAccountCredentials                       [AccountCredentials.MyAccountCredentials].
 * @property error                                      String resource id for showing an error.
 */
data class AuthenticityCredentialsState(
    val contactCredentials: AccountCredentials.ContactCredentials? = null,
    val areCredentialsVerified: Boolean = false,
    val isVerifyingCredentials: Boolean = false,
    val myAccountCredentials: AccountCredentials.MyAccountCredentials? = null,
    val error: Int? = null,
)