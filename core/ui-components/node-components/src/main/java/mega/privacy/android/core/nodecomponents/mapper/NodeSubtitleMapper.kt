package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

// TODO: Use string res and remove usage of context
/**
 * Mapper for creating node subtitles
 */
class NodeSubtitleMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Get subtitle for a node
     * @param node The node to get subtitle for
     * @param showPublicLinkCreationTime Whether to show public link creation time
     * @return LocalizedText subtitle
     */
    operator fun invoke(node: TypedNode, showPublicLinkCreationTime: Boolean): LocalizedText {
        return LocalizedText.Literal(
            value = when (node) {
                is TypedFileNode -> formatFileSize(node.size, context)
                    .plus(" · ")
                    .plus(
                        formatModifiedDate(
                            java.util.Locale.getDefault(),
                            if (showPublicLinkCreationTime) node.exportedData?.publicLinkCreationTime
                                ?: node.modificationTime
                            else node.modificationTime
                        )
                    )

                is TypedFolderNode -> {
                    "${node.childFolderCount} folder, ${node.childFileCount} file"
                }

                else -> ""
            }
        )
    }
} 