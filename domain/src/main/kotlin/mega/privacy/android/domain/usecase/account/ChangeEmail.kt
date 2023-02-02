package mega.privacy.android.domain.usecase.account

/**
 * Change email
 *
 */
fun interface ChangeEmail {
    /**
     * Invoke
     *
     * @param email new email
     */
    suspend operator fun invoke(email: String): String
}