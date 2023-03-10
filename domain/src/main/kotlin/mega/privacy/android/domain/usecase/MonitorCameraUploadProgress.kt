package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Monitor camera upload pause state
 */
fun interface MonitorCameraUploadProgress {
    /**
     * Invoke
     *
     * @return flow of Pair of
     *          [Int] value representing progress between 0 and 100;
     *          [Int] value representing pending elements waiting for upload
     */
    operator fun invoke(): Flow<Pair<Int, Int>>
}
