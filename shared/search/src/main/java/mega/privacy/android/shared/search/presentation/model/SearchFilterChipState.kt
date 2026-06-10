package mega.privacy.android.shared.search.presentation.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

/**
 * A single filter chip rendered in the search filter row.
 *
 * @property id Stable identifier the consumer uses to know which filter was tapped.
 * @property label Text shown on the chip (typically the selected option, or a default title).
 * @property isSelected Whether the chip is in the selected (highlighted) state.
 */
@Immutable
data class SearchFilterChipState(
    val id: String,
    val label: LocalizedText,
    val isSelected: Boolean,
)
