package mega.privacy.android.core.achievementcomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens


@Composable
fun FreeTrailAchievementAwardedText(
    freeTrialText: String,
    isReceivedAward: Boolean,
    isExpired: Boolean,
    isPermanent: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        isPermanent -> Color.Transparent
        else -> DSTokens.colors.background.surface1
    }

    val borderColor = when {
        isPermanent -> Color.Transparent
        isReceivedAward && isExpired -> DSTokens.colors.support.warning
        else -> DSTokens.colors.border.strong
    }

    MegaText(
        text = freeTrialText,
        textColor = TextColor.Secondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(5.dp)
            )
            .border(
                width = if (isReceivedAward) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(5.dp)
            )
            .padding(10.dp)
    )
}