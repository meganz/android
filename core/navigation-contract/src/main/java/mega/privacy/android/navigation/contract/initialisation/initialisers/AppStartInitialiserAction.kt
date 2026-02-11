package mega.privacy.android.navigation.contract.initialisation.initialisers

/**
 * App start initialiser that executes a given action and handles any exceptions that may occur.
 * These are injected via multi injection into the auth viewmodel and executed on initialisation.
 * They will not block the login process from proceeding.
 *
 * @property action The suspend function to be executed during app start initialisation.
 */
open class AppStartInitialiserAction(private val action: suspend () -> Unit): AppStartInitialiser {
    override suspend operator fun invoke() {
        action()
    }
}

interface AppStartInitialiser {
    suspend operator fun invoke()
}
