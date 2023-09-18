package mega.privacy.android.domain.usecase.psa

import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.repository.psa.PsaRepository
import javax.inject.Inject

/**
 * Fetch psa use case
 *
 * @property psaRepository
 */
class FetchPsaUseCase @Inject constructor(
    private val psaRepository: PsaRepository,
) {
    /**
     * Psa request timeout
     */
    val psaRequestTimeout = 3_600_000L

    /**
     * Invoke
     *
     * @param currentTime
     */
    suspend operator fun invoke(currentTime: Long): Psa? {
        val refreshCache = shouldRefreshCache(currentTime)
        if (refreshCache) psaRepository.setLastFetchedTime(currentTime)
        return psaRepository.fetchPsa(refreshCache)
    }

    private suspend fun shouldRefreshCache(currentTime: Long) =
        (currentTime - psaRequestTimeout) > (psaRepository.getLastPsaFetchedTime()
            ?: Long.MIN_VALUE)
}