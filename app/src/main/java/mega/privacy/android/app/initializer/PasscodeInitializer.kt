package mega.privacy.android.app.initializer

import android.content.Context
import androidx.lifecycle.ProcessLifecycleInitializer
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.core.passcode.PasscodeLifeCycleObserver
import mega.privacy.android.core.passcode.PasscodeLifecycleDispatcher
import mega.privacy.android.core.passcode.PasscodeProcessLifecycleOwner

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
         * Get passcode lifecycle observer
         *
         * @return passcode lifecycle observer
         */
        fun passcodeLifecycleObserver(): PasscodeLifeCycleObserver
    }

    /**
     * Create
     */
    override fun create(context: Context) {
        if (!context.canResolveHiltEntryPoints()) return
        PasscodeLifecycleDispatcher.init(context)
        PasscodeProcessLifecycleOwner.init(context)
        val entryPoint =
            EntryPointAccessors.fromApplication(context, PasscodeInitializerEntrypoint::class.java)
        with(entryPoint) {
            PasscodeProcessLifecycleOwner.get().observer = passcodeLifecycleObserver()
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