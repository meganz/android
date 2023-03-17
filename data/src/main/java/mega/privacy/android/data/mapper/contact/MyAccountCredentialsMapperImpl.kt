package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.extensions.getCredentials
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import javax.inject.Inject

/**
 * [MyAccountCredentialsMapper] implementation
 */
class MyAccountCredentialsMapperImpl @Inject constructor() : MyAccountCredentialsMapper {

    override fun invoke(credentials: String?) = (credentials?.getCredentials()?.let {
        AccountCredentials.MyAccountCredentials(it)
    })
}