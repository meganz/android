package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use case for checking if the device is connected to a wifi network.
 */
class IsOnWifiNetworkUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {

    operator fun invoke(): Boolean = networkRepository.isOnWifi()
}
