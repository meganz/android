package mega.privacy.android.domain.usecase.network

import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use case for checking whether device is connected to internet or not
 */
class IsConnectedToInternetUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {

    /**
     * invoke
     * @return [Boolean]
     */
    operator fun invoke() = networkRepository.isConnectedToInternet()
}
