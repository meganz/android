package mega.privacy.android.shared.original.core.ui.controls.chat

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
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
fun ChatStatusIcon(
    status: UiChatStatus,
    modifier: Modifier = Modifier,
) {
    when (status) {
        UiChatStatus.Online -> ChatStatusIcon(
            modifier = modifier,
            background = DSTokens.colors.indicator.green,
            borderColor = DSTokens.colors.background.pageBackground
        )

        UiChatStatus.Away -> ChatStatusIcon(
            modifier = modifier,
            background = DSTokens.colors.indicator.yellow,
            borderColor = DSTokens.colors.background.pageBackground
        )

        UiChatStatus.Busy -> ChatStatusIcon(
            modifier = modifier,
            background = DSTokens.colors.indicator.pink,
            borderColor = DSTokens.colors.background.pageBackground
        )

        else -> ChatStatusIcon(
            modifier = modifier,
            background = DSTokens.colors.icon.secondary,
            borderColor = DSTokens.colors.background.pageBackground
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatStatusIcon(status)
    }
}

internal class ChatStatusProvider :
    CollectionPreviewParameterProvider<UiChatStatus>(UiChatStatus.entries.toList())