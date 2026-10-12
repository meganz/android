package mega.privacy.android.domain.usecase.call

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.call.AudioRoute
import mega.privacy.android.domain.repository.CallAudioRouteRepository
import javax.inject.Inject

/**
 * Monitors the device that plays call audio and the devices that are available.
 */
class MonitorCallAudioRouteUseCase @Inject constructor(
    private val callAudioRouteRepository: CallAudioRouteRepository,
) {

    /**
     * Returns the call audio route as state: a new collector receives the latest [AudioRoute]
     * immediately.
     */
    operator fun invoke(): Flow<AudioRoute> = callAudioRouteRepository.monitorAudioRoute()
}
