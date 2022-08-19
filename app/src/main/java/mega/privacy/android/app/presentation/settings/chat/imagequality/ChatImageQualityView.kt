package mega.privacy.android.app.presentation.settings.chat.imagequality

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Divider
import androidx.compose.material.RadioButton
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
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.title
import mega.privacy.android.app.presentation.settings.chat.imagequality.model.SettingsChatImageQualityState
import mega.privacy.android.app.presentation.theme.AndroidTheme
import mega.privacy.android.app.presentation.theme.Typography
import mega.privacy.android.domain.entity.ChatImageQuality

@Composable
fun ChatImageQualityView(
    settingsChatImageQualityState: SettingsChatImageQualityState,
    onOptionChanged: (ChatImageQuality) -> Unit = {},
) {
    Column {
        settingsChatImageQualityState.options.forEach {
            ChatImageQualityItem(
                it.title,
                it.description,
                it == settingsChatImageQualityState.selectedQuality
            ) { onOptionChanged(it) }
        }
    }
}

@Composable
fun ChatImageQualityItem(
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
        RadioButton(selected = selected, onClick = null)
    }
    Divider(color = colorResource(id = R.color.grey_012_white_012), thickness = 1.dp)
}

@Preview(uiMode = UI_MODE_NIGHT_YES, name = "DarkPreviewChatImageQualityView")
@Preview
@Composable
fun PreviewChatImageQualityView() {
    var selected by remember { mutableStateOf(ChatImageQuality.Original) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatImageQualityView(
            settingsChatImageQualityState = SettingsChatImageQualityState(
                selectedQuality = selected,
                options = ChatImageQuality.values().asList()
            ),
            onOptionChanged = { selected = it })
    }
}