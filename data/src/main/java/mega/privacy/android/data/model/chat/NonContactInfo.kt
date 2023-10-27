package mega.privacy.android.data.model.chat

/**
 * Data class to store information about a non-contact.
 *
 * @property handle    Non-contact handle.
 * @property fullName  Non-contact full name.
 * @property firstName Non-contact first name.
 * @property lastName  Non-contact last name.
 * @property email     Non-contact email.
 */
data class NonContactInfo(
    val handle: String,
    val fullName: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
) {
    /**
     * Short name of the non-contact.
     */
    val shortName: String?
        get() = firstName?.takeIf { it.isNotBlank() }
            ?: lastName?.takeIf { it.isNotBlank() }
            ?: email
}