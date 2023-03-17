package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.extensions.getCredentials
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.entity.contacts.ContactData
import javax.inject.Inject

/**
 * [ContactCredentialsMapper] implementation
 */
class ContactCredentialsMapperImpl @Inject constructor() : ContactCredentialsMapper {

    override fun invoke(
        credentials: String?,
        email: String,
        name: String,
    ) = credentials?.getCredentials()?.let {
        AccountCredentials.ContactCredentials(
            credentials = it,
            email = email,
            name = name
        )
    }

}