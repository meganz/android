package mega.privacy.android.domain.usecase.network

import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use case for getting the current connectivity state directly from the system
 * instead of the cached state flow
 *
 */
class GetCurrentConnectivityStateUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {

    /**
     * Invoke
     * @return [ConnectivityState]
     */
    suspend operator fun invoke(): ConnectivityState =
        networkRepository.getCurrentConnectivityState()
}
