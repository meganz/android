package mega.privacy.android.app.domain.repository

import android.hardware.SensorEvent
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.presentation.featureflag.model.ShakeEvent

interface ShakeDetectorRepository {

    fun vibrateDevice()

    fun getVibrationCount(): Flow<Boolean>

    fun monitorShakeEvents(): Flow<ShakeEvent>
}