package mega.privacy.android.feature.chat.components.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.feature.chat.components.reaction.model.UIReaction

/**
 * A chip that shows one reaction and its count.
 *
 * @param reaction The [UIReaction] to show
 * @param onClick Called with the reaction emoji when the user taps the chip
 * @param onLongClick Called with the reaction emoji when the user long presses the chip
 * @param systemLayoutDirection Layout direction for the chip content. The parent row can flip
 * its own direction, but the emoji and the count must keep the system order.
 * @param interactionEnabled False to ignore taps, for example in select mode
 * @param modifier [Modifier]
 */
@Composable
fun ReactionChip(
    reaction: UIReaction,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    systemLayoutDirection: LayoutDirection,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val accentColor = DSTokens.colors.border.strongSelected
    CompositionLocalProvider(LocalLayoutDirection provides systemLayoutDirection) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
                .height(reactionsChipHeight)
                .sizeIn(minWidth = reactionsChipWidth)
                .clip(RoundedCornerShape(12.dp))
                .conditional(interactionEnabled) {
                    combinedClickable(
                        onClick = { onClick(reaction.reaction) },
                        onLongClick = { onLongClick(reaction.reaction) },
                    )
                }
                .border(
                    width = 1.dp,
                    color = if (reaction.hasMe) accentColor else DSTokens.colors.border.disabled,
                    shape = RoundedCornerShape(12.dp),
                )
                .background(color = DSTokens.colors.background.surface2)
                .padding(horizontal = 8.dp)
                .testTag(TEST_TAG_CHAT_MESSAGE_REACTION_CHIP),
        ) {
            Text(
                modifier = Modifier.padding(end = 2.dp),
                text = reaction.reaction,
                fontSize = 14.sp,
            )
            Text(
                text = reaction.countString(),
                color = if (reaction.hasMe) accentColor else DSTokens.colors.text.secondary,
                style = AppTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Minimum width of the reaction chip.
 */
internal val reactionsChipWidth = 44.dp

/**
 * Height of the reaction chip.
 */
internal val reactionsChipHeight = 24.dp

/**
 * Test tag for the reaction chip.
 */
const val TEST_TAG_CHAT_MESSAGE_REACTION_CHIP = "chat_message_reaction:reaction_chip"

@CombinedThemePreviews
@Composable
private fun ReactionChipPreview(
    @PreviewParameter(BooleanProvider::class) hasMe: Boolean,
) {
    AndroidThemeForPreviews {
        ReactionChip(
            reaction = UIReaction(
                reaction = "🍷",
                count = 150,
                hasMe = hasMe,
                shortCode = ":wine_glass:",
            ),
            onClick = {},
            onLongClick = {},
            systemLayoutDirection = LayoutDirection.Ltr,
            interactionEnabled = true,
        )
    }
}
