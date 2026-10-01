package mega.privacy.android.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Use case to get the consumer friendly local device name, always starting with the
 * manufacturer followed by the user set device name.
 *
 * @property environmentRepository [EnvironmentRepository]
 */
class GetLocalDeviceNameUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {

    /**
     * Invoke
     *
     * @return the local device name
     */
    operator fun invoke(): String = environmentRepository.getDeviceName()
}
