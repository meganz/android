package mega.privacy.android.app.presentation.videosection.view.allvideos

import mega.privacy.android.core.R as coreR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.chip.ChipBar
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun VideosFilterButtonView(
    isLocationFilterSelected: Boolean,
    isDurationFilterSelected: Boolean,
    locationDefaultText: String,
    durationDefaultText: String,
    locationFilterSelectText: String,
    durationFilterSelectText: String,
    modifier: Modifier,
    onLocationFilterClicked: () -> Unit,
    onDurationFilterClicked: () -> Unit,
) {
    ChipBar(modifier = modifier) {
        MegaChip(
            selected = isLocationFilterSelected,
            text = if (isLocationFilterSelected) locationFilterSelectText else locationDefaultText,
            modifier = Modifier.testTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG),
            onClick = onLocationFilterClicked,
            trailingIcon = coreR.drawable.ic_chevron_down,
            leadingIcon = coreR.drawable.ic_filter_selected.takeIf { isLocationFilterSelected },
        )
        MegaChip(
            selected = isDurationFilterSelected,
            text = if (isDurationFilterSelected) durationFilterSelectText else durationDefaultText,
            modifier = Modifier.testTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG),
            onClick = onDurationFilterClicked,
            trailingIcon = coreR.drawable.ic_chevron_down,
            leadingIcon = coreR.drawable.ic_filter_selected.takeIf { isDurationFilterSelected },
        )

    }
}

@CombinedThemePreviews
@Composable
private fun VideosFilterButtonViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideosFilterButtonView(
            isLocationFilterSelected = false,
            isDurationFilterSelected = false,
            modifier = Modifier.padding(start = 10.dp),
            onLocationFilterClicked = {},
            onDurationFilterClicked = {},
            locationDefaultText = "Location",
            durationDefaultText = "Duration",
            locationFilterSelectText = "Cloud drive",
            durationFilterSelectText = "More than 20 minutes"
        )
    }
}

@CombinedThemePreviews
@Composable
private fun VideosFilterButtonViewSelectedPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VideosFilterButtonView(
            isLocationFilterSelected = true,
            isDurationFilterSelected = true,
            modifier = Modifier.padding(start = 10.dp),
            onLocationFilterClicked = {},
            onDurationFilterClicked = {},
            locationDefaultText = "Location",
            durationDefaultText = "Duration",
            locationFilterSelectText = "Cloud drive",
            durationFilterSelectText = "More than 20 minutes"
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