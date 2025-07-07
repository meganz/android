package mega.privacy.android.app.appstate.initialisation.initialisers

/**
 * App start initialiser that executes a given action and handles any exceptions that may occur.
 * These are injected via multi injection into the auth viewmodel and executed on initialisation.
 * They will not block the login process from proceeding.
 *
 * @property action The suspend function to be executed during pre-login initialisation. It takes an optional existing session string as a parameter.
 */
class AppStartInitialiser(private val action: suspend () -> Unit) {
    suspend operator fun invoke() {
        action()
    }
}