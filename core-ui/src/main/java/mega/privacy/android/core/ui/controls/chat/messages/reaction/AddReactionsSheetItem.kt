package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Bottom sheet item for adding reactions to a chat message.
 */
@Composable
fun AddReactionsSheetItem(
    onReactionClicked: (String) -> Unit,
    onMoreReactionsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier
        .testTag(TEST_TAG_ADD_REACTIONS_SHEET_ITEM)
        .fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    SLIGHT_SMILE_REACTION.let {
        ReactionItem(
            onClick = { onReactionClicked(it) },
            modifier = Modifier.testTag(TEST_TAG_SLIGHT_SMILE_REACTION),
            reaction = it
        )
    }
    ROLLING_EYES_REACTION.let {
        ReactionItem(
            onClick = { onReactionClicked(it) },
            modifier = Modifier.testTag(TEST_TAG_ROLLING_EYES_REACTION),
            reaction = it,
        )
    }
    ROLLING_ON_THE_FLOOR_LAUGHING_REACTION.let {
        ReactionItem(
            onClick = { onReactionClicked(it) },
            modifier = Modifier.testTag(TEST_TAG_ROLLING_ON_THE_FLOOR_LAUGHING_REACTION),
            reaction = it,
        )
    }
    THUMBS_UP_REACTION.let {
        ReactionItem(
            onClick = { onReactionClicked(it) },
            modifier = Modifier.testTag(TEST_TAG_THUMBS_UP_REACTION),
            reaction = it,
        )
    }
    CLAP_REACTION.let {
        ReactionItem(
            onClick = { onReactionClicked(it) },
            modifier = Modifier.testTag(TEST_TAG_CLAP_REACTION),
            reaction = it,
        )
    }
    ReactionItem(
        onClick = onMoreReactionsClicked,
        modifier = Modifier.testTag(TEST_TAG_ADD_MORE_REACTIONS),
    )
}

@Composable
internal fun ReactionItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    reaction: String? = null,
) = Box(
    modifier = modifier
        .clickable { onClick() }
        .size(48.dp)
        .background(
            color = MegaTheme.colors.background.surface2,
            shape = CircleShape
        ),
    contentAlignment = Alignment.Center
) {

    reaction?.let {
        Text(
            modifier = Modifier.padding(bottom = 1.dp, end = 1.dp),
            text = reaction,
            style = TextStyle(fontSize = 22.sp)
        )
    } ?: Icon(
        modifier = Modifier.size(24.dp),
        imageVector = ImageVector.vectorResource(R.drawable.ic_icon_add_small_regular_outline),
        contentDescription = "Add more reactions",
        tint = MegaTheme.colors.icon.secondary,
    )
}

@CombinedThemePreviews
@Composable
private fun AddReactionSheetPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AddReactionsSheetItem(onReactionClicked = {}, onMoreReactionsClicked = {})
    }
}

/**
 * Test tag for the add reactions sheet item.
 */
const val TEST_TAG_ADD_REACTIONS_SHEET_ITEM = "chat_view:add_reactions_sheet_item"
internal const val TEST_TAG_SLIGHT_SMILE_REACTION =
    "chat_view:add_reactions_sheet_item:slight_smile_reaction"
internal const val TEST_TAG_ROLLING_EYES_REACTION =
    "chat_view:add_reactions_sheet_item:rolling_eyes_reaction"
internal const val TEST_TAG_ROLLING_ON_THE_FLOOR_LAUGHING_REACTION =
    "chat_view:add_reactions_sheet_item:rolling_on_the_floor_laughing_reaction"
internal const val TEST_TAG_THUMBS_UP_REACTION =
    "chat_view:add_reactions_sheet_item:thumbs_up_reaction"
internal const val TEST_TAG_CLAP_REACTION =
    "chat_view:add_reactions_sheet_item:clap_reaction"
internal const val TEST_TAG_ADD_MORE_REACTIONS =
    "chat_view:add_reactions_sheet_item:add_more_reactions"
internal const val SLIGHT_SMILE_REACTION = "\uD83D\uDE42"
internal const val ROLLING_EYES_REACTION = "\uD83D\uDE44"
internal const val ROLLING_ON_THE_FLOOR_LAUGHING_REACTION = "\uD83E\uDD23"
internal const val THUMBS_UP_REACTION = "\uD83D\uDC4D"
internal const val CLAP_REACTION = "\uD83D\uDC4F"