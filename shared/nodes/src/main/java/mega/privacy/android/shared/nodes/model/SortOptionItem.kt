package mega.privacy.android.shared.nodes.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.node.SortDirection

/**
 * Interface for sort option items that can be used in the SortBottomSheet.
 *
 * @property displayName Human-readable name to display in the UI
 * @property defaultSortDirection Direction applied when the user switches to this option (before toggling).
 */
interface SortOptionItem {
    @get:StringRes
    val displayName: Int
    val testTag: String
    val defaultSortDirection: SortDirection
}
