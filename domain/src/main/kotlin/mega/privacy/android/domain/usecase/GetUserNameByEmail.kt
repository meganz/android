package mega.privacy.android.domain.usecase

/**
 * The use case for getting user name by email
 */
fun interface GetUserNameByEmail {

    /**
     * Get user name by email
     *
     * @param email email
     * @return user name
     */
    suspend operator fun invoke(email: String): String?
}