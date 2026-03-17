package mega.privacy.android.feature.photos.presentation.playlists.videoselect.mapper

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.extension.getIcon
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSubtitleMapper
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import javax.inject.Inject
import mega.privacy.android.shared.nodes.R as NodesR

class SelectVideoItemUiEntityMapper @Inject constructor(
    private val fileTypeIconMapper: FileTypeIconMapper,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val nodeSubtitleMapper: NodeSubtitleMapper,
) {
    operator fun invoke(
        node: TypedNode,
    ) = SelectVideoItemUiEntity(
        id = node.id,
        name = node.name,
        title = getNodeTitle(node),
        subtitle = nodeSubtitleMapper(node = node, showPublicLinkCreationTime = false),
        iconRes = node.getIcon(fileTypeIconMapper),
        isSensitive = node.isMarkedSensitive || node.isSensitiveInherited,
        isFolder = node is TypedFolderNode,
        duration = if (node is TypedFileNode) {
            durationInSecondsTextMapper((node.type as? VideoFileTypeInfo)?.duration)
        } else null,
        isTakenDown = node.isTakenDown,
    )

    private fun getNodeTitle(node: TypedNode): LocalizedText {
        return if (node.isNodeKeyDecrypted.not()) {
            if (node is FileNode)
                LocalizedText.PluralsRes(
                    resId = NodesR.plurals.shared_items_verify_credentials_undecrypted_file,
                    quantity = 1
                )
            else LocalizedText.StringRes(NodesR.string.shared_items_verify_credentials_undecrypted_folder)
        } else {
            LocalizedText.Literal(node.name)
        }
    }
}