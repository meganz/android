package mega.privacy.android.app.presentation.pdfviewer

import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * Resolves a legacy adapter type to the [NodeSourceType] the Compose PDF viewer branches on.
 * [ViewTypeToNodeSourceTypeMapper] covers the cloud sections; the viewer-specific sources it has
 * no case for (offline, chat, contact files, versions) are resolved here.
 */
fun legacyPdfNodeSourceType(
    viewType: Int?,
    viewTypeToNodeSourceTypeMapper: ViewTypeToNodeSourceTypeMapper,
): NodeSourceType = when (viewType) {
    OFFLINE_ADAPTER -> NodeSourceType.OFFLINE
    FROM_CHAT -> NodeSourceType.CHAT
    CONTACT_FILE_ADAPTER -> NodeSourceType.INCOMING_SHARES
    VERSIONS_ADAPTER -> NodeSourceType.VERSIONS
    else -> viewTypeToNodeSourceTypeMapper(viewType)
}
