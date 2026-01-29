package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Default set use https implementation
 *
 * @property networkRepository
 */
class SetUseHttpsUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {
    /**
     * Invoke the use case
     */
    suspend operator fun invoke(enabled: Boolean): Boolean {
        networkRepository.setUseHttps(enabled)

        return enabled
    }
}