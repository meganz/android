package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject


/**
 * Default feature flag repository
 *
 * @property ioDispatcher
 * @property featureFlagValueProviderSet
 */
internal class DefaultFeatureFlagRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val featureFlagValueProviderSet: Set<@JvmSuppressWildcards FeatureFlagValueProvider>,
) : FeatureFlagRepository {

    override suspend fun getFeatureValue(feature: Feature) =
        withContext(ioDispatcher) {
            val sorted = featureFlagValueProviderSet.sortedWith(
                compareByDescending<FeatureFlagValueProvider> { it.priority }
                    .thenBy { it::class.qualifiedName }
            )
            sorted.firstNotNullOfOrNull { it.isEnabled(feature) }
        }
}