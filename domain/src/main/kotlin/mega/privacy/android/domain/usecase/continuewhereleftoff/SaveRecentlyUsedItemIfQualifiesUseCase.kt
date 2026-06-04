package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.entity.continuewhereleftoff.CWLO_NEAR_COMPLETION_FRACTION
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import javax.inject.Inject

/**
 * Records, on leaving a document-style viewer (PDF, text editor), whether the item qualifies for
 * the Continue Where Left Off carousel based on how far it was read.
 *
 * Below [CWLO_NEAR_COMPLETION_FRACTION] the item qualifies and is saved as resumable; at or above
 * it the item is read through, so any existing entry is removed instead of being resurfaced —
 * mirroring the near-completion exclusion applied to audio/video.
 */
class SaveRecentlyUsedItemIfQualifiesUseCase @Inject constructor(
    private val saveRecentlyUsedItemUseCase: SaveRecentlyUsedItemUseCase,
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase,
) {
    /**
     * @param nodeHandle the item's node handle.
     * @param type the item's content type.
     * @param fileName the item's file name, stored for fast widget loading.
     * @param progress the read-through progress, in the range 0.0..1.0.
     * @return true if the item was saved as resumable, false if it was removed as read through.
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        type: RecentlyUsedType,
        fileName: String,
        progress: Float,
    ): Boolean {
        if (progress >= CWLO_NEAR_COMPLETION_FRACTION) {
            removeRecentlyUsedItemUseCase(nodeHandle)
            return false
        }
        saveRecentlyUsedItemUseCase(nodeHandle, type, fileName)
        return true
    }
}
