package mega.privacy.android.feature.sharelink.presentation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.AccountType

/**
 * UI state for the revamped Share link result screen.
 *
 * A single node and multiple nodes share the same [Data] state: [Data.nodeLinks] holds one entry
 * per shared node. The single-node layout renders [Data.primary]; the multi-node list is added by
 * AND-24079. Password / expiry / separate-key are single-node only (the multi-node design has no
 * per-node security options), so they stay on [Data] keyed to the primary node.
 */
@Stable
sealed interface ShareLinkUiState {

    /**
     * Shown while the nodes and their links are being resolved.
     */
    data object Loading : ShareLinkUiState

    /**
     * Shown when the nodes could not be loaded or their links could not be created.
     */
    data object Error : ShareLinkUiState

    /**
     * Loaded state with the shared nodes and their public links.
     *
     * @property nodeLinks One entry per shared node, in selection order.
     * @property accountType The current account type, used for Pro gating of link settings.
     * @property isKeySeparate Whether the link and key are shared separately (session state): the
     * link card then shows the key-less link and a separate key card is shown. Single-node only.
     * @property isPasswordSet Whether the link is currently password-protected (session state).
     * @property password The current plaintext password, kept in-session so Link settings can
     * pre-fill it for change/remove; null when not protected. Not rendered.
     * @property linkWithPassword The password-encrypted link to share, or null when not protected.
     */
    data class Data(
        val nodeLinks: List<ShareLinkNodeItem>,
        val accountType: AccountType?,
        val isKeySeparate: Boolean = false,
        val isPasswordSet: Boolean = false,
        val password: String? = null,
        val linkWithPassword: String? = null,
    ) : ShareLinkUiState {

        /** Handles of all shared nodes, in selection order. */
        val handles: List<Long> get() = nodeLinks.map { it.handle }

        /** Whether more than one node is being shared. */
        val isMultiNode: Boolean get() = nodeLinks.size > 1

        /** The first shared node, rendered by the single-node layout. */
        val primary: ShareLinkNodeItem get() = nodeLinks.first()
    }
}

/**
 * A single shared node and its public link.
 *
 * @property handle Node handle.
 * @property name Display name of the node.
 * @property isFolder Whether the node is a folder.
 * @property iconRes Header icon: the file-type icon for files, the folder icon for folders.
 * @property sizeInBytes File size in bytes, or null for folders.
 * @property modificationTime File modification time (seconds since epoch), or null for folders.
 * @property childFolderCount Number of child folders, or null for files.
 * @property childFileCount Number of child files, or null for files.
 * @property link The full public link including the decryption key.
 * @property linkWithoutKey The public link with the decryption key stripped, or null.
 * @property key The decryption key split from the link, or null.
 */
@Stable
data class ShareLinkNodeItem(
    val handle: Long,
    val name: String,
    val isFolder: Boolean,
    @DrawableRes val iconRes: Int,
    val sizeInBytes: Long?,
    val modificationTime: Long?,
    val childFolderCount: Int?,
    val childFileCount: Int?,
    val link: String,
    val linkWithoutKey: String?,
    val key: String?,
)
