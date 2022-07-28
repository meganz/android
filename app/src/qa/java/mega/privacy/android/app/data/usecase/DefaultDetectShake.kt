package mega.privacy.android.app.data.usecase

import android.hardware.SensorManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import mega.privacy.android.app.domain.model.ShakeEvent
import mega.privacy.android.app.domain.usecase.DetectShake
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Implementation of @DetectShake
 *
 * @param repository: @ShakeDetectorRepository
 */
class DefaultDetectShake @Inject constructor(val repository: ShakeDetectorRepository) :
    DetectShake {

    override fun invoke(): Flow<ShakeEvent> {
        return repository.monitorShakeEvents().filter {
            val gX = it.x / SensorManager.GRAVITY_EARTH
            val gY = it.y / SensorManager.GRAVITY_EARTH
            val gZ = it.z / SensorManager.GRAVITY_EARTH
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ))
            gForce > SHAKE_THRESHOLD_GRAVITY
        }
    }

    companion object {
        private const val SHAKE_THRESHOLD_GRAVITY = 2.7f
    }
}