package mega.privacy.android.domain.usecase

/**
 * Change user's password
 */
fun interface ChangePassword {

    /**
     * Invoke
     * @param newPassword as user's new password
     * @return true if change password is successful, else false
     */
    suspend operator fun invoke(newPassword: String): Boolean
}