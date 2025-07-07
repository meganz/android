package mega.privacy.android.app.appstate.initialisation.initialisers

/**
 * Pre-login initialiser that executes a given action and handles any exceptions that may occur.
 * These are injected via multi injection into the auth viewmodel and executed after existing session check.
 * They will not block the login process from proceeding.
 *
 * @property action The suspend function to be executed during pre-login initialisation. It takes an optional existing session string as a parameter.
 */
class PreLoginInitialiser(private val action: suspend (String?) -> Unit) {
    suspend operator fun invoke(existingSession: String?) {
        action(existingSession)
    }
}