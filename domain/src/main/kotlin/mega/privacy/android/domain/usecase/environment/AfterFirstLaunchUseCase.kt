package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * After first launch use case
 *
 * @property environmentRepository
 */
class AfterFirstLaunchUseCase @Inject constructor(private val environmentRepository: EnvironmentRepository) {

    /**
     * Invoke
     * Sets the first launch variable to false
     */
    suspend operator fun invoke() = environmentRepository.setIsFirstLaunch(false)
}