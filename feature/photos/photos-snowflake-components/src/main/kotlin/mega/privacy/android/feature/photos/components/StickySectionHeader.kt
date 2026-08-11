package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A generic section header: a title on the start with optional leading and trailing content, laid out
 * over the page background so it can double as a sticky/pinned header without content showing through.
 *
 * @param title the header text.
 * @param modifier the [Modifier] to apply to the header row.
 * @param leadingContent optional content shown before the title (e.g. a select-all checkbox).
 * @param trailingContent optional content shown at the end of the header (e.g. an action button).
 * @param onClick optional click handler for the header row; content with its own click handling
 * still takes precedence within its bounds.
 */
@Composable
fun StickySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DSTokens.colors.background.pageBackground)
            .then(
                onClick?.let {
                    // No ripple: a full-width flash over a section header reads as a glitch.
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = it,
                    )
                } ?: Modifier
            )
            .padding(DSTokens.spacings.s5, vertical = DSTokens.spacings.s4),
        horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent?.invoke()

        MegaText(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTheme.typography.titleSmall,
            textColor = TextColor.Primary,
        )

        trailingContent?.invoke()
    }
}

@CombinedThemePreviews
@Composable
private fun StickySectionHeaderPreview() {
    AndroidThemeForPreviews {
        StickySectionHeader(title = "May 2026")
    }
}

@CombinedThemePreviews
@Composable
private fun StickySectionHeaderWithLeadingContentPreview() {
    AndroidThemeForPreviews {
        StickySectionHeader(
            title = "May 2026",
            leadingContent = {
                Checkbox(
                    checked = true,
                    onCheckStateChanged = {},
                    tapTargetArea = false
                )
            },
        )
    }
}
