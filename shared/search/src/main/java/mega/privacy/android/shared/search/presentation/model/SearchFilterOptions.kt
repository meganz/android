package mega.privacy.android.shared.search.presentation.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

/**
 * The set of options shown in the filter bottom sheet when a chip is tapped.
 *
 * @property id Identifier of the filter these options belong to (matches [SearchFilterChipState.id]).
 * @property title Header title shown at the top of the sheet.
 * @property options Available options for this filter.
 * @property selectedOptionId Currently selected option id, or null if none.
 */
@Immutable
data class SearchFilterOptions(
    val id: String,
    val title: LocalizedText,
    val options: List<SearchFilterOption>,
    val selectedOptionId: String? = null,
)
