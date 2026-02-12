package mega.privacy.android.domain.usecase.psa

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.psa.PsaRepository
import javax.inject.Inject

/**
 * Monitor displayed psa use case
 *
 * @property psaRepository
 */
class MonitorDisplayedPsaUseCase @Inject constructor(private val psaRepository: PsaRepository) {
    operator fun invoke(): Flow<Int?> = psaRepository.monitorDisplayedPsa()
}