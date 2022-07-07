package mega.privacy.android.domain.usecase

/**
 * Fast login use case.
 */
fun interface FastLogin {

    /**
     * Invoke.
     *
     * @param session Required account session for login.
     */
    suspend operator fun invoke(session: String)
}