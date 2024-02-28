package mega.privacy.android.app.presentation.videosection.view.allvideos

import mega.privacy.android.core.R as coreR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.chip.Chip
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Test tag for location filter button text.
 */
const val LOCATION_FILTER_BUTTON_TEXT_TEST_TAG = "location_filter_button_text_test_tag"

/**
 * Test tag for duration filter button text.
 */
const val DURATION_FILTER_BUTTON_TEXT_TEST_TAG = "duration_filter_button_text_test_tag"

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
    Row(modifier = modifier) {
        Chip(
            selected = isLocationFilterSelected,
            contentDescription = "Location filter chip",
            modifier = Modifier,
            onClick = onLocationFilterClicked
        ) {
            MegaText(
                modifier = Modifier.testTag(LOCATION_FILTER_BUTTON_TEXT_TEST_TAG),
                text = getDisplayedText(
                    isSelected = isLocationFilterSelected,
                    defaultText = locationDefaultText,
                    selectedText = locationFilterSelectText
                ),
                textColor = getTextColor(isLocationFilterSelected)
            )
            Icon(
                imageVector = ImageVector.vectorResource(
                    getIconImage(isLocationFilterSelected)
                ),
                contentDescription = "Location filter icon",
            )
        }

        Chip(
            selected = isDurationFilterSelected,
            contentDescription = "Duration filter chip",
            modifier = Modifier.padding(start = 10.dp),
            onClick = onDurationFilterClicked
        ) {
            MegaText(
                modifier = Modifier.testTag(DURATION_FILTER_BUTTON_TEXT_TEST_TAG),
                text = getDisplayedText(
                    isSelected = isDurationFilterSelected,
                    defaultText = durationDefaultText,
                    selectedText = durationFilterSelectText
                ),
                textColor = getTextColor(isDurationFilterSelected)
            )
            Icon(
                imageVector = ImageVector.vectorResource(
                    getIconImage(isDurationFilterSelected)
                ),
                contentDescription = "Duration filter icon",
            )
        }
    }
}

private fun getIconImage(isSelected: Boolean) =
    if (isSelected) coreR.drawable.ic_filter_selected else coreR.drawable.ic_filter_not_selected

private fun getDisplayedText(isSelected: Boolean, defaultText: String, selectedText: String) =
    if (isSelected) selectedText else defaultText

private fun getTextColor(isSelected: Boolean) =
    if (isSelected) TextColor.Inverse else TextColor.Secondary

@CombinedThemePreviews
@Composable
private fun VideosFilterButtonViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
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