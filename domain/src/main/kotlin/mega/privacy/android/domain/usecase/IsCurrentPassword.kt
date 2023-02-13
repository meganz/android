package mega.privacy.android.domain.usecase

/**
 * Check is password the current password
 */
fun interface IsCurrentPassword {
    /**
     * Invoke and returns true if password is the same as current password, else false
     */
    suspend operator fun invoke(password: String): Boolean
}