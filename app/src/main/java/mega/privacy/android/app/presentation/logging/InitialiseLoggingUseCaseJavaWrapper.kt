package mega.privacy.android.app.presentation.logging

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.InitialiseLogging

/**
 * Initialise logging use case java wrapper
 * Temporary wrapper for the initialise logging use case to allow it to be called from the application class.
 * To be removed once Application class is converted to Kotlin
 *
 * @property useCase
 */
class InitialiseLoggingUseCaseJavaWrapper(private val useCase: InitialiseLogging) {
    /**
     * Invoke use case
     *
     * @param isDebug
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun invokeUseCase(isDebug: Boolean) {
        GlobalScope.launch { useCase(isDebug) }
    }
}