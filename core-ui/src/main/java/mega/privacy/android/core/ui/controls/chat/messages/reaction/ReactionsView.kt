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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Necessary data for a reaction chip
 *
 * @property reaction [String] of the reaction
 * @property count Count of the reaction
 * @property hasMe Whether the current user has reacted with this reaction
 */
data class Reaction(
    val reaction: String,
    val count: Int,
    val hasMe: Boolean,
)

/**
 * A container view for the actions
 *
 * @param modifier Modifier. Explict width must be specified in the modifier, so the number of reactions
 *                          per row can be calculated dynamically.
 * @param reactions List of [Reaction]
 * @param isMine Whether the current user is the sender of the message
 * @param onAddReactionClicked Callback when the add reaction button is clicked
 * @param onReactionClicked Callback when a reaction is clicked
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionsView(
    modifier: Modifier,
    reactions: List<Reaction> = emptyList(),
    isMine: Boolean = false,
    onAddReactionClicked: () -> Unit = {},
    onReactionClicked: (String) -> Unit = {},
) {
    val systemLayoutDirection = LocalLayoutDirection.current
    val flowDirection = if (isMine) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides flowDirection) {
        FlowRow(
            modifier = modifier.padding(4.dp),
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

@CombinedThemeRtlPreviews
@Composable
private fun ReactionsViewRtlPreview(
    @PreviewParameter(BooleanProvider::class) isMine: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val list = listOf(
            Reaction("😀", 1, true),
            Reaction("😀", 2, false),
            Reaction("😀", 3, false),
            Reaction("😀", 4, false),
            Reaction("😀", 11, true),
            Reaction("😀", 33, false),
            Reaction("😀", 44, false),
        )
        ReactionsView(
            modifier = Modifier.width(300.dp),
            reactions = list,
            isMine = isMine,
        )
    }
}
