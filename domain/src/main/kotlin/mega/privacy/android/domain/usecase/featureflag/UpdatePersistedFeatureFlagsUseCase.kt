package mega.privacy.android.domain.usecase.featureflag

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.qualifier.PersistedFeatures
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Update persisted feature flags use case
 *
 * @property featureFlagRepository
 * @property managedFeatures
 */
class UpdatePersistedFeatureFlagsUseCase @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    @PersistedFeatures private val managedFeatures: Set<@JvmSuppressWildcards Feature>,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() {
        val previous = featureFlagRepository.getCurrentPersistedSnapshot()
        val newSnapshot = managedFeatures.associateWith { feature ->
            val fresh = featureFlagRepository.getFeatureValue(feature, SOURCE_PRIORITIES)
            val fallback = previous[feature]
                ?: featureFlagRepository.getFeatureValue(feature, DEFAULT_PRIORITIES)
                ?: false
            fresh ?: fallback
        }
        featureFlagRepository.applySnapshot(newSnapshot)
    }


    private companion object {
        val SOURCE_PRIORITIES = setOf(
            FeatureFlagValuePriority.ConfigurationFile,
            FeatureFlagValuePriority.BuildTimeOverride,
            FeatureFlagValuePriority.RemoteToggled,
        )
        val DEFAULT_PRIORITIES = setOf(FeatureFlagValuePriority.Default)
    }
}
