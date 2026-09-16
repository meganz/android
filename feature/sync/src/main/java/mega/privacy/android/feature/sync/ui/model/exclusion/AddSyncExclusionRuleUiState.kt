package mega.privacy.android.feature.sync.ui.model.exclusion

import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The two tabs of the Add exclusion screen.
 *
 * Extensions is not a third tab: it is the fourth entry of the Type sheet, which swaps the body of
 * [ByName].
 */
internal enum class AddExclusionTab(@get:StringRes val labelRes: Int) {
    ByName(sharedR.string.sync_exclusions_add_tab_by_name),
    BySize(sharedR.string.sync_exclusions_add_tab_by_size),
}

/**
 * What the Add exclusion screen shows.
 *
 * Each tab keeps its own draft, so a half-filled name rule cannot follow the user into the size
 * form or the reverse. Nothing is persisted until the rule is confirmed, and closing the screen
 * discards every draft with the screen.
 */
internal data class AddSyncExclusionRuleUiState(
    val selectedTab: AddExclusionTab = AddExclusionTab.ByName,
    val byName: AddExclusionByNameFormState = AddExclusionByNameFormState(),
    val byExtension: AddExclusionByExtensionFormState = AddExclusionByExtensionFormState(),
    val bySize: AddExclusionBySizeFormState = AddExclusionBySizeFormState(),
) {

    /**
     * Whether the size tab is holding a rule that cannot be added. The primary button stays
     * enabled and refuses the tap, rather than going grey. The By name tab has no producer for
     * its own error yet, so nothing on it blocks.
     */
    val showsBlockingError: Boolean
        get() = selectedTab == AddExclusionTab.BySize &&
                (bySize.showsRangeError || bySize.hasUnusableBound)

    /** Whether the By name tab is showing extension entry rather than the name form. */
    val showsExtensionForm: Boolean
        get() = selectedTab == AddExclusionTab.ByName &&
                byName.typeOption == SyncExclusionTypeOption.Extensions
}
