package mega.privacy.android.navigation.contract.bottomsheet

import androidx.navigation3.runtime.NavEntry

object BottomSheetMetadata {
    const val KEY = "bottom_sheet_key"
    const val SKIP_PARTIALLY_EXPANDED = "skip_partially_expanded"
    const val DISMISS_ON_BACK = "dismiss_on_back"
    const val DISMISS_ON_OUTSIDE_CLICK = "dismiss_on_outside_click"
}

fun bottomSheetMetadata(
    skipPartiallyExpanded: Boolean = true,
    dismissOnBack: Boolean = false,
    dismissOnOutsideClick: Boolean = false,
) = mapOf(
    BottomSheetMetadata.KEY to true,
    BottomSheetMetadata.SKIP_PARTIALLY_EXPANDED to skipPartiallyExpanded,
    BottomSheetMetadata.DISMISS_ON_BACK to dismissOnBack,
    BottomSheetMetadata.DISMISS_ON_OUTSIDE_CLICK to dismissOnOutsideClick,
)

internal fun NavEntry<*>.isBottomSheet() = this.metadata[BottomSheetMetadata.KEY] == true
internal fun NavEntry<*>.skipPartiallyExpanded() =
    (this.metadata[BottomSheetMetadata.SKIP_PARTIALLY_EXPANDED] ?: true) == true
internal fun NavEntry<*>.dismissOnBack() =
    (this.metadata[BottomSheetMetadata.DISMISS_ON_BACK] ?: false) == true
internal fun NavEntry<*>.dismissOnOutsideClick() =
    (this.metadata[BottomSheetMetadata.DISMISS_ON_OUTSIDE_CLICK] ?: false) == true
