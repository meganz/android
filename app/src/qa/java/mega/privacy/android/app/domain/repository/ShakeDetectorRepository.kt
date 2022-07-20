package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.model.ShakeEvent

/**
 * Repository to handle interaction to gateway
 */
interface ShakeDetectorRepository {

    /**
     * Function to call @VibratorGateway to vibrate device
     */
    fun vibrateDevice()

    /**
     * Function to monitor sensor event and return flow of @ShakeEvent
     *
     * @return Flow of @ShakeEvent
     */
    fun monitorShakeEvents(): Flow<ShakeEvent>
}