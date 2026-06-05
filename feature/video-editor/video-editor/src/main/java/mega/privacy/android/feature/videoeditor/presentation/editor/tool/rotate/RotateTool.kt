package mega.privacy.android.feature.videoeditor.presentation.editor.tool.rotate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import mega.privacy.android.feature.videoeditor.components.RotateTile
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.BuiltInToolIds
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId

/**
 * Built-in Rotate / Flip tool.
 *
 * Rotation degrees are stored as a running total (NOT reduced mod 360) so that
 * the preview's `animateFloatAsState` can interpolate continuously across the
 * 270→360 wrap. The preview already renders the rotation/flip from the state,
 * so this tool only contributes the reducer, the panel, and the export effect.
 */
@UnstableApi
object RotateTool : EditorTool {

    override val id: ToolId = BuiltInToolIds.Rotate
    override val icon: ImageVector = Icons.Filled.Rotate90DegreesCw
    override val label: String = "Rotate"

    override fun reduce(state: EditorState, action: ToolAction): EditorState {
        val rotateAction = action as? RotateAction ?: return state
        return when (rotateAction) {
            RotateAction.RotateLeft ->
                state.copy(rotate = state.rotate.copy(degrees = state.rotate.degrees - 90))

            RotateAction.RotateRight ->
                state.copy(rotate = state.rotate.copy(degrees = state.rotate.degrees + 90))

            RotateAction.ToggleFlipHorizontal ->
                state.copy(rotate = state.rotate.copy(flipHorizontal = !state.rotate.flipHorizontal))
        }
    }

    override fun reset(state: EditorState): EditorState =
        state.copy(rotate = RotateState())

    override fun captureRollback(state: EditorState): ToolRollback {
        val saved = state.rotate
        return ToolRollback { it.copy(rotate = saved) }
    }

    override fun isApplied(state: EditorState): Boolean =
        !state.rotate.isIdentity

    override fun videoEffects(state: EditorState): List<Effect> {
        if (state.rotate.isIdentity) return emptyList()
        val transformation = ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(state.rotate.degrees.toFloat())
            .apply { if (state.rotate.flipHorizontal) setScale(-1f, 1f) }
            .build()
        return listOf(transformation)
    }

    @Composable
    override fun Panel(
        state: EditorState,
        onAction: (ToolAction) -> Unit,
        modifier: Modifier,
    ) {
        // Three actions in a single row. Current degrees are visible in the
        // live preview itself, so no readout row is needed.
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RotateTile(
                icon = Icons.AutoMirrored.Filled.RotateLeft,
                label = "Left",
                selected = false,
                onClick = { onAction(RotateAction.RotateLeft) },
                modifier = Modifier.weight(1f),
            )
            RotateTile(
                icon = Icons.AutoMirrored.Filled.RotateRight,
                label = "Right",
                selected = false,
                onClick = { onAction(RotateAction.RotateRight) },
                modifier = Modifier.weight(1f),
            )
            RotateTile(
                icon = Icons.Filled.Flip,
                label = "Flip",
                selected = state.rotate.flipHorizontal,
                onClick = { onAction(RotateAction.ToggleFlipHorizontal) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
