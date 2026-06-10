package mega.privacy.android.shared.search.presentation.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

/**
 * A selectable option within a [SearchFilterOptions] picker.
 *
 * @property id Stable identifier the consumer maps back to its own filter value.
 * @property label Text shown for the option.
 */
@Immutable
data class SearchFilterOption(
    val id: String,
    val label: LocalizedText,
)
