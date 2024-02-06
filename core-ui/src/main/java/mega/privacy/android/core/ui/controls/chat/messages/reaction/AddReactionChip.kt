package mega.privacy.android.core.ui.controls.chat.messages.reaction

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
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme


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
) {
    Box(
        modifier = Modifier
            .size(32.dp, 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color = MegaTheme.colors.background.surface2)
            .border(
                width = 1.dp,
                color = MegaTheme.colors.border.disabled,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onAddClicked)
            .testTag(TEST_TAG_CHAT_MESSAGE_ADD_REACTION_CHIP),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_icon_add_small_regular_outline),
            contentDescription = null,
            tint = MegaTheme.colors.icon.secondary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun AddReactionChipPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AddReactionChip(
            onAddClicked = {}
        )
    }
}