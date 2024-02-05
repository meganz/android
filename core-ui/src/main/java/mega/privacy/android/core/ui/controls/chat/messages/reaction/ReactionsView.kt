package mega.privacy.android.core.ui.controls.chat.messages.reaction

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * A container view for the actions
 *
 * @param modifier Modifier. Explict width must be specified in the modifier, so the number of reactions
 *                          per row can be calculated dynamically.
 * @param reactions List of [UIReaction]
 * @param isMine Whether the current user is the sender of the message
 * @param onAddReactionClicked Callback when the add reaction button is clicked
 * @param onReactionClicked Callback when a reaction is clicked
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionsView(
    modifier: Modifier,
    reactions: List<UIReaction> = emptyList(),
    isMine: Boolean = false,
    onAddReactionClicked: () -> Unit = {},
    onReactionClicked: (String) -> Unit = {},
) {
    val systemLayoutDirection = LocalLayoutDirection.current
    val flowDirection = if (isMine) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides flowDirection) {
        FlowRow(
            modifier = modifier.padding(4.dp).testTag(TEST_TAG_REACTIONS_VIEW),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            reactions.forEach {
                ReactionChip(
                    reaction = it,
                    onClick = onReactionClicked,
                    systemLayoutDirection = systemLayoutDirection,
                )
            }
            AddReactionChip(
                onAddClicked = onAddReactionClicked,
            )
        }
    }
}

internal val reactionsList = listOf(
    UIReaction("😀", 1, true),
    UIReaction("😀", 2, false),
    UIReaction("😀", 3, false),
    UIReaction("😀", 4, false),
    UIReaction("😀", 11, true),
    UIReaction("😀", 33, false),
    UIReaction("😀", 44, false),
)

@CombinedThemeRtlPreviews
@Composable
private fun ReactionsViewRtlPreview(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ReactionsView(
            modifier = Modifier.width(300.dp),
            reactions = reactionsList,
            isMine = isMine,
        )
    }
}

internal const val TEST_TAG_REACTIONS_VIEW = "chat_view:message:reactions_view"
