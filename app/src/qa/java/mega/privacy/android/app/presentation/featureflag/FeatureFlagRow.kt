package mega.privacy.android.app.presentation.featureflag

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaRadioButton
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.grey_700

/**
 * Creates one row for each element in @@FeatureFlag list
 *
 * @param name: Feature flag name
 * @param isEnabled : Value of feature flag
 * @param onCheckedChange: Lambda function for to handle click
 * @param modifier: Modifier
 */
@Composable
fun FeatureFlagRow(
    name: String,
    description: String?,
    isEnabled: Boolean,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    useRadioButton: Boolean = false,
) {
    Column(
        modifier = modifier
            .toggleable(value = isEnabled,
                role = Role.Switch,
                onValueChange = { onCheckedChange(name, it) })
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                modifier = Modifier
                    .padding(
                        start = 10.dp,
                        top = 6.dp,
                    )
                    .wrapContentWidth()
                    .testTag(TEST_TAG_FEATURE_FLAG_ROW_TITLE),
                color = MaterialTheme.colors.onSurface,
            )
            if (useRadioButton) {
                MegaRadioButton(
                    modifier = Modifier
                        .semantics {
                            // the ui automator can not capture the switch state, so we need to set the contentDescription
                            contentDescription = if (isEnabled) "Enabled" else "Disabled"
                        }
                        .testTag(TEST_TAG_FEATURE_FLAG_ROW_SWITCH),
                    selected = isEnabled,
                    onClick = null
                )
            } else {
                Switch(
                    modifier = Modifier
                        .semantics {
                            // the ui automator can not capture the switch state, so we need to set the contentDescription
                            contentDescription = if (isEnabled) "Enabled" else "Disabled"
                        }
                        .testTag(TEST_TAG_FEATURE_FLAG_ROW_SWITCH),
                    checked = isEnabled,
                    onCheckedChange = null
                )
            }
        }
        description?.let {
            Text(
                modifier = Modifier
                    .padding(
                        start = 10.dp,
                        top = 2.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    )
                    .testTag(TEST_TAG_FEATURE_FLAG_ROW_DESCRIPTION),
                text = description,
                style = TextStyle(
                    color = grey_700,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colors.onSurface,
            )
        }
    }
}

private const val TEST_TAG_FEATURE_FLAG_ROW_TITLE = "feature_flag_row:title"
private const val TEST_TAG_FEATURE_FLAG_ROW_DESCRIPTION = "feature_flag_row:description"
private const val TEST_TAG_FEATURE_FLAG_ROW_SWITCH = "feature_flag_row:switch"

@CombinedThemePreviews
@Composable
private fun FeatureFlagRowPreview() {
    var enabled by remember { mutableStateOf(true) }
    val description by remember {
        derivedStateOf {
            if (enabled) "This is the subtitle, it goes away when you toggle this switch" else null
        }
    }
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FeatureFlagRow(
            name = "Test Flag",
            description = description,
            isEnabled = enabled,
            onCheckedChange = { _, _ -> enabled = !enabled }
        )
    }
}
