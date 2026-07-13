package mega.privacy.android.feature.sharelink.presentation

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.AccountType

/**
 * UI state for the revamped Share link result screen (single node).
 *
 * Multi-node "Share links" is added later (AND-24043); password / expiry / separate-key
 * fields are added by their respective MRs.
 */
@Stable
sealed interface ShareLinkUiState {

    /**
     * Shown while the node and its link are being resolved.
     */
    data object Loading : ShareLinkUiState

    /**
     * Shown when the node could not be loaded or its link could not be created.
     */
    data object Error : ShareLinkUiState

    /**
     * Loaded state with the node details and its public link.
     *
     * @property handles Node handles whose link is being shared.
     * @property nodeName Display name of the node.
     * @property isFolder Whether the node is a folder.
     * @property iconRes Header icon: the file-type icon for files, the folder icon for folders.
     * @property sizeInBytes File size in bytes for the header, or null for folders.
     * @property modificationTime File modification time (seconds since epoch), or null for folders.
     * @property link The full public link including the decryption key.
     * @property linkWithoutKey The public link with the decryption key stripped, or null.
     * @property key The decryption key split from the link, or null.
     * @property accountType The current account type, used for Pro gating of link settings.
     * @property isPasswordSet Whether the link is currently password-protected (session state).
     * @property password The current plaintext password, kept in-session so Link settings can
     * pre-fill it for change/remove; null when not protected. Not rendered.
     * @property linkWithPassword The password-encrypted link to share, or null when not protected.
     */
    data class Data(
        val handles: List<Long>,
        val nodeName: String,
        val isFolder: Boolean,
        @DrawableRes val iconRes: Int,
        val sizeInBytes: Long?,
        val modificationTime: Long?,
        val link: String,
        val linkWithoutKey: String?,
        val key: String?,
        val accountType: AccountType?,
        val isPasswordSet: Boolean = false,
        val password: String? = null,
        val linkWithPassword: String? = null,
    ) : ShareLinkUiState
}
