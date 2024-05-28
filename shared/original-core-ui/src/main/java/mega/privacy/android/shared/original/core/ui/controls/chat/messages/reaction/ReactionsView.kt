package mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction

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
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * A container view for the actions
 *
 * @param modifier Modifier. Explict width must be specified in the modifier, so the number of reactions
 *                          per row can be calculated dynamically.
 * @param reactions List of [UIReaction]
 * @param isMine Whether the current user is the sender of the message
 * @param onMoreReactionsClick Callback when the add more reactions button is clicked
 * @param onReactionClick Callback when a reaction is clicked
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionsView(
    modifier: Modifier,
    reactions: List<UIReaction> = emptyList(),
    isMine: Boolean = false,
    interactionEnabled: Boolean = true,
    onMoreReactionsClick: () -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onReactionLongClick: (String) -> Unit = {},
) {
    val systemLayoutDirection = LocalLayoutDirection.current
    val flowDirection = if (isMine) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides flowDirection) {
        FlowRow(
            modifier = modifier
                .padding(vertical = 4.dp)
                .testTag(TEST_TAG_REACTIONS_VIEW),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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

internal val reactionsList = listOf(
    UIReaction("😀", 1, hasMe = true),
    UIReaction("😀", 2, hasMe = false),
    UIReaction("😀", 3, hasMe = false),
    UIReaction("😀", 4, hasMe = false),
    UIReaction("😀", 11, hasMe = true),
    UIReaction("😀", 33, hasMe = false),
    UIReaction("😀", 44, hasMe = false),
)

@CombinedThemeRtlPreviews
@Composable
private fun ReactionsViewRtlPreview(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ReactionsView(
            modifier = Modifier.width(300.dp),
            reactions = reactionsList,
            isMine = isMine,
        )
    }
}

internal const val TEST_TAG_REACTIONS_VIEW = "chat_view:message:reactions_view"
