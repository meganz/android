package mega.privacy.android.app.domain.usecase

/**
 * Init megaChat API use case.
 */
interface InitMegaChat {

    /**
     * Invoke.
     *
     * @param session   Account session.
     */
    suspend operator fun invoke(session: String)
}