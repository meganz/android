package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Is camera upload service running
 *
 */
interface IsCameraUploadRunning {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke(): Flow<Boolean>
}
