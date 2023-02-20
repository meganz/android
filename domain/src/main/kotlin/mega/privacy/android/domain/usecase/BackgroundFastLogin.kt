package mega.privacy.android.domain.usecase

/**
 * Background fast login use case.
 * This fast login does not require to show the login screen.
 * A complete fast login process includes three different requests in this order:
 *      1.- initMegaChat
 *      2.- fastLogin
 *      3.- fetchNodes
 * Until all of them have been completed, a new login will not be possible.
 * If this is broken at some point, then the app can suffer unexpected behaviors like
 * logout and lose the current user's session.
 */
interface BackgroundFastLogin {

    /**
     * Invoke.
     *
     */
    suspend operator fun invoke(): String
}