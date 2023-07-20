package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.qualifier.FeatureFlagPriorityKey
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject


/**
 * Default feature flag repository
 *
 * @property ioDispatcher
 * @property featureFlagValueProvider
 */
internal class DefaultFeatureFlagRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val featureFlagValueProvider: Map<FeatureFlagPriorityKey, @JvmSuppressWildcards FeatureFlagValueProvider>,
) : FeatureFlagRepository {

    override suspend fun getFeatureValue(feature: Feature) =
        withContext(ioDispatcher) {
            featureFlagValueProvider.toSortedMap(
                compareByDescending<FeatureFlagPriorityKey> { it.priority }
                    .thenBy { it.implementingClass.simpleName }
            ).firstNotNullOfOrNull { it.value.isEnabled(feature) }
        }
}