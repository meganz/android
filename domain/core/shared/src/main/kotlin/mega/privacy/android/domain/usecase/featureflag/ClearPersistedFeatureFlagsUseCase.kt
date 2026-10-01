package mega.privacy.android.domain.usecase.featureflag

import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Clear persisted feature flags use case
 *
 * @property featureFlagRepository
 */
class ClearPersistedFeatureFlagsUseCase @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = featureFlagRepository.clearPersistedSnapshot()
}
