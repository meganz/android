package mega.privacy.android.app.presentation.settings.calls

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.calls.model.SettingsCallsState
import mega.privacy.android.app.presentation.theme.AndroidTheme
import mega.privacy.android.app.presentation.theme.Typography
import mega.privacy.android.domain.entity.CallsSoundNotifications

@Composable
fun SettingsCallsView(
    settingsCallsState: SettingsCallsState,
    onOptionChanged: (CallsSoundNotifications) -> Unit = {},
) {
    Column {
        settingsCallsState.let {
            CallSoundNotificationsItem(R.string.automatic_image_quality,
                R.string.automatic_image_quality,
                true) {
                onOptionChanged(it.soundNotifications ?: return@CallSoundNotificationsItem)
            }
        }
    }
}

@Composable
fun CallSoundNotificationsItem(
    titleId: Int,
    textId: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .toggleable(
                value = selected,
                role = Role.RadioButton,
                onValueChange = {})
            .clickable { onClick() }
            .padding(start = 15.dp, top = 16.dp, bottom = 16.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(id = titleId),
                style = Typography.subtitle1,
                color = colorResource(id = R.color.grey_087_white_087)
            )
            Text(
                text = stringResource(id = textId), style = Typography.subtitle2,
                color = colorResource(id = R.color.grey_054_white_054)
            )
        }
        Switch(checked = selected, onCheckedChange = null)
    }
    Divider(color = colorResource(id = R.color.grey_012_white_012), thickness = 1.dp)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewSettingsCallsView")
@Preview
@Composable
fun PreviewSettingsCallsView() {
    var selected by remember { mutableStateOf(CallsSoundNotifications.Enabled) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SettingsCallsView(
            settingsCallsState = SettingsCallsState(
                CallsSoundNotifications.Enabled
            ),
            onOptionChanged = { selected = it })
    }
}