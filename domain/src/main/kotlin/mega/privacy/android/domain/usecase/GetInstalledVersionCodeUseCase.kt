package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

/**
 * Get Installed VersionCode Use Case
 */
class GetInstalledVersionCodeUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Int = environmentRepository.getInstalledVersionCode()
}