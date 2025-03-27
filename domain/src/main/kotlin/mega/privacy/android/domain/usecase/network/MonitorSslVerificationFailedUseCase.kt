package mega.privacy.android.domain.usecase.network

import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use case to monitor SSL verification failures
 */
class MonitorSslVerificationFailedUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke() = networkRepository.monitorSslVerificationFailed()
}