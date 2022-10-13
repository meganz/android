package mega.privacy.android.app.presentation.featureflag

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.grey_700

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
            )
            Switch(
                checked = isEnabled,
                onCheckedChange = null
            )
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
                )
            )
        }
    }
}

@Preview
@Preview(
    uiMode = UI_MODE_NIGHT_YES,
    name = "DarkFeatureFlagRowPreview"
)
@Composable
fun FeatureFlagRowPreview() {
    var enabled by remember { mutableStateOf(true) }
    val description by derivedStateOf {
        if (enabled) "This is the subtitle, it goes away when you toggle this switch" else null
    }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FeatureFlagRow(
            name = "Test Flag",
            description = description,
            isEnabled = enabled,
            onCheckedChange = { _, _ -> enabled = !enabled }
        )
    }
}
