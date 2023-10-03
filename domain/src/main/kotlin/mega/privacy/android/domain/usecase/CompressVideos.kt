package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.VideoCompressionState

/**
 * Video Compression task
 */
interface CompressVideos {

    /**
     * Invoke
     * @param rootPath
     * @param pendingList [List] of [SyncRecord]
     * @return [Flow] of [VideoCompressionState]
     */
    operator fun invoke(
        rootPath: String,
        pendingList: List<SyncRecord>,
    ): Flow<VideoCompressionState>
}
