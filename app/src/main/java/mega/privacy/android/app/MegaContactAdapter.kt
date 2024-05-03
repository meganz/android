package mega.privacy.android.app

import mega.privacy.android.domain.entity.Contact
import nz.mega.sdk.MegaUser

/**
 * A data model represents the Mega Contact.
 *
 * @property contact [Contact]
 * @property megaUser [MegaUser]
 * @property fullName The full name of the contact
 * @property lastGreen Represents the time when the user last online
 * @property isSelected True if the contact is selected, false otherwise
 */
data class MegaContactAdapter @JvmOverloads constructor(
    val contact: Contact?,
    val megaUser: MegaUser?,
    val fullName: String?,
    var lastGreen: String = "",
    var isSelected: Boolean = false,
)
