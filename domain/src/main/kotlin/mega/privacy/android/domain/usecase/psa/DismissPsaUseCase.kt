package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.repository.psa.PsaRepository
import javax.inject.Inject

/**
 * Dismiss psa use case
 *
 * @property psaRepository
 */
class DismissPsaUseCase @Inject constructor(
    private val psaRepository: PsaRepository,
) {
    /**
     * Invoke
     *
     * @param psaId
     */
    suspend operator fun invoke(psaId: Int) {
        psaRepository.dismissPsa(psaId)
        psaRepository.clearCache()
        if (psaRepository.monitorDisplayedPsa().firstOrNull()?.equals(psaId) == true) {
            psaRepository.setDisplayedPsa(null)
        }
    }
}