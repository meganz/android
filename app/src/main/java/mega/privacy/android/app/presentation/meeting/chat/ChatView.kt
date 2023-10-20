package mega.privacy.android.app.presentation.meeting.chat

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
fun ChatView(
    uiState: ChatUiState = ChatUiState(),
    onBackPressed: () -> Unit,
) {
    Scaffold(
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = uiState.title.orEmpty(),
                onNavigationPressed = onBackPressed,
                elevation = 0.dp,
                titleIcons = { TitleIcons(uiState) },
            )
        }
    )
    { innerPadding ->
        Text(modifier = Modifier.padding(innerPadding), text = "Hello chat fragment")
    }
}

@Composable
private fun TitleIcons(uiState: ChatUiState) {
    MuteIcon(uiState.isNotificationMute)
}

@Composable
private fun MuteIcon(isNotificationMute: Boolean) {
    if (isNotificationMute) {
        Icon(
            modifier = Modifier.testTag(TEST_TAG_NOTIFICATION_MUTE),
            imageVector = ImageVector.vectorResource(R.drawable.ic_bell_off_small),
            contentDescription = "Mute icon"
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
fun ChatViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val uiState = ChatUiState(
            title = "My Name",
            isNotificationMute = true,
        )
        ChatView(
            uiState = uiState,
            onBackPressed = {}
        )
    }
}

const val TEST_TAG_NOTIFICATION_MUTE = "iconNotificationMute"