package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Use case to get the device SDK version as an [Int].
 *
 * @property environmentRepository [EnvironmentRepository]
 */
class GetDeviceSdkVersionUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     *
     * @return The device SDK version as an [Int].
     */
    operator fun invoke(): Int = environmentRepository.getDeviceSdkVersionInt()
}
