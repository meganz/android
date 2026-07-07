package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import javax.inject.Inject

/**
 * Use case that emits a signal whenever media count has changed.
 */
class SignalMediaCountChangesUseCase @Inject constructor(
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
) {
    /**
     * @return a flow emitting [Unit] once per node update that affects the media timeline sections.
     */
    operator fun invoke(): Flow<Unit> =
        monitorNodeUpdatesUseCase()
            .filter { update -> update.affectsTimeline() }
            .map { }

    private fun NodeUpdate.affectsTimeline(): Boolean =
        changes.any { (node, nodeChanges) ->
            node is FileNode && node.isMediaNode() &&
                    nodeChanges.any { it in SECTION_AFFECTING_CHANGES }
        }

    private fun FileNode.isMediaNode(): Boolean =
        (type is ImageFileTypeInfo && type !is SvgFileTypeInfo) || type is VideoFileTypeInfo

    private companion object {
        val SECTION_AFFECTING_CHANGES = setOf(
            NodeChanges.New,
            NodeChanges.Remove,
            NodeChanges.Parent,
            NodeChanges.Timestamp,
            NodeChanges.Sensitive
        )
    }
}
