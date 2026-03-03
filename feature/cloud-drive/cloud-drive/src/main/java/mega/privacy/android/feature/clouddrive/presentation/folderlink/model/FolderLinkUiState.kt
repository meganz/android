package mega.privacy.android.feature.clouddrive.presentation.folderlink.model

import androidx.compose.runtime.Immutable

@Immutable
data class FolderLinkUiState(
    val contentState: FolderLinkContentState = FolderLinkContentState.Loading,
)
