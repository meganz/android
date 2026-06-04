package mega.privacy.android.data.featuretoggle.persisted

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted feature flag value provider
 *
 * @property persistentFeatureFlagMemoryCache
 */
@Singleton
internal class PersistedFeatureFlagValueProvider @Inject constructor(
    private val persistentFeatureFlagMemoryCache: PersistentFeatureFlagMemoryCache,
) : FeatureFlagValueProvider {

    override val priority = FeatureFlagValuePriority.Cached

    override suspend fun isEnabled(feature: Feature) =
        persistentFeatureFlagMemoryCache.enabled(feature)

}
