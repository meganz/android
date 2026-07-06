package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import javax.inject.Inject

/**
 * Emits the ids of media nodes (image or video, excluding SVG) whose render-affecting content changed —
 * favourite or sensitivity — while leaving the timeline sections unchanged. The timeline uses this to
 * patch the affected thumbnails in place (e.g. toggle the favourite icon or the sensitive blur) instead
 * of reloading sections.
 */
class MonitorMediaNodeContentChangesUseCase @Inject constructor(
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
) {
    /**
     * @return a flow emitting the non-empty set of changed media node ids on each relevant update.
     */
    operator fun invoke(): Flow<Set<Long>> =
        monitorNodeUpdatesUseCase()
            .map { update ->
                update.changes
                    .filter { (node, changes) ->
                        node is FileNode && node.isMediaNode() &&
                                changes.any { it in CONTENT_CHANGES }
                    }
                    .keys
                    .map { it.id.longValue }
                    .toSet()
            }
            .filter { it.isNotEmpty() }

    private fun FileNode.isMediaNode(): Boolean =
        (type is ImageFileTypeInfo && type !is SvgFileTypeInfo) || type is VideoFileTypeInfo

    private companion object {
        val CONTENT_CHANGES = setOf(NodeChanges.Favourite, NodeChanges.Sensitive)
    }
}
