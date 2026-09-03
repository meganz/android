package mega.privacy.android.app.appstate.global.initialisation.appcreate

import dagger.Lazy
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.NativeLibraryLoader
import mega.privacy.android.domain.usecase.EnableLogAllToConsole
import mega.privacy.android.navigation.contract.initialisation.CriticalAsyncAppCreateInitialiser
import javax.inject.Inject

/**
 * Waits for the native library to load, then attaches the SDK console loggers.
 *
 * Must be first in the critical async list: the SDK-dependent initialisers that follow hold
 * lazy SDK dependencies, so no SDK instance is constructed (and no SDK thread runs) until this
 * initialiser has completed with the loggers in place.
 */
internal class SdkBootInitialiser @Inject constructor(
    private val enableLogAllToConsole: Lazy<EnableLogAllToConsole>,
) : CriticalAsyncAppCreateInitialiser {
    override val name = "SdkBootInitialiser"

    override suspend operator fun invoke() {
        NativeLibraryLoader.awaitLoadedSuspend()
        enableLogAllToConsole.get()(BuildConfig.DEBUG)
    }
}
