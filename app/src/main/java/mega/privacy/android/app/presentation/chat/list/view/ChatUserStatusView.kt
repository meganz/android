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
import mega.privacy.android.domain.entity.contacts.UserStatus

/**
 * Chat user status icon view
 *
 * @param modifier
 * @param userStatus    [UserStatus]
 */
@Composable
fun ChatUserStatusView(
    modifier: Modifier = Modifier,
    userStatus: UserStatus?,
) {
    val isLightMode = MaterialTheme.colors.isLight
    val borderColor = if (isLightMode) white else dark_grey
    val statusColor = when (userStatus) {
        UserStatus.Online -> if (isLightMode) lime_green_500 else lime_green_300
        UserStatus.Away -> if (isLightMode) orange_400 else orange_300
        UserStatus.Busy -> if (isLightMode) salmon_700 else salmon_300
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
    @PreviewParameter(UserStatusParameterProvider::class) userStatus: UserStatus,
) {
    ChatUserStatusView(
        userStatus = userStatus
    )
}

internal class UserStatusParameterProvider : PreviewParameterProvider<UserStatus> {
    override val values = UserStatus.values().asSequence()
}
