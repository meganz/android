package mega.privacy.android.app.presentation.pdfviewer

import mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER
import mega.privacy.android.app.utils.Constants.FROM_CHAT
import mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER
import mega.privacy.android.app.utils.Constants.VERSIONS_ADAPTER
import mega.privacy.android.core.nodecomponents.mapper.ViewTypeToNodeSourceTypeMapper
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.destination.PdfViewerNavKey

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

/**
 * Builds the [PdfViewerNavKey] that opens this content in the Compose PDF viewer.
 *
 * A local file is passed by path; remote content is streamed from its URL and carries whether the
 * HTTP server must be stopped once the viewer closes.
 */
fun NodeContentUri.toPdfViewerNavKey(
    nodeHandle: Long,
    mimeType: String,
    nodeSourceType: NodeSourceType,
    title: String? = null,
    chatId: Long? = null,
    messageId: Long? = null,
    publicLinkUrl: String? = null,
): PdfViewerNavKey = when (this) {
    is NodeContentUri.LocalContentUri -> PdfViewerNavKey(
        nodeHandle = nodeHandle,
        contentUri = file.path,
        isLocalContent = true,
        shouldStopHttpServer = false,
        nodeSourceType = nodeSourceType,
        mimeType = mimeType,
        chatId = chatId,
        messageId = messageId,
        title = title,
        publicLinkUrl = publicLinkUrl,
    )

    is NodeContentUri.RemoteContentUri -> PdfViewerNavKey(
        nodeHandle = nodeHandle,
        contentUri = url,
        isLocalContent = false,
        shouldStopHttpServer = shouldStopHttpSever,
        nodeSourceType = nodeSourceType,
        mimeType = mimeType,
        chatId = chatId,
        messageId = messageId,
        title = title,
        publicLinkUrl = publicLinkUrl,
    )
}
