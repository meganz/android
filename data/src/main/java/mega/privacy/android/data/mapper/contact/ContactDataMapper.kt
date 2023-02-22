package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import nz.mega.sdk.MegaUser

/**
 * Mapper to convert data to [ContactData]
 */
fun interface ContactDataMapper {
    /**
     * Invoke
     *
     * @param fullName nullable [String]
     * @param alias nullable [String]
     * @param avatarUri nullable [String]
     *
     * @return [ContactData]
     */
    operator fun invoke(
        fullName: String?,
        alias: String?,
        avatarUri: String?,
    ): ContactData
}