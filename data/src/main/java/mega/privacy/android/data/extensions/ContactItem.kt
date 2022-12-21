package mega.privacy.android.data.extensions

import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Sorts the [ContactItem] list by alias if exists, if not by full name if exists, else by email.
 *
 * @return The ordered list.
 */
fun List<ContactItem>.sortList(): List<ContactItem> =
    sortedBy { (_, email, contactData) ->
        contactData.alias?.lowercase() ?: contactData.fullName?.lowercase() ?: email.lowercase()
    }

/**
 * Searches a [ContactItem] with the received handle.
 *
 * @param userHandle User handle.
 * @return [ContactItem] if any.
 */
fun List<ContactItem>.findItemByHandle(userHandle: Long): ContactItem? =
    find { (handle) -> handle == userHandle }

/**
 * Replaces an existing [ContactItem] if exists.
 *
 * @param contactItem The [ContactItem] to replace.
 */
fun MutableList<ContactItem>.replaceIfExists(contactItem: ContactItem) {
    removeAll { (handle) -> handle == contactItem.handle }
    add(contactItem)
}