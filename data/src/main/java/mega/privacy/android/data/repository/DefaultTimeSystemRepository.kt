package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.repository.TimeSystemRepository
import javax.inject.Inject

/**
 * Default time system repository
 *
 */
internal class DefaultTimeSystemRepository @Inject constructor(
    private val deviceGateway: DeviceGateway
) : TimeSystemRepository {
    override fun getCurrentTimeInMillis() = deviceGateway.getCurrentTimeInMillis()
}