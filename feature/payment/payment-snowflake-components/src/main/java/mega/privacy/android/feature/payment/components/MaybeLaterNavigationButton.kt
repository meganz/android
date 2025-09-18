package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

@Composable
fun MaybeLaterNavigationButton(
    modifier: Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(DSTokens.colors.background.surface1)
            .padding(horizontal = LocalSpacing.current.x12),
        contentAlignment = Alignment.Center
    ) {
        MegaText(
            text = text,
            textColor = TextColor.Accent,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.bodyLarge
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MaybeLaterNavigationButtonReview() {
    AndroidTheme(isSystemInDarkTheme()) {
        MaybeLaterNavigationButton(
            modifier = Modifier,
            text = "Maybe later",
            onClick = {}
        )
    }
}