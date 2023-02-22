package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import nz.mega.sdk.MegaUser

/**
 * Mapper for converting [MegaUser] into [ContactItem]
 */
fun interface ContactItemMapper {
    /**
     * Invoke
     *
     * @param megaUser [MegaUser]
     * @param contactData [ContactData]
     * @param defaultAvatarColor [String]
     * @param areCredentialsVerified [Boolean]
     * @param status [Int]
     * @param lastSeen nullable [Int]
     *
     * @return the [ContactItem] of [megaUser]
     */
    operator fun invoke(
        megaUser: MegaUser,
        contactData: ContactData,
        defaultAvatarColor: String,
        areCredentialsVerified: Boolean,
        status: Int,
        lastSeen: Int?,
    ): ContactItem
}