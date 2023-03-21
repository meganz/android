package mega.privacy.android.data.mapper.contact

import mega.privacy.android.data.extensions.getCredentials
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import javax.inject.Inject

/**
 * Mapper to convert data to [AccountCredentials.MyAccountCredentials]
 */
internal class MyAccountCredentialsMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param credentials String containing account credentials.
     * @return [AccountCredentials.MyAccountCredentials]
     */
    operator fun invoke(credentials: String?) = (credentials?.getCredentials()?.let {
        AccountCredentials.MyAccountCredentials(it)
    })
}