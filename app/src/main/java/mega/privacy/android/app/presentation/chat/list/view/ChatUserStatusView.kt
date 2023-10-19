package mega.privacy.android.app.presentation.chat.list.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.grey_300
import mega.privacy.android.core.ui.theme.grey_700
import mega.privacy.android.core.ui.theme.lime_green_300
import mega.privacy.android.core.ui.theme.lime_green_500
import mega.privacy.android.core.ui.theme.orange_300
import mega.privacy.android.core.ui.theme.orange_400
import mega.privacy.android.core.ui.theme.salmon_300
import mega.privacy.android.core.ui.theme.salmon_700
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Chat user status icon view
 *
 * @param modifier
 * @param userChatStatus    [UserChatStatus]
 */
@Composable
fun ChatUserStatusView(
    modifier: Modifier = Modifier,
    userChatStatus: UserChatStatus?,
) {
    val isLightMode = MaterialTheme.colors.isLight
    val borderColor = if (isLightMode) white else dark_grey
    val statusColor = when (userChatStatus) {
        UserChatStatus.Online -> if (isLightMode) lime_green_500 else lime_green_300
        UserChatStatus.Away -> if (isLightMode) orange_400 else orange_300
        UserChatStatus.Busy -> if (isLightMode) salmon_700 else salmon_300
        else -> if (isLightMode) grey_700 else grey_300
    }

    Box(
        modifier = modifier
            .size(10.dp)
            .fillMaxSize()
            .background(statusColor, CircleShape)
            .border(2.dp, borderColor, CircleShape),
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewChatUserStatusView(
    @PreviewParameter(UserStatusParameterProvider::class) userChatStatus: UserChatStatus,
) {
    ChatUserStatusView(
        userChatStatus = userChatStatus
    )
}

internal class UserStatusParameterProvider : PreviewParameterProvider<UserChatStatus> {
    override val values = UserChatStatus.values().asSequence()
}
