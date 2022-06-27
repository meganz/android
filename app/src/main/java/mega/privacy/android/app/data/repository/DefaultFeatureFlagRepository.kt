package mega.privacy.android.app.data.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @FeatureFlagRepository
 * @param preferencesGateway: Data Store Preferences gateway
 */
class DefaultFeatureFlagRepository @Inject constructor(
    private val preferencesGateway: FeatureFlagPreferencesGateway,
) : FeatureFlagRepository {

    /**
     * Sets value of feature flag
     *
     * @param featureName: Name of the feature
     * @param isEnabled: Boolean value
     */
    override suspend fun setFeature(featureName: String, isEnabled: Boolean) =
        preferencesGateway.setFeature(featureName, isEnabled)

    /**
     * Gets a map of Preferences from gateway and returns flow
     *
     * @return: Flow of List of @FeatureFlag
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllFeatures(): Flow<List<FeatureFlag>> =
        preferencesGateway.getAllFeatures().mapLatest { map ->
            map.asMap().mapNotNull { entry: Map.Entry<Preferences.Key<*>, Any> ->
                entry.takeIf { it.value is Boolean }?.let {
                    toFeatureFlag(it.key, it.value as Boolean)
                }
            }
        }
}

/**
 * Type alias to make map output readable
 */
typealias FeatureFlagMapper = (
    @JvmSuppressWildcards Preferences.Key<*>,
    @JvmSuppressWildcards Boolean,
) ->
@JvmSuppressWildcards FeatureFlag

internal fun toFeatureFlag(
    key: Preferences.Key<*>,
    value: Boolean,
) = FeatureFlag(key.toString(), value)