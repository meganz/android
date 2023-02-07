package mega.privacy.android.domain.usecase

/**
 * Use case for querying signup links.
 */
fun interface QuerySignupLink {

    /**
     * Invoke.
     *
     * @param link Link to query.
     * @return The email related to the link.
     */
    suspend operator fun invoke(link: String): String
}