package mega.privacy.android.data.mapper.audios

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedAudioNode
import javax.inject.Inject

internal class TypedAudioNodeMapper @Inject constructor() {
    operator fun invoke(
        fileNode: FileNode,
        duration: Int,
    ) = TypedAudioNode(
        fileNode = fileNode,
        duration = duration,
    )
}