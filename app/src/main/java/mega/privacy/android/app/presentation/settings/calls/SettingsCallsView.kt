package mega.privacy.android.app.presentation.settings.calls

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.calls.model.SettingsCallsState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.Typography
import mega.privacy.android.domain.entity.CallsSoundNotifications

@Composable
fun SettingsCallsView(
    settingsCallsState: SettingsCallsState,
    onCheckedChange: (CallsSoundNotifications) -> Unit = {},
) {
    Column {
        CallSoundNotificationsItem(R.string.settings_calls_preferences_sound_notifications,
            R.string.settings_calls_preferences_sound_notifications_text,
            settingsCallsState.soundNotifications == CallsSoundNotifications.Enabled,
            onCheckedChange = onCheckedChange)
    }
}

@Composable
fun CallSoundNotificationsItem(
    titleId: Int,
    textId: Int,
    checked: Boolean,
    onCheckedChange: (CallsSoundNotifications) -> Unit,
) {
    Row(
        modifier = Modifier
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    onCheckedChange(
                        if (it) CallsSoundNotifications.Enabled else CallsSoundNotifications.Disabled
                    )
                })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier
            .weight(1f)
            .padding(start = 0.dp, top = 0.dp, bottom = 0.dp, end = 19.dp)) {
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

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = switchColors()
        )
    }
    Divider(color = colorResource(id = R.color.grey_012_white_012), thickness = 1.dp)
}


@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = colorResource(id = R.color.teal_300_teal_200),
    checkedTrackColor = colorResource(id = R.color.teal_100_teal_200_038),
    uncheckedThumbColor = colorResource(id = R.color.grey_020_grey_100),
    uncheckedTrackColor = colorResource(id = R.color.grey_700_grey_050_038),
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewSettingsCallsView")
@Preview
@Composable
fun PreviewSettingsCallsView() {
    val checked by remember { mutableStateOf(CallsSoundNotifications.Disabled) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SettingsCallsView(
            settingsCallsState = SettingsCallsState(
                soundNotifications = checked
            ))
    }
}