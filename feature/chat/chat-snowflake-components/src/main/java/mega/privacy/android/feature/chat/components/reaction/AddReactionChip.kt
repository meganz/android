package mega.privacy.android.feature.chat.components.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * The chip that adds a new reaction to a message.
 *
 * @param onAddClicked Called when the user taps the chip
 * @param interactionEnabled False to ignore taps, for example in select mode
 * @param modifier [Modifier]
 */
@Composable
internal fun AddReactionChip(
    onAddClicked: () -> Unit,
    interactionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(addReactionChipWidth, reactionsChipHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(color = DSTokens.colors.background.surface2)
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.disabled,
                shape = RoundedCornerShape(12.dp),
            )
            .conditional(interactionEnabled) { clickable(onClick = onAddClicked) }
            .testTag(TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP),
        contentAlignment = Alignment.Center,
    ) {
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_emoji_add_small_thin_outline),
            tint = IconColor.Secondary,
            contentDescription = null,
        )
    }
}

/**
 * Width of the add reaction chip.
 */
internal val addReactionChipWidth = 32.dp

/**
 * Test tag for the add reaction chip.
 */
const val TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP = "chat_message_add_reaction:add_reaction_chip"

@CombinedThemePreviews
@Composable
private fun AddReactionChipPreview() {
    AndroidThemeForPreviews {
        AddReactionChip(
            onAddClicked = {},
            interactionEnabled = true,
        )
    }
}
