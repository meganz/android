package mega.privacy.android.domain.usecase

/**
 * Initialise MegaChat
 */
fun interface InitialiseMegaChat {

    /**
     * Invoke method
     *
     * @param session Required account session.
     */
    suspend operator fun invoke(session: String)
}