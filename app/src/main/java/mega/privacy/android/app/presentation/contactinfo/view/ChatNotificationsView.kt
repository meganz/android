package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

@Composable
internal fun ChatNotificationsView(isNotificationEnabled: Boolean) = Column {
    Row(
        modifier = Modifier
            .padding(start = 72.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.title_properties_chat_notifications_contact),
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.textColorPrimary,
                lineHeight = 24.sp
            ),
        )
        val switchState by remember {
            derivedStateOf { isNotificationEnabled }
        }
        MegaSwitch(checked = switchState, onCheckedChange = {

        })
    }
    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewChatNotificationsLight() {
    AndroidTheme(isDark = false) {
        Surface {
            ChatNotificationsView(isNotificationEnabled = false)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewChatNotificationsDark() {
    AndroidTheme(isDark = true) {
        Surface {
            ChatNotificationsView(isNotificationEnabled = true)
        }
    }
}