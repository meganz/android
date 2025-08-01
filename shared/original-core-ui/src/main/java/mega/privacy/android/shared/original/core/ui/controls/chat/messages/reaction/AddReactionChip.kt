package mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional


internal const val TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP =
    "chat_message_add_reaction:add_reaction_chip"

/**
 * The button to add a new reaction.
 *
 * @param onAddClicked callback when it is clicked
 */
@Composable
internal fun AddReactionChip(
    onAddClicked: () -> Unit,
    interactionEnabled: Boolean,
) {
    Box(
        modifier = Modifier
            .size(addReactionChipWidth, 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color = DSTokens.colors.background.surface2)
            .border(
                width = 1.dp,
                color = DSTokens.colors.border.disabled,
                shape = RoundedCornerShape(12.dp)
            )
            .conditional(interactionEnabled) { clickable(onClick = onAddClicked) }
            .testTag(TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(mega.privacy.android.icon.pack.R.drawable.ic_emoji_add_small_thin_outline),
            contentDescription = null,
            tint = DSTokens.colors.icon.secondary,
        )
    }
}

/**
 * Width of the add reaction chip
 */
internal val addReactionChipWidth = 32.dp

@CombinedThemePreviews
@Composable
private fun AddReactionChipPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        AddReactionChip(
            onAddClicked = {},
            interactionEnabled = true
        )
    }
}