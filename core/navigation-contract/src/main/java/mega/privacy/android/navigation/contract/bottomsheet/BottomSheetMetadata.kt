package mega.privacy.android.navigation.contract.bottomsheet

import androidx.navigation3.runtime.NavEntry

object BottomSheetMetadata {
    const val KEY = "bottom_sheet_key"
    const val SKIP_PARTIALLY_EXPANDED = "skip_partially_expanded"
}

fun bottomSheetMetadata(skipPartiallyExpanded: Boolean = true) = mapOf(
    BottomSheetMetadata.KEY to true,
    BottomSheetMetadata.SKIP_PARTIALLY_EXPANDED to skipPartiallyExpanded
)

internal fun NavEntry<*>.isBottomSheet() = this.metadata[BottomSheetMetadata.KEY] == true
internal fun NavEntry<*>.skipPartiallyExpanded() =
    (this.metadata[BottomSheetMetadata.SKIP_PARTIALLY_EXPANDED] ?: true) == true
