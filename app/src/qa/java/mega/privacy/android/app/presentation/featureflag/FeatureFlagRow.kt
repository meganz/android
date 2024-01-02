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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.core.ui.controls.controlssliders.MegaRadioButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.grey_700
import mega.privacy.android.shared.theme.MegaAppTheme

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
                    .wrapContentWidth(),
                color = MaterialTheme.colors.onSurface,
            )
            if (useRadioButton) {
                MegaRadioButton(selected = isEnabled, onClick = null)
            } else {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = null
                )
            }
        }
        description?.let {
            Text(
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 2.dp,
                    end = 16.dp,
                    bottom = 8.dp
                ),
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

@CombinedThemePreviews
@Composable
private fun FeatureFlagRowPreview() {
    var enabled by remember { mutableStateOf(true) }
    val description by remember {
        derivedStateOf {
            if (enabled) "This is the subtitle, it goes away when you toggle this switch" else null
        }
    }
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FeatureFlagRow(
            name = "Test Flag",
            description = description,
            isEnabled = enabled,
            onCheckedChange = { _, _ -> enabled = !enabled }
        )
    }
}
