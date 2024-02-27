package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.conditional


internal const val TEST_TAG_CHAT_MESSAGE_REACTION_CHIP =
    "chat_message_reaction:reaction_chip"

/**
 * Reaction chip
 *
 * @param reaction [UIReaction]
 * @param onClick
 * @param systemLayoutDirection internal layout of Reaction should follow system layout direction
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReactionChip(
    reaction: UIReaction,
    onClick: (String) -> Unit,
    onLongClick: (String) -> Unit,
    systemLayoutDirection: LayoutDirection,
    interactionEnabled: Boolean,
) {
    CompositionLocalProvider(LocalLayoutDirection provides systemLayoutDirection) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(44.dp, 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .conditional(interactionEnabled) {
                    combinedClickable(
                        onClick = { onClick(reaction.reaction) },
                        onLongClick = { onLongClick(reaction.reaction) }
                    )
                }
                .border(
                    width = 1.dp,
                    color = if (reaction.hasMe) MegaTheme.colors.border.strongSelected else MegaTheme.colors.border.disabled,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(color = MegaTheme.colors.background.surface2)
                .padding(bottom = 2.dp)
                .testTag(TEST_TAG_CHAT_MESSAGE_REACTION_CHIP),
        ) {
            Text(
                modifier = Modifier.padding(end = 2.dp),
                text = reaction.reaction,
                color = MegaTheme.colors.border.strongSelected,
                fontSize = 14.sp,
            )
            Text(
                text = "${reaction.userList.size}",
                color = if (reaction.hasMe) MegaTheme.colors.border.strongSelected else MegaTheme.colors.text.secondary,
                style = MaterialTheme.typography.subtitle2,
            )
        }
    }

}


@CombinedThemePreviews
@Composable
private fun ReactionChipWithRtlCountPreview(
    @PreviewParameter(BooleanProvider::class) hasMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ReactionChip(
            reaction = UIReaction(
                reaction = "\uD83C\uDF77",
                hasMe = hasMe,
                count = 1,
                shortCode = ":wine_glass:",
            ),

            onClick = {},
            onLongClick = {},
            systemLayoutDirection = LayoutDirection.Rtl,
            interactionEnabled = true,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ReactionChipWithCountPreview(
    @PreviewParameter(BooleanProvider::class) hasMe: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ReactionChip(
            reaction = UIReaction(
                reaction = "\uD83C\uDF77",
                count = 1,
                hasMe = hasMe,
                shortCode = ":wine_glass:"
            ),
            onClick = {},
            onLongClick = {},
            systemLayoutDirection = LayoutDirection.Ltr,
            interactionEnabled = true,
        )
    }
}