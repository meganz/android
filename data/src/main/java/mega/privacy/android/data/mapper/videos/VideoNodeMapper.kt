package mega.privacy.android.data.mapper.videos

import mega.privacy.android.data.mapper.FileDurationMapper
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.VideoNode
import javax.inject.Inject

internal class VideoNodeMapper @Inject constructor(
    private val durationMapper: FileDurationMapper,
) {
    operator fun invoke(
        fileNode: FileNode,
        thumbnailFilePath: String?
    ) = VideoNode(
        fileNode = fileNode,
        duration = durationMapper(fileNode.type) ?: 0,
        thumbnailFilePath = thumbnailFilePath
    )
}