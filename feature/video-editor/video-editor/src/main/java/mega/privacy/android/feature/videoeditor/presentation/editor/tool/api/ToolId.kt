package mega.privacy.android.feature.videoeditor.presentation.editor.tool.api

import androidx.compose.runtime.Immutable

/**
 * Stable identifier for a tool. Built-in tools use the constants in
 * [BuiltInToolIds]; custom tools should pick a unique string (e.g.,
 * `"com.example.video.filter"`) to avoid clashes.
 */
@JvmInline
@Immutable
value class ToolId(val value: String) {
    override fun toString(): String = value
}

object BuiltInToolIds {
    val Trim = ToolId("trim")
    val Crop = ToolId("crop")
    val Rotate = ToolId("rotate")
    val Speed = ToolId("speed")
    val Volume = ToolId("volume")
}
