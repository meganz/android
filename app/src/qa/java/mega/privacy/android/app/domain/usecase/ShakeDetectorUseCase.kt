package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.presentation.featureflag.model.ShakeEvent

fun interface ShakeDetectorUseCase {
    operator fun invoke(): Flow<ShakeEvent>
}