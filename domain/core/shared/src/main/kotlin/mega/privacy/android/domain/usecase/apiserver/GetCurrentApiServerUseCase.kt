package mega.privacy.android.domain.usecase.apiserver

import mega.privacy.android.domain.entity.apiserver.ApiServer
import mega.privacy.android.domain.repository.apiserver.ApiServerRepository
import javax.inject.Inject

/**
 * Get current api server
 *
 * @property apiServerRepository [ApiServerRepository]
 */
class GetCurrentApiServerUseCase @Inject constructor(
    private val apiServerRepository: ApiServerRepository,
) {

    /**
     * Invoke
     *
     * @return [ApiServer]
     */
    suspend operator fun invoke() = apiServerRepository.getCurrentApi()
}