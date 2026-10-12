package mega.privacy.android.feature.chat.components.reaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import mega.android.core.ui.preview.BooleanProvider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.components.reaction.model.UIReaction

/**
 * The reactions of a chat message, followed by the add reaction chip. The chips wrap onto new
 * rows when they do not fit the available width. Use it in the reactions slot of
 * ChatMessageContainer.
 *
 * @param reactions The reactions to show
 * @param isMine True if the current user sent the message. The chips then start from the end.
 * @param interactionEnabled False to ignore taps, for example in select mode
 * @param onMoreReactionsClick Called when the user taps the add reaction chip
 * @param onReactionClick Called with the reaction emoji when the user taps a reaction
 * @param onReactionLongClick Called with the reaction emoji when the user long presses a reaction
 * @param modifier [Modifier]
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionsView(
    reactions: List<UIReaction>,
    isMine: Boolean,
    interactionEnabled: Boolean,
    onMoreReactionsClick: () -> Unit,
    onReactionClick: (String) -> Unit,
    onReactionLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemLayoutDirection = LocalLayoutDirection.current
    // RTL flow puts the chips of own messages against the end edge, next to the bubble.
    val flowDirection = if (isMine) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides flowDirection) {
        FlowRow(
            modifier = modifier
                .padding(vertical = 4.dp)
                .testTag(TEST_TAG_REACTIONS_VIEW),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            reactions.forEach {
                ReactionChip(
                    reaction = it,
                    onClick = onReactionClick,
                    onLongClick = onReactionLongClick,
                    systemLayoutDirection = systemLayoutDirection,
                    interactionEnabled = interactionEnabled,
                )
            }
            AddReactionChip(
                onAddClicked = onMoreReactionsClick,
                interactionEnabled = interactionEnabled,
            )
        }
    }
}

/**
 * Approximate height of a [ReactionsView] with [amountOfReactions] reactions on a screen of
 * [screenWidth]. It assumes the minimum chip width, so the real height can be larger.
 */
fun computeReactionsViewApproximateHeight(
    amountOfReactions: Int,
    screenWidth: Dp,
): Dp =
    if (amountOfReactions == 0) {
        0.dp
    } else {
        val rows = amountOfReactions / calculateItemsPerRow(screenWidth) + 1
        val verticalPaddings = 22.dp
        reactionsChipHeight * rows.toFloat() + itemSpacing * (rows - 1) + verticalPaddings
    }

private fun calculateItemsPerRow(screenWidth: Dp): Int {
    val availableWidth = screenWidth - addReactionChipWidth - avatarWidth
    val numElements = availableWidth / (reactionsChipWidth + itemSpacing)
    return numElements.toInt().coerceAtLeast(1)
}

/**
 * Test tag for the reactions view.
 */
const val TEST_TAG_REACTIONS_VIEW = "chat_view:message:reactions_view"

private val itemSpacing = 4.dp

/**
 * Width of the avatar column of ChatMessageContainer, which the reactions row does not use.
 */
private val avatarWidth = 40.dp

internal val sampleReactions = listOf(
    UIReaction("😀", 1, hasMe = true),
    UIReaction("😂", 2, hasMe = false),
    UIReaction("👍", 3, hasMe = false),
    UIReaction("🎉", 4, hasMe = false),
    UIReaction("❤️", 11, hasMe = true),
    UIReaction("🔥", 33, hasMe = false),
    UIReaction("👏", 150, hasMe = false),
)

@CombinedThemePreviews
@Composable
private fun ReactionsViewPreview(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    AndroidThemeForPreviews {
        ReactionsView(
            reactions = sampleReactions,
            isMine = isMine,
            interactionEnabled = true,
            onMoreReactionsClick = {},
            onReactionClick = {},
            onReactionLongClick = {},
            modifier = Modifier.width(300.dp),
        )
    }
}
