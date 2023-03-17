package mega.privacy.android.data.mapper.contact

import mega.privacy.android.domain.entity.contacts.AccountCredentials

/**
 * Mapper to convert data to [AccountCredentials.MyAccountCredentials]
 */
internal fun interface MyAccountCredentialsMapper {
    /**
     * Invoke
     *
     * @param credentials String containing account credentials.
     * @return [AccountCredentials.MyAccountCredentials]
     */
    operator fun invoke(credentials: String?): AccountCredentials.MyAccountCredentials?
}