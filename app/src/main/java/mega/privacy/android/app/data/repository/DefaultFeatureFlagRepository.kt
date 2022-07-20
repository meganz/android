package mega.privacy.android.app.data.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.data.mapper.FeatureFlagMapper
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.domain.entity.FeatureFlag
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @FeatureFlagRepository
 * @property preferencesGateway
 * @property featureFlagMapper
 * @property ioDispatcher
 */
class DefaultFeatureFlagRepository @Inject constructor(
    private val preferencesGateway: FeatureFlagPreferencesGateway,
    private val featureFlagMapper: FeatureFlagMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FeatureFlagRepository {

    /**
     * Sets value of feature flag
     *
     * @param featureName: Name of the feature
     * @param isEnabled: Boolean value
     */
    override suspend fun setFeature(featureName: String, isEnabled: Boolean) =
        withContext(ioDispatcher) {
            preferencesGateway.setFeature(featureName, isEnabled)
        }


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
                    featureFlagMapper(it.key, it.value as Boolean)
                }
            }
        }
}