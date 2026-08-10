package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.featureflag.FetchAndActivateRemoteConfigUseCase
import timber.log.Timber

/**
 * Fetches and activates Firebase Remote Config values at app startup so that
 * A/B test experiment values are available before the first screen renders
 */
@Suppress("EnsureInitializerMetadata")
class RemoteConfigInitializer : Initializer<Unit> {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RemoteConfigInitializerEntryPoint {

        @ApplicationScope
        fun appScope(): CoroutineScope

        fun fetchAndActivateRemoteConfigUseCase(): FetchAndActivateRemoteConfigUseCase
    }

    override fun create(context: Context) {
        if (!context.canResolveHiltEntryPoints()) return
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            RemoteConfigInitializerEntryPoint::class.java
        )

        entryPoint.appScope().launch {
            runCatching {
                entryPoint.fetchAndActivateRemoteConfigUseCase()(
                    useMinimalFetchInterval = BuildConfig.DEBUG
                )
            }.onFailure {
                Timber.w(it, "Fetching Remote Config values failed")
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java)
}
