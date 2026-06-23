package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText

@Stable
internal data class ExplorerUiState(
    val isLoading: Boolean = true,
    val hasContent: Boolean = false,
    val folderName: LocalizedText = LocalizedText.Literal(""),
    val isConnected: Boolean = true,
    val noConnectionEvent: StateEvent = consumed,
)

internal data class TabSignal(
    val isLoading: Boolean = true,
    val hasContent: Boolean = false,
    val folderName: LocalizedText = LocalizedText.Literal(""),
)
