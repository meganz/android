package mega.privacy.android.shared.contact.extension

import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Resolves the name to display for a contact: the alias if set, otherwise the full name,
 * otherwise the email. Blank alias or full name values are treated as absent.
 */
fun ContactItem.displayName(): String =
    contactData.alias?.takeIf { it.isNotBlank() }
        ?: contactData.fullName?.takeIf { it.isNotBlank() }
        ?: email
