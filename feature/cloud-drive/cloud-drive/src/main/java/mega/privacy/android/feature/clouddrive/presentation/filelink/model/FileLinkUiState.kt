package mega.privacy.android.feature.clouddrive.presentation.filelink.model

import androidx.compose.runtime.Immutable
import mega.privacy.android.domain.entity.node.TypedFileNode

@Immutable
data class FileLinkUiState(
    val url: String? = null,
    val contentState: FileLinkContentState = FileLinkContentState.Loading,
    val fileNode: TypedFileNode? = null,
    val hasCredentials: Boolean = false,
) {

    /**
     * Title shown in the top app bar — file name with extension when loaded, empty otherwise.
     */
    val title: String = fileNode?.name.orEmpty()
}
