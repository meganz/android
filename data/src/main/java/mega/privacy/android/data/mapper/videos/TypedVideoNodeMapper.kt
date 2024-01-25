package mega.privacy.android.data.mapper.videos

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class TypedVideoNodeMapper @Inject constructor() {
    operator fun invoke(
        fileNode: FileNode,
        duration: Int,
    ) = TypedVideoNode(
        fileNode = fileNode,
        duration = duration.seconds,
    )
}