package mega.privacy.android.app.presentation.meeting.chat.view.appbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isValid
import mega.privacy.android.app.presentation.extensions.vectorRes
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.domain.entity.contacts.UserChatStatus

@Composable
internal fun TitleIcons(uiState: ChatUiState) {
    UserChatStateIcon(uiState.userChatStatus)
    PrivateChatIcon(uiState.isPrivateChat)
    MuteIcon(uiState.isChatNotificationMute)
}

@Composable
private fun UserChatStateIcon(userChatStatus: UserChatStatus?) {
    if (userChatStatus?.isValid() == true) {
        Image(
            painter = painterResource(id = userChatStatus.vectorRes(MaterialTheme.colors.isLight)),
            modifier = Modifier.testTag(TEST_TAG_USER_CHAT_STATE),
            contentDescription = "Status icon"
        )
    }
}

@Composable
private fun MuteIcon(isNotificationMute: Boolean) {
    if (isNotificationMute) {
        Icon(
            modifier = Modifier.testTag(TEST_TAG_NOTIFICATION_MUTE),
            painter = painterResource(id = R.drawable.ic_bell_off_small),
            contentDescription = "Mute icon"
        )
    }
}

@Composable
private fun PrivateChatIcon(isPrivateChat: Boolean?) {
    if (isPrivateChat == true) {
        Icon(
            modifier = Modifier
                .testTag(TEST_TAG_PRIVATE_ICON)
                .size(16.dp),
            painter = painterResource(id = R.drawable.ic_key_02),
            contentDescription = "private room icon",
        )
    }
}