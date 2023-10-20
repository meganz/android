package mega.privacy.android.app.presentation.meeting.chat

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Chat view
 *
 * @param uiState
 * @param onBackPressed
 */
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
    PrivateChatIcon(uiState.isPrivateChat)
    MuteIcon(uiState.isChatNotificationMute)
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

@Composable
private fun PrivateChatIcon(isPrivateChat: Boolean?) {
    if (isPrivateChat == true) {
        Icon(
            modifier = Modifier.testTag(TEST_TAG_PRIVATE_ICON).size(16.dp),
            painter = painterResource(id = R.drawable.ic_key_02),
            contentDescription = "private room icon",
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "ChatView")
@Composable
private fun ChatViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val uiState = ChatUiState(
            title = "My Name",
            isChatNotificationMute = true,
            isPrivateChat = true
        )
        ChatView(
            uiState = uiState,
            onBackPressed = {}
        )
    }
}

internal const val TEST_TAG_NOTIFICATION_MUTE = "chat_view:icon_chat_notification_mute"
internal const val TEST_TAG_PRIVATE_ICON = "chat_view:icon_chat_room_private"