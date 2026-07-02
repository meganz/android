package mega.privacy.android.core.sharedcomponents.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A generic section header: a title on the start with optional trailing content on the end, laid out
 * over the page background so it can double as a sticky/pinned header without content showing through.
 *
 * @param title the header text.
 * @param modifier the [Modifier] to apply to the header row.
 * @param trailingContent optional content shown at the end of the header (e.g. an action button).
 */
@Composable
fun StickySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DSTokens.colors.background.pageBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaText(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTheme.typography.titleMedium,
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
