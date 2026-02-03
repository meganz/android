package mega.privacy.android.navigation.contract.bottomsheet

import androidx.navigation3.runtime.NavEntry
import mega.privacy.android.navigation.contract.metadata.NavEntryMetadataScope

object BottomSheetMetadata {
    const val KEY = "bottom_sheet_key"
    const val SKIP_PARTIALLY_EXPANDED = "skip_partially_expanded"
    const val DISMISS_ON_BACK = "dismiss_on_back"
    const val DISMISS_ON_OUTSIDE_CLICK = "dismiss_on_outside_click"
}

/**
 * Registers bottom sheet metadata on this scope so the entry is presented as a bottom sheet.
 *
 * Use inside [mega.privacy.android.navigation.contract.metadata.buildMetadata]:
 * ```
 * metadata = buildMetadata {
 *     withBottomSheet(skipPartiallyExpanded = false)
 *     withAnalytics(SomeEvent)
 * }
 * ```
 *
 * @param skipPartiallyExpanded Whether to skip the partially expanded state (default: true)
 */
fun NavEntryMetadataScope.withBottomSheet(
    skipPartiallyExpanded: Boolean = true,
    dismissOnBack: Boolean = true,
    dismissOnOutsideClick: Boolean = true,
) {
    set(BottomSheetMetadata.KEY, true)
    set(BottomSheetMetadata.SKIP_PARTIALLY_EXPANDED, skipPartiallyExpanded)
    set(BottomSheetMetadata.DISMISS_ON_BACK, dismissOnBack)
    set(BottomSheetMetadata.DISMISS_ON_OUTSIDE_CLICK, dismissOnOutsideClick)
}

/**
 * Creates metadata map to mark a navigation entry as a bottom sheet.
 *
 * Prefer [NavEntryMetadataScope.withBottomSheet] inside [mega.privacy.android.navigation.contract.metadata.buildMetadata] for new code.
 *
 * @param skipPartiallyExpanded Whether to skip the partially expanded state (default: true)
 * @return Map of metadata to mark entry as bottom sheet
 */
fun bottomSheetMetadata(
    skipPartiallyExpanded: Boolean = true,
    dismissOnBack: Boolean = true,
    dismissOnOutsideClick: Boolean = true,
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
