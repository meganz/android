package mega.privacy.android.domain.usecase.account

/**
 * Update my avatar with new email
 *
 */
fun interface UpdateMyAvatarWithNewEmail {
    /**
     * Invoke
     *
     * @param oldEmail
     * @param newEmail
     */
    suspend operator fun invoke(oldEmail: String, newEmail: String): Boolean
}