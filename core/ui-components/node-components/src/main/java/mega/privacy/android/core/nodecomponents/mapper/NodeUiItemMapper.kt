package mega.privacy.android.core.nodecomponents.mapper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.extension.getNodeItemThumbnail
import mega.privacy.android.core.nodecomponents.extension.getSharesIcon
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.toDuration
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Mapper to convert [TypedNode] to [NodeUiItem] with all UI properties
 * This mapper is designed for performance with thousands of items
 */
class NodeUiItemMapper @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val nodeSubtitleMapper: NodeSubtitleMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Map a list of nodes to NodeUiItems
     * @param nodeList List of nodes to map
     * @param nodeSourceType Source type for determining specific logic
     * @param showPublicLinkCreationTime Whether to show public link creation time, for root directory of links
     * @param isPublicNodes Whether the nodes are public nodes like folder links
     * @param highlightedNodeId Optional highlighted node ID
     * @param highlightedNames Optional set of highlighted names
     * @return List of mapped NodeUiItem
     */
    suspend operator fun invoke(
        nodeList: List<TypedNode>,
        nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
        isPublicNodes: Boolean = false,
        showPublicLinkCreationTime: Boolean = false,
        highlightedNodeId: NodeId? = null,
        highlightedNames: Set<String>? = null,
        shouldApplySensitiveMode: Boolean = false,
        isContactVerificationOn: Boolean = false,
    ): List<NodeUiItem<TypedNode>> = withContext(ioDispatcher) {
        nodeList.mapAsync { node ->
            val isHighlighted = node.id == highlightedNodeId ||
                    highlightedNames?.contains(node.name) == true
            val duration = if (node is TypedFileNode) {
                node.type.toDuration()?.let { duration ->
                    durationInSecondsTextMapper(duration)
                }
            } else null
            NodeUiItem(
                node = node,
                isSelected = false,
                isInvisible = false,
                isHighlighted = isHighlighted,
                title = LocalizedText.Literal(node.name), // TODO: Handle unverified shares
                subtitle = nodeSubtitleMapper(
                    node = node,
                    showPublicLinkCreationTime = showPublicLinkCreationTime
                ),
                formattedDescription = node.description?.replace("\n", " ")
                    ?.let { LocalizedText.Literal(it) },
                tags = node.tags.takeIf { nodeSourceType != NodeSourceType.RUBBISH_BIN },
                iconRes = node.getNodeItemThumbnail(fileTypeIconMapper), // TODO: Create separate mapper
                thumbnailData = ThumbnailRequest(
                    id = node.id,
                    isPublicNode = isPublicNodes
                ),
                accessPermissionIcon = (node as? ShareFolderNode)
                    .getSharesIcon(isContactVerificationOn),
                showIsVerified = isContactVerificationOn && node.isIncomingShare
                        && (node as? ShareFolderNode)?.shareData?.isContactCredentialsVerified == true,
                labelColor = null, // TODO: Implement label color logic
                showLink = node.exportedData != null,
                showFavourite = node.isFavourite && node.isIncomingShare.not(),
                isSensitive = nodeSourceType !in setOf(
                    NodeSourceType.INCOMING_SHARES,
                    NodeSourceType.OUTGOING_SHARES,
                    NodeSourceType.LINKS,
                ) && shouldApplySensitiveMode && (node.isMarkedSensitive || node.isSensitiveInherited),
                showBlurEffect = shouldShowBlurEffect(node),
                isFolderNode = node is TypedFolderNode,
                isVideoNode = node is TypedFileNode && node.type is VideoFileTypeInfo,
                duration = duration,
            )
        }
    }

    private fun shouldShowBlurEffect(node: TypedNode): Boolean {
        return (node as? FileNode)?.type?.let { fileTypeInfo ->
            fileTypeInfo is ImageFileTypeInfo || fileTypeInfo is VideoFileTypeInfo || fileTypeInfo is PdfFileTypeInfo || fileTypeInfo is AudioFileTypeInfo
        } == true
    }
}
