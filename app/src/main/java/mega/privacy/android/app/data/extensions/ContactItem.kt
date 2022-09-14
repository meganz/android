package mega.privacy.android.app.data.extensions

import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Sorts the [ContactItem] list by alias if exists, if not by full name if exists, else by email.
 *
 * @return The ordered list.
 */
fun List<ContactItem>.sortList(): List<ContactItem> =
    sortedBy { (_, email, fullName, alias) ->
        alias?.lowercase() ?: fullName?.lowercase() ?: email.lowercase()
    }