package mega.privacy.android.feature.videoeditor.presentation.editor.engine

import androidx.media3.common.Effect
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Effects
import mega.privacy.android.feature.videoeditor.presentation.editor.state.EditorState
import mega.privacy.android.feature.videoeditor.presentation.editor.tool.api.EditorTool

/**
 * Combine the per-tool [Effect] and [AudioProcessor] contributions of `tools`
 * into a single [Effects] bundle for the transformer.
 *
 * The order in `tools` matters: built-in tools are registered in the order
 * Trim → Crop → Rotate → Speed → Volume, which gives Crop → Rotate → Speed
 * at export (Trim is in MediaItem clipping, Volume is audio).
 */
@UnstableApi
fun composeEffects(state: EditorState, tools: List<EditorTool>): Effects {
    val video = mutableListOf<Effect>()
    val audio = mutableListOf<AudioProcessor>()
    tools.forEach { tool ->
        video += tool.videoEffects(state)
        audio += tool.audioProcessors(state)
    }
    return Effects(audio, video)
}
