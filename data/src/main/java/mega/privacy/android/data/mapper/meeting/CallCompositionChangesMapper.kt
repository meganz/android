package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.meeting.CallCompositionChanges
import nz.mega.sdk.MegaChatCall
import javax.inject.Inject

/**
 * Mapper to convert call composition changes to [CallCompositionChanges]
 */
internal class CallCompositionChangesMapper @Inject constructor() {
    operator fun invoke(endCallReason: Int): CallCompositionChanges = when (endCallReason) {
        MegaChatCall.PEER_REMOVED -> CallCompositionChanges.Removed
        MegaChatCall.NO_COMPOSITION_CHANGE -> CallCompositionChanges.NoChange
        MegaChatCall.PEER_ADDED -> CallCompositionChanges.Added
        else -> CallCompositionChanges.Unknown
    }
}