package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Is first launch
 *
 * @property environmentRepository
 */
class IsFirstLaunchUseCase @Inject constructor(private val environmentRepository: EnvironmentRepository) {
    /**
     * Invoke
     *
     * @return true if this is the first launch
     */
    suspend operator fun invoke(): Boolean = environmentRepository.getIsFirstLaunch() ?: true
}