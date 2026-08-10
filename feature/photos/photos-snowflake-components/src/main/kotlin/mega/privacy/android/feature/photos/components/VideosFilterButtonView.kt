package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.chip.SelectionChipStyle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.IconPack

@Composable
fun VideosFilterButtonView(
    isLocationFilterSelected: Boolean,
    isDurationFilterSelected: Boolean,
    locationText: String,
    durationText: String,
    onLocationFilterClicked: () -> Unit,
    onDurationFilterClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MegaChip(
            selected = isLocationFilterSelected,
            content = locationText,
            style = SelectionChipStyle,
            modifier = Modifier.testTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG),
            onClick = onLocationFilterClicked,
            trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
            leadingPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check).takeIf { isLocationFilterSelected },
        )

        MegaChip(
            selected = isDurationFilterSelected,
            content = durationText,
            style = SelectionChipStyle,
            modifier = Modifier.testTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG),
            onClick = onDurationFilterClicked,
            trailingPainter = rememberVectorPainter(IconPack.Small.Thin.Outline.ChevronDown),
            leadingPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check).takeIf { isDurationFilterSelected },
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideosFilterButtonViewPreview() {
    AndroidThemeForPreviews {
        VideosFilterButtonView(
            isLocationFilterSelected = false,
            isDurationFilterSelected = false,
            modifier = Modifier.padding(start = 10.dp),
            onLocationFilterClicked = {},
            onDurationFilterClicked = {},
            locationText = "Location",
            durationText = "Duration",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideosFilterButtonViewSelectedPreview() {
    AndroidThemeForPreviews {
        VideosFilterButtonView(
            isLocationFilterSelected = true,
            isDurationFilterSelected = true,
            modifier = Modifier.padding(start = 10.dp),
            onLocationFilterClicked = {},
            onDurationFilterClicked = {},
            locationText = "Location",
            durationText = "Duration",
        )
    }
}

/**
 * Test tag for location filter button text.
 */
const val LOCATION_FILTER_BUTTON_TEXT_TEST_TAG = "filter_button:text_location"

/**
 * Test tag for duration filter button text.
 */
const val DURATION_FILTER_BUTTON_TEXT_TEST_TAG = "filter_button:text_duration"