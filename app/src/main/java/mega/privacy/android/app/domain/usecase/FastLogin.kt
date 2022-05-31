package mega.privacy.android.app.domain.usecase

/**
 * Fast login use case.
 */
interface FastLogin {

    /**
     * Invoke.
     *
     * @param session Required account session for login.
     */
    suspend operator fun invoke(session: String)
}