package mega.privacy.android.feature.chat.components.reaction

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.components.reaction.model.UIReaction

/**
 * Screenshot tests for the chat reaction components: the reaction chip in its default,
 * own-reacted and long-count states, the add reaction chip, the reactions row for own and other
 * messages, the reactions info view and the quick reactions sheet item.
 *
 * There is no test for ChatEmojiPicker. It hosts the platform EmojiPickerView, which renders
 * empty in layoutlib, so a golden would show only the background.
 */
class ReactionsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionChipDefault() = Chip(UIReaction(reaction = "👍", count = 1, hasMe = false))

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionChipOwnReacted() = Chip(UIReaction(reaction = "👍", count = 2, hasMe = true))

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionChipLongCount() = Chip(UIReaction(reaction = "👍", count = 150, hasMe = true))

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddReactionChipDefault() {
        AndroidThemeForPreviews {
            AddReactionChip(onAddClicked = {}, interactionEnabled = true)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionsViewSingleReaction() = Reactions(
        reactions = listOf(UIReaction(reaction = "😂", count = 1, hasMe = false)),
        isMine = false,
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionsViewMultipleReactionsOther() = Reactions(sampleReactions, isMine = false)

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionsViewMultipleReactionsMine() = Reactions(sampleReactions, isMine = true)

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionsInfoViewOwnReacted() = InfoView(hasMe = true)

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ReactionsInfoViewOtherReacted() = InfoView(hasMe = false)

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddReactionsSheetItemDefault() {
        AndroidThemeForPreviews {
            AddReactionsSheetItem(
                onReactionClicked = {},
                onMoreReactionsClicked = {},
                moreReactionsContentDescription = "Add more reactions",
            )
        }
    }

    @Composable
    private fun Chip(reaction: UIReaction) {
        AndroidThemeForPreviews {
            ReactionChip(
                reaction = reaction,
                onClick = {},
                onLongClick = {},
                systemLayoutDirection = LayoutDirection.Ltr,
                interactionEnabled = true,
            )
        }
    }

    @Composable
    private fun Reactions(reactions: List<UIReaction>, isMine: Boolean) {
        AndroidThemeForPreviews {
            ReactionsView(
                reactions = reactions,
                isMine = isMine,
                interactionEnabled = true,
                onMoreReactionsClick = {},
                onReactionClick = {},
                onReactionLongClick = {},
                modifier = Modifier.width(300.dp),
            )
        }
    }

    @Composable
    private fun InfoView(hasMe: Boolean) {
        AndroidThemeForPreviews {
            ReactionsInfoView(
                currentReaction = ROLLING_EYES_REACTION,
                reactionList = sampleReactionsInfo(hasMe),
                onUserClick = {},
                modifier = Modifier.height(300.dp),
            )
        }
    }
}
