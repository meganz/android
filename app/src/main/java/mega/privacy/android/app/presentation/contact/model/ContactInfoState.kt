package mega.privacy.android.app.presentation.contact.model

/**
 * Contact info UI state
 *
 * @property error             String resource id for showing an error.
 * @property isCallStarted     Handle when a call is started.
 */
data class ContactInfoState(
    val error: Int? = null,
    val isCallStarted: Boolean? = false,
)