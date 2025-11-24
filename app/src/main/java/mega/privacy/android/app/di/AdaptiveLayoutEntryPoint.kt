package mega.privacy.android.app.di

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.usecase.orientation.GetCachedAdaptiveLayoutUseCase
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import java.util.concurrent.atomic.AtomicReference

/**
 * Extension property to check if adaptive layout is enabled.
 *
 * This provides access to the adaptive layout feature flag before Hilt injection
 * is complete in activity lifecycle methods. It uses a cached EntryPoint to avoid
 * repeated expensive lookups.
 *
 * @return true if adaptive layout is enabled, false otherwise
 */
val Context.isAdaptiveLayoutEnabled: Boolean
    get() = AdaptiveLayoutFeatureFlagProvider.get(this)

/**
 * Suspended function to fetch the most recent adaptive layout feature flag value.
 *
 * Unlike [isAdaptiveLayoutEnabled], this function bypasses the cached value and retrieves
 * the latest feature flag state from the SDK or underlying data source.
 *
 * @return true if adaptive layout is enabled, false otherwise
 */
suspend fun Context.fetchIsAdaptiveLayoutEnabled(): Boolean =
    AdaptiveLayoutFeatureFlagProvider.fetch(this)

/**
 * Hilt EntryPoint for accessing [GetCachedAdaptiveLayoutUseCase] from non-Hilt contexts.
 *
 * This EntryPoint allows classes that are not Hilt components to access the
 * adaptive layout use case through the application context. It's particularly
 * useful for accessing dependencies before Hilt injection is complete in
 * activity lifecycle methods.
 *
 * @property getCachedAdaptiveLayoutUseCase The [GetCachedAdaptiveLayoutUseCase] instance
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdaptiveLayoutEntryPoint {
    val getCachedAdaptiveLayoutUseCase: GetCachedAdaptiveLayoutUseCase

    val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase
}

/**
 * Internal provider for cached adaptive layout feature flag state.
 *
 * This provider uses an AtomicReference to cache the adaptive layout state,
 * avoiding repeated expensive EntryPoint lookups. The cache is populated
 * on first access and reused for subsequent calls.
 */
internal object AdaptiveLayoutFeatureFlagProvider {
    private val featureFlagRef = AtomicReference<Boolean?>(null)

    /**
     * Gets the cached adaptive layout state, populating the cache if needed.
     *
     * @param context The application context for accessing the EntryPoint
     * @return true if adaptive layout is enabled, false otherwise
     */
    fun get(context: Context): Boolean {
        return featureFlagRef.get() ?: run {
            val isEnabled = EntryPointAccessors.fromApplication(
                context.applicationContext,
                AdaptiveLayoutEntryPoint::class.java
            ).getCachedAdaptiveLayoutUseCase()
            featureFlagRef.set(isEnabled)
            isEnabled
        }
    }

    /**
     * Get the most recent feature flag value from SDK.
     * @param context The application context for accessing the EntryPoint
     * @return true if adaptive layout is enabled, false otherwise
     */
    suspend fun fetch(context: Context): Boolean = EntryPointAccessors.fromApplication(
        context.applicationContext,
        AdaptiveLayoutEntryPoint::class.java
    ).getFeatureFlagValueUseCase(ApiFeatures.Android16OrientationMigrationEnabled)
}
