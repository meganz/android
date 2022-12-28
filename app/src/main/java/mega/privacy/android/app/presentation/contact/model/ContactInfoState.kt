package mega.privacy.android.app.presentation.contact.model

import mega.privacy.android.domain.entity.user.UserId

/**
 * Contact info UI state
 *
 * @property error                  String resource id for showing an error.
 * @property isCallStarted          Handle when a call is started.
 * @property userId                 Contact's handle.
 * @property email                  Contact's email.
 * @property areCredentialsVerified True if user credentials are verified, false otherwise.
 */
data class ContactInfoState(
    val error: Int? = null,
    val isCallStarted: Boolean? = false,
    val userId: UserId? = null,
    val email: String? = null,
    val areCredentialsVerified: Boolean = false,
)