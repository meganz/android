package mega.privacy.android.feature.clouddrive.presentation.filelink.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

@Immutable
data class FileLinkUiState(
    val url: String? = null,
    val contentState: FileLinkContentState = FileLinkContentState.Loading,
    val hasCredentials: Boolean = false,
) {

    /**
     * Title shown in the top app bar — file name with extension when loaded, empty otherwise.
     */
    val title: LocalizedText = when (contentState) {
        is FileLinkContentState.Loaded -> LocalizedText.Literal(contentState.fileNode.name)
        else -> LocalizedText.Literal("")
    }

    /**
     * Subtitle shown beneath the title — "File link" when loaded.
     */
    val subTitle: LocalizedText? = when (contentState) {
        is FileLinkContentState.Loaded -> LocalizedText.Literal("File link") // TODO LocalizedText.StringRes(sharedR.string.file_link_subtitle)
        else -> null
    }
}
