package mega.privacy.android.domain.usecase

/**
 * Resets user's password from link
 */
fun interface ResetPassword {
    /**
     * Invoke
     * @param link as reset link
     * @param newPassword as user's new password
     * @param masterKey as user's master key, can be null if account is parked
     * @return true if reset password is successful, else false
     */
    suspend operator fun invoke(
        link: String?,
        newPassword: String,
        masterKey: String?
    ): Boolean
}