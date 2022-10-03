package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.CheckAppUpdate

/**
 * Check app upgrade initializer
 *
 */
class CheckAppUpgradeInitializer : Initializer<Unit> {
    /**
     * Check app upgrade initializer entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CheckAppUpgradeInitializerEntryPoint {
        /**
         * App scope
         *
         */
        @ApplicationScope
        fun appScope(): CoroutineScope

        /**
         * Check app update
         *
         */
        fun checkAppUpdate(): CheckAppUpdate
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context,
                CheckAppUpgradeInitializerEntryPoint::class.java)
        entryPoint.appScope().launch {
            entryPoint.checkAppUpdate().invoke()
        }
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(
        LoggerInitializer::class.java
    )
}