package mega.privacy.android.domain.repository

/**
 * Links repository
 */
interface LinksRepository {

    /**
     * Decrypt link with password
     *
     * @param passwordProtectedLink
     * @param password
     * @return decrypted link to the file or folder
     */
    suspend fun decryptPasswordProtectedLink(
        passwordProtectedLink: String,
        password: String,
    ): String?
}

