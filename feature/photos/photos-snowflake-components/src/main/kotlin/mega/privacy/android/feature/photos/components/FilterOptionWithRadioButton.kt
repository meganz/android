package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaRadioButton
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor

@Composable
fun FilterOptionWithRadioButton(
    title: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(start = 12.dp, end = 16.dp)
            .testTag(FILTER_OPTION_WITH_RADIO_BUTTON_TEST_TAG),
        horizontalArrangement = Arrangement.spacedBy(12.dp)

    ) {
        MegaRadioButton(
            modifier = Modifier.testTag(FILTER_OPTION_RADIO_BUTTON_TEST_TAG),
            identifier = title,
            selected = selected,
            onOptionSelected = {
                onClick?.invoke()
            }
        )
        MegaText(
            modifier = Modifier.testTag(FILTER_OPTION_TITLE_TEST_TAG),
            text = title,
            textColor = TextColor.Primary,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SettingsItemPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    var selected by remember { mutableStateOf(initialValue) }
    AndroidThemeForPreviews {
        FilterOptionWithRadioButton(
            title = "Filter option",
            selected = selected,
            onClick = { selected = !selected })
    }
}

/**
 * Test tag for [FilterOptionWithRadioButton]
 */
const val FILTER_OPTION_WITH_RADIO_BUTTON_TEST_TAG = "filter_option_with_radio_button"

/**
 * Test tag for radio button in [FilterOptionWithRadioButton]
 */
const val FILTER_OPTION_RADIO_BUTTON_TEST_TAG = "filter_option_radio_button"

/**
 * Test tag for title text in [FilterOptionWithRadioButton]
 */
const val FILTER_OPTION_TITLE_TEST_TAG = "filter_option_title"