package mega.privacy.android.app.appstate.global.initialisation.appcreate

import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.utils.greeter.Greeter
import mega.privacy.android.navigation.contract.initialisation.AsyncAppCreateInitialiser
import javax.inject.Inject
import javax.inject.Provider

/**
 * Initialises the [Greeter] debugging tool on builds where it is activated. The [Provider]
 * keeps the greeter from being constructed at all on other builds.
 */
class GreeterInitialiser @Inject constructor(
    private val greeter: Provider<Greeter>,
) : AsyncAppCreateInitialiser {
    override val name = "GreeterInitialiser"

    override suspend operator fun invoke() {
        if (BuildConfig.ACTIVATE_GREETER) greeter.get().initialize()
    }
}
