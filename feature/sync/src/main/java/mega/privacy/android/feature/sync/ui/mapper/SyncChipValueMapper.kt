package mega.privacy.android.feature.sync.ui.mapper

import mega.privacy.android.feature.sync.ui.synclist.SyncChip
import javax.inject.Inject

/**
 * Maps [Int] value to the corresponding [SyncChip]
 */
class SyncChipValueMapper @Inject constructor() {

    /**
     * Maps [Int] value to the corresponding [SyncChip]
     *
     * @param value [Int] value
     * @return The [SyncChip] corresponding with the value
     */
    operator fun invoke(value: Int): SyncChip {
        return when (value) {
            1 -> SyncChip.STALLED_ISSUES
            2 -> SyncChip.SOLVED_ISSUES
            else -> SyncChip.SYNC_FOLDERS
        }
    }
}