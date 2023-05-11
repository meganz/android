package mega.privacy.android.app.presentation.shares.links.model

import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode

/**
 * Links state
 */
sealed interface LinksState {

    /**
     * Loading
     */
    object Loading : LinksState

    /**
     * Data
     *
     * @property links
     */
    data class Data(
        val links: List<PublicLinkNode>,
    ) : LinksState

    /**
     * NoPublicLinks
     */
    object NoPublicLinks : LinksState

    /**
     * Child data
     *
     * @property currentFolder
     * @property links
     */
    data class ChildData(
        val currentFolder: PublicLinkFolder,
        val links: List<PublicLinkNode>,
    ) : LinksState
}