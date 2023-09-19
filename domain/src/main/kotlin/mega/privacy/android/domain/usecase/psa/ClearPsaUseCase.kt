package mega.privacy.android.domain.usecase.psa

import mega.privacy.android.domain.repository.psa.PsaRepository
import javax.inject.Inject

/**
 * Clear psa use case
 *
 * @property psaRepository
 */
class ClearPsaUseCase @Inject constructor(private val psaRepository: PsaRepository) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() {
        psaRepository.clearCache()
        psaRepository.setLastFetchedTime(null)
    }
}