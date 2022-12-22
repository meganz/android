package mega.privacy.android.domain.entity.contacts

/**
 * Entity sealed class for storing data related to credentials.
 * We refer to credentials as the fingerprint of the signing key of an user's account.
 *
 * @property credentials The fingerprint of the signing key of an user's account.
 */
sealed class AccountCredentials {

    abstract val credentials: List<String>

    /**
     * [AccountCredentials] of the current logged in account.
     */
    data class MyAccountCredentials(override val credentials: List<String>) : AccountCredentials()

    /**
     * [AccountCredentials] of a contact of the current logged in account.
     *
     * @property email Contact's email.
     * @property name  Contact's name.
     */
    data class ContactCredentials(
        override val credentials: List<String>,
        val email: String,
        val name: String,
    ) : AccountCredentials()
}
