package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.model.ShakeEvent

/**
 * Shake detector use case
 */
fun interface DetectShake {
    /**
     * operator function
     *
     * @return flow of @ShakeEvent
     */
    operator fun invoke(): Flow<ShakeEvent>
}