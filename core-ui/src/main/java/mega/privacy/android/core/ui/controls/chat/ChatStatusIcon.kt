package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

@Composable
fun ChatStatusIcon(
    status: UiChatStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        UiChatStatus.Online -> ChatStatusIcon(
            modifier = modifier,
            background = MegaTheme.colors.indicator.green,
            borderColor = MegaTheme.colors.background.pageBackground
        )

        UiChatStatus.Away -> ChatStatusIcon(
            modifier = modifier,
            background = MegaTheme.colors.indicator.yellow,
            borderColor = MegaTheme.colors.background.pageBackground
        )

        UiChatStatus.Busy -> ChatStatusIcon(
            modifier = modifier,
            background = MegaTheme.colors.indicator.pink,
            borderColor = MegaTheme.colors.background.pageBackground
        )

        else -> ChatStatusIcon(
            modifier = modifier,
            background = MegaTheme.colors.icon.secondary,
            borderColor = MegaTheme.colors.background.pageBackground
        )
    }
}

@Composable
private fun ChatStatusIcon(
    background: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .border(2.dp, borderColor, CircleShape)
            .padding(1.dp)
            .background(background, CircleShape)
    ) {}
}

@CombinedThemePreviews
@Composable
private fun ChatStatusIconPreview(
    @PreviewParameter(ChatStatusProvider::class) status: UiChatStatus,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatStatusIcon(status)
    }
}

internal class ChatStatusProvider :
    CollectionPreviewParameterProvider<UiChatStatus>(UiChatStatus.entries.toList())