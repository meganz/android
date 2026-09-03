package mega.privacy.android.app.appstate.global.initialisation.appcreate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.InitialiseLoggingUseCase
import mega.privacy.android.navigation.contract.initialisation.SynchronousAppCreateInitialiser
import javax.inject.Inject

/**
 * Kicks off the file logging subsystem initialisation.
 *
 * The SDK console loggers are attached by [SdkBootInitialiser] once the native library is
 * loaded, before any SDK instance is created. The file logging initialisation itself remains
 * fire-and-forget in the application scope.
 */
internal class LoggingInitialiser @Inject constructor(
    private val initialiseLoggingUseCase: InitialiseLoggingUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
) : SynchronousAppCreateInitialiser {
    override val name = "LoggingInitialiser"

    override operator fun invoke() {
        appScope.launch {
            initialiseLoggingUseCase()
        }
    }
}
