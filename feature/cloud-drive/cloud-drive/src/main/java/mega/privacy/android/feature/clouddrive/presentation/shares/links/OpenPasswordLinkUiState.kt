package mega.privacy.android.feature.clouddrive.presentation.shares.links

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

data class OpenPasswordLinkUiState(
    val decryptedLinkEvent: StateEventWithContent<DecryptedLink> = consumed(),
    val errorMessage: Boolean = false,
)

sealed interface DecryptedLink {
    val link: String

    data class FileLink(override val link: String) : DecryptedLink
    data class FolderLink(override val link: String) : DecryptedLink

}