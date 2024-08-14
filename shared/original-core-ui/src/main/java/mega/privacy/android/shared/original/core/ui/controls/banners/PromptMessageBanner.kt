package mega.privacy.android.shared.original.core.ui.controls.banners

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body3
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * A banner that displays a prompt message.
 */
@Composable
fun PromptMessageBanner(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MegaOriginalTheme.colors.notifications.notificationError)
            .padding(16.dp)
    ) {
        MegaText(
            text = message,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.body3,
            textAlign = TextAlign.Center,
            overflow = LongTextBehaviour.MiddleEllipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PROMPT_MESSAGE_BANNER_TEXT_TEST_TAG),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PromptMessageBannerPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        PromptMessageBanner("This is a prompt message")
    }
}


internal const val PROMPT_MESSAGE_BANNER_TEXT_TEST_TAG =
    "prompt_message_banner:text"