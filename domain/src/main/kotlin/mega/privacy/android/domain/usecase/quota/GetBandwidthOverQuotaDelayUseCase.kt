package mega.privacy.android.domain.usecase.quota

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case for getting bandwidth width quota delay
 */
class GetBandwidthOverQuotaDelayUseCase @Inject constructor(
    private val repository: NodeRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = repository.getBannerQuotaTime()
}