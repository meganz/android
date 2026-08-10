package mega.privacy.android.feature.videoeditor.presentation.editor.tool.crop

import android.graphics.RectF
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import mega.privacy.android.feature.videoeditor.components.AspectRatioChip
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.state.ToolRollback
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.BuiltInToolIds
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolAction
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.ToolId

/**
 * Built-in Crop tool.
 *
 * Lifecycle: [pauseOnEnter] is `false` — on long high-bitrate videos the video
 * decoder discards reference frames during a pause, so pausing-then-resuming on
 * Apply would take several seconds to re-establish them.
 *
 * At export this contributes a Media3 [Crop] effect in NDC space; the preview
 * renders the crop via Compose clipping + the [FreeFormCropOverlay] (drawn by
 * `EditorPreview`).
 */
@UnstableApi
object CropTool : EditorTool {

    override val id: ToolId = BuiltInToolIds.Crop
    override val icon: ImageVector = Icons.Filled.Crop
    override val label: String = "Crop"

    override val pauseOnEnter: Boolean = false
    override val resumeOnApply: Boolean = false

    override fun reduce(state: EditorState, action: ToolAction): EditorState {
        val cropAction = action as? CropAction ?: return state
        return when (cropAction) {
            is CropAction.SetRect -> state.copy(crop = state.crop.copy(rect = RectF(cropAction.rect)))
            is CropAction.SetPreset -> {
                val srcW = state.source.widthPx
                val srcH = state.source.heightPx
                val sourceAspect = if (srcW > 0 && srcH > 0) srcW.toFloat() / srcH.toFloat() else null
                val lock = when {
                    cropAction.preset.free -> null
                    cropAction.preset == CropPreset.ORIGINAL -> sourceAspect
                    else -> cropAction.preset.ratio
                }
                state.copy(
                    crop = state.crop.copy(
                        rect = cropRectForPreset(cropAction.preset, srcW, srcH),
                        freeForm = true,
                        aspectLock = lock,
                        selectedPreset = cropAction.preset,
                    ),
                )
            }
        }
    }

    override fun reset(state: EditorState): EditorState =
        state.copy(crop = CropState())

    override fun captureRollback(state: EditorState): ToolRollback {
        val saved = state.crop
        return ToolRollback { it.copy(crop = saved) }
    }

    override fun isApplied(state: EditorState): Boolean =
        !state.crop.isFullFrame

    override fun videoEffects(state: EditorState): List<Effect> {
        if (state.crop.isFullFrame) return emptyList()

        // The crop rect is in normalised source coordinates (0..1, top-left
        // origin). Media3's Crop effect expects normalised device coordinates:
        // both axes span -1..1, with the Y axis flipped (top = +1, bottom = -1).
        val cropRect = state.crop.rect
        val ndcLeft = 2f * cropRect.left - 1f
        val ndcRight = 2f * cropRect.right - 1f
        val ndcTop = 1f - 2f * cropRect.top
        val ndcBottom = 1f - 2f * cropRect.bottom
        return listOf(Crop(ndcLeft, ndcRight, ndcBottom, ndcTop))
    }

    @Composable
    override fun Panel(
        state: EditorState,
        onAction: (ToolAction) -> Unit,
        modifier: Modifier,
    ) {
        // Just the chip row — the crop frame on the preview is the dimensions
        // indicator and "drag corners" affordance. The horizontal inset is
        // applied INSIDE horizontalScroll so the chips scroll edge-to-edge.
        Row(
            modifier = modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CropPreset.entries.forEach { preset ->
                AspectRatioChip(
                    label = preset.displayName,
                    aspectRatio = preset.ratio,
                    isFree = preset.free,
                    selected = state.crop.selectedPreset == preset,
                    onClick = { onAction(CropAction.SetPreset(preset)) },
                )
            }
        }
    }
}
