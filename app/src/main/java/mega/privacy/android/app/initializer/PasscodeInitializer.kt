package mega.privacy.android.app.initializer

import android.content.Context
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.security.PasscodeLifeCycleObserver
import mega.privacy.android.app.presentation.security.PasscodeLifecycleDispatcher
import mega.privacy.android.app.presentation.security.PasscodeProcessLifecycleOwner
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase

/**
 * Passcode initializer
 */
class PasscodeInitializer : Initializer<Unit> {

    /**
     * Passcode initializer entrypoint
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PasscodeInitializerEntrypoint {

        /**
         * Get feature flag value
         *
         * @return feature flag value use case
         */
        fun getFeatureFlagValue(): GetFeatureFlagValueUseCase

        /**
         * Get passcode lifecycle observer
         *
         * @return passcode lifecycle observer
         */
        fun passcodeLifecycleObserver(): PasscodeLifeCycleObserver

        /**
         * App scope
         *
         * @return application scope
         */
        @ApplicationScope
        fun appScope(): CoroutineScope

        /**
         * Main dispatcher
         *
         * @return main thread dispatcher
         */
        @MainDispatcher
        fun mainDispatcher(): CoroutineDispatcher
    }

    /**
     * Create
     */
    override fun create(context: Context) {
        PasscodeLifecycleDispatcher.init(context)
        PasscodeProcessLifecycleOwner.init(context)
        val entryPoint =
            EntryPointAccessors.fromApplication(context, PasscodeInitializerEntrypoint::class.java)
        with(entryPoint) {
            appScope().launch {
                if (getFeatureFlagValue()(AppFeatures.Passcode)) {
                    withContext(mainDispatcher()) {
                        PasscodeProcessLifecycleOwner.get().observer = passcodeLifecycleObserver()
                    }
                }
            }
        }
    }

    /**
     * Dependencies
     *
     * Api needs to be initialised as it is a dependency the database handler which is required
     * for the passcode data store migration
     */
    override fun dependencies(): MutableList<Class<out Initializer<*>>> =
        mutableListOf(
            ProcessLifecycleInitializer::class.java,
            LoggerInitializer::class.java,
            SetupMegaApiInitializer::class.java
        )
}