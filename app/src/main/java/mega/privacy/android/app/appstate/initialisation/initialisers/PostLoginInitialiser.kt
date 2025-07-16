package mega.privacy.android.app.appstate.initialisation.initialisers

/**
 * Post login initialiser that executes a given action and handles any exceptions that may occur.
 * These are injected via multi injection into the auth viewmodel and executed after login.
 * They will not block the post login process from proceeding.
 *
 * @property action The suspend function to be executed during pre-login initialisation. It takes the non nullable session string as a parameter.
 */
open class PostLoginInitialiser(private val action: suspend (String) -> Unit) {
    suspend operator fun invoke(session: String) {
        action(session)
    }
}