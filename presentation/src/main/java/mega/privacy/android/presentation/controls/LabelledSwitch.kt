package mega.privacy.android.presentation.controls

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.grey_010
import mega.privacy.android.presentation.theme.grey_200
import mega.privacy.android.presentation.theme.grey_alpha_038
import mega.privacy.android.presentation.theme.teal_100
import mega.privacy.android.presentation.theme.teal_300

@Composable
fun LabelledSwitch(
    label: String,
    checked: Boolean,
    onCheckChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckChanged
            )
    ) {
        Text(text = label)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = if (MaterialTheme.colors.isLight) lightSwitchColors() else darkSwitchColors()
        )
    }
}

@Composable
private fun darkSwitchColors() = SwitchDefaults.colors(
    uncheckedThumbColor = grey_200,
)

@Composable
private fun lightSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = teal_300,
    checkedTrackColor = teal_100,
    uncheckedThumbColor = grey_010,
    uncheckedTrackColor = grey_alpha_038,
)

@ShowkaseComposable("Labelled Switch", "Controls and sliders")
@Composable
fun ShowkasePreviewLabelledSwitch() = PreviewLabelledSwitch()

@Preview
@Preview(
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewLabelledSwitch() {
    var checked by remember { mutableStateOf(true) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LabelledSwitch(label = "The label",
            checked = checked,
            onCheckChanged = { checked = !checked })
    }
}