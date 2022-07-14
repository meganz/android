package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import javax.inject.Inject

class DefaultGetVibrateCountUseCase @Inject constructor(val repository: ShakeDetectorRepository) :
    GetVibrateCountUseCase {

    override fun invoke(): Flow<Boolean> = repository.getVibrationCount()
}