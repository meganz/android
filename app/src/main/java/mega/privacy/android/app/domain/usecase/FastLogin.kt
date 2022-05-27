package mega.privacy.android.app.domain.usecase

/**
 * Fast login use case.
 */
interface FastLogin {

    suspend operator fun invoke(session: String)
}