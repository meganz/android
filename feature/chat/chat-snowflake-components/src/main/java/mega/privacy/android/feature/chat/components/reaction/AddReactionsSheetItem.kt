package mega.privacy.android.feature.chat.components.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * Bottom sheet item that gives quick reactions for a chat message, plus a button that opens
 * the full emoji picker.
 *
 * @param onReactionClicked Called with the emoji when the user taps a quick reaction
 * @param onMoreReactionsClicked Called when the user taps the more reactions button
 * @param moreReactionsContentDescription Accessibility label of the more reactions button
 * @param modifier [Modifier]
 */
@Composable
fun AddReactionsSheetItem(
    onReactionClicked: (String) -> Unit,
    onMoreReactionsClicked: () -> Unit,
    moreReactionsContentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .testTag(TEST_TAG_ADD_REACTIONS_SHEET_ITEM)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        quickReactions.forEach { (reaction, tag) ->
            ReactionItem(
                onClick = { onReactionClicked(reaction) },
                modifier = Modifier.testTag(tag),
                reaction = reaction,
            )
        }
        ReactionItem(
            onClick = onMoreReactionsClicked,
            modifier = Modifier.testTag(TEST_TAG_ADD_MORE_REACTIONS),
            contentDescription = moreReactionsContentDescription,
        )
    }
}

@Composable
private fun ReactionItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reaction: String? = null,
    contentDescription: String? = null,
) = Box(
    modifier = modifier
        .clickable { onClick() }
        .size(48.dp)
        .background(
            color = DSTokens.colors.background.surface2,
            shape = CircleShape,
        ),
    contentAlignment = Alignment.Center,
) {
    if (reaction != null) {
        Text(
            modifier = Modifier.padding(bottom = 1.dp, end = 1.dp),
            text = reaction,
            style = TextStyle(fontSize = 22.sp),
        )
    } else {
        MegaIcon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(IconPackR.drawable.ic_emoji_add_small_thin_outline),
            contentDescription = contentDescription,
            tint = IconColor.Secondary,
        )
    }
}

/**
 * Test tag for the add reactions sheet item.
 */
const val TEST_TAG_ADD_REACTIONS_SHEET_ITEM = "chat_view:add_reactions_sheet_item"

/**
 * Test tag for the slight smile quick reaction.
 */
const val TEST_TAG_SLIGHT_SMILE_REACTION =
    "chat_view:add_reactions_sheet_item:slight_smile_reaction"

/**
 * Test tag for the rolling eyes quick reaction.
 */
const val TEST_TAG_ROLLING_EYES_REACTION =
    "chat_view:add_reactions_sheet_item:rolling_eyes_reaction"

/**
 * Test tag for the rolling on the floor laughing quick reaction.
 */
const val TEST_TAG_ROLLING_ON_THE_FLOOR_LAUGHING_REACTION =
    "chat_view:add_reactions_sheet_item:rolling_on_the_floor_laughing_reaction"

/**
 * Test tag for the thumbs up quick reaction.
 */
const val TEST_TAG_THUMBS_UP_REACTION = "chat_view:add_reactions_sheet_item:thumbs_up_reaction"

/**
 * Test tag for the clap quick reaction.
 */
const val TEST_TAG_CLAP_REACTION = "chat_view:add_reactions_sheet_item:clap_reaction"

/**
 * Test tag for the more reactions button.
 */
const val TEST_TAG_ADD_MORE_REACTIONS = "chat_view:add_reactions_sheet_item:add_more_reactions"

internal const val SLIGHT_SMILE_REACTION = "🙂"
internal const val ROLLING_EYES_REACTION = "🙄"
internal const val ROLLING_ON_THE_FLOOR_LAUGHING_REACTION = "🤣"
internal const val THUMBS_UP_REACTION = "👍"
internal const val CLAP_REACTION = "👏"

private val quickReactions = listOf(
    SLIGHT_SMILE_REACTION to TEST_TAG_SLIGHT_SMILE_REACTION,
    ROLLING_EYES_REACTION to TEST_TAG_ROLLING_EYES_REACTION,
    ROLLING_ON_THE_FLOOR_LAUGHING_REACTION to TEST_TAG_ROLLING_ON_THE_FLOOR_LAUGHING_REACTION,
    THUMBS_UP_REACTION to TEST_TAG_THUMBS_UP_REACTION,
    CLAP_REACTION to TEST_TAG_CLAP_REACTION,
)

@CombinedThemePreviews
@Composable
private fun AddReactionsSheetItemPreview() {
    AndroidThemeForPreviews {
        AddReactionsSheetItem(
            onReactionClicked = {},
            onMoreReactionsClicked = {},
            moreReactionsContentDescription = "Add more reactions",
        )
    }
}
