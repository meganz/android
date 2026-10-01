package mega.privacy.android.domain.usecase.psa

import mega.privacy.android.domain.repository.psa.PsaRepository
import javax.inject.Inject

/**
 * Set displayed psa use case
 *
 * @property psaRepository
 */
class SetDisplayedPsaUseCase @Inject constructor(private val psaRepository: PsaRepository) {

    suspend operator fun invoke(psaId: Int) = psaRepository.setDisplayedPsa(psaId)
}