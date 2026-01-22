package mega.privacy.android.feature.photos.presentation.effects

import androidx.compose.runtime.Composable
import de.palm.composestateevents.EventEffect
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.node.AddVideoToPlaylistResult

@Composable
internal fun MediaNodeActionEffects(
    nodeActionState: NodeActionState,
    onDismissRequest: () -> Unit,
    onDismissEventConsumed: () -> Unit,
    onActionTriggered: () -> Unit,
    onActionTriggeredEventConsumed: () -> Unit,
    onAddVideoToPlaylistResult: (AddVideoToPlaylistResult) -> Unit,
    onResetAddVideoToPlaylistResultEventConsumed: () -> Unit,
) {
    EventEffect(
        event = nodeActionState.dismissEvent,
        onConsumed = onDismissEventConsumed,
        action = onDismissRequest
    )

    EventEffect(
        event = nodeActionState.actionTriggeredEvent,
        onConsumed = onActionTriggeredEventConsumed,
        action = onActionTriggered
    )

    EventEffect(
        event = nodeActionState.addVideoToPlaylistResultEvent,
        onConsumed = onResetAddVideoToPlaylistResultEventConsumed,
        action = onAddVideoToPlaylistResult
    )
}
