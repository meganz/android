package mega.privacy.android.feature.clouddrive.presentation.filelink.model

import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.shared.resources.R as sharedR

@Immutable
data class FileLinkUiState(
    val url: String? = null,
    val contentState: FileLinkContentState = FileLinkContentState.Loading,
    val fileNode: PublicLinkFile? = null,
    val hasCredentials: Boolean = false,
) {

    /**
     * Title shown in the top app bar — file name when loaded, MEGA brand name while
     * loading or when a decryption key is required, empty for expired/unavailable links.
     */
    val title: LocalizedText = when {
        contentState is FileLinkContentState.Expired ||
                contentState is FileLinkContentState.Unavailable -> LocalizedText.Literal("")

        fileNode?.name != null -> LocalizedText.Literal(fileNode.name)
        else -> LocalizedText.StringRes(sharedR.string.photos_empty_screen_brand_name_text)
    }

    /**
     * Subtitle shown in the top app bar — "File link" label, hidden for expired/unavailable links.
     */
    val subTitle: LocalizedText? = when (contentState) {
        FileLinkContentState.Expired, FileLinkContentState.Unavailable -> null
        else -> LocalizedText.StringRes(sharedR.string.file_link_subtitle)
    }
}
