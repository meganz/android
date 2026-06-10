package mega.privacy.android.shared.search.presentation.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.surface.ThemedSurface
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.search.presentation.model.SearchFilterChipState

/**
 * Horizontally scrollable row of filter chips driven by a generic list of [SearchFilterChipState].
 *
 * Any analytics or value resolution is the consumer's responsibility: it builds the chip states
 * (resolving labels) and reacts to [onFilterClicked] with the chip [SearchFilterChipState.id].
 */
@Composable
fun SearchFilterChips(
    filters: List<SearchFilterChipState>,
    onFilterClicked: (filterId: String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ThemedSurface(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(state = rememberScrollState())
                .testTag(FILTER_CHIPS_TAG),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.size(8.dp))

            filters.forEach { filter ->
                MegaChip(
                    modifier = Modifier
                        .animateContentSize()
                        .testTag("${FILTER_CHIP_TAG}_${filter.id}"),
                    content = filter.label.text,
                    selected = filter.isSelected,
                    trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
                    onClick = { onFilterClicked(filter.id) },
                    enabled = enabled
                )
            }

            Spacer(Modifier.size(8.dp))
        }
    }
}

@CombinedThemePreviews
@Composable
private fun SearchFilterChipsPreview() {
    AndroidThemeForPreviews {
        SearchFilterChips(
            filters = listOf(
                SearchFilterChipState("type", LocalizedText.Literal("Type"), isSelected = false),
                SearchFilterChipState(
                    "modified",
                    LocalizedText.Literal("Last 7 days"),
                    isSelected = true
                ),
                SearchFilterChipState(
                    "added",
                    LocalizedText.Literal("Date added"),
                    isSelected = false
                ),
            ),
            onFilterClicked = {}
        )
    }
}

internal const val FILTER_CHIPS_TAG = "search_filter_chips"
internal const val FILTER_CHIP_TAG = "search_filter_chips:chip"
