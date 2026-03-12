package mega.privacy.android.shared.nodes.model

import androidx.annotation.StringRes

/**
 * Interface for sort option items that can be used in the SortBottomSheet.
 *
 * @property displayName Human-readable name to display in the UI
 */
interface SortOptionItem {
    @get:StringRes
    val displayName: Int
    val testTag: String
}
