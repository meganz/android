package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.ContactData
import javax.inject.Inject

/**
 * [ContactDataMapper] implementation
 */
class ContactDataMapperImpl @Inject constructor() : ContactDataMapper {
    override fun invoke(fullName: String?, alias: String?, avatarUri: String?) =
        ContactData(
            fullName = fullName,
            alias = alias,
            avatarUri = avatarUri,
        )
}