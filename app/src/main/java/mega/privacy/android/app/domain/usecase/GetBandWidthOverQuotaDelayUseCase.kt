package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Use case for getting bandwidth width quota delay
 */
class GetBandWidthOverQuotaDelayUseCase @Inject constructor(
    private val repository: NodeRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = repository.getBannerQuotaTime()
}