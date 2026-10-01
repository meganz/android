package mega.privacy.android.domain.usecase.network

import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use case to broadcast SSL verification failure
 */
class BroadcastSslVerificationFailedUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = networkRepository.broadcastSslVerificationFailed()
}