package mega.privacy.android.domain.usecase.file

import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

class IsNodeOpenableTextFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {
    operator fun invoke(node: FileNode) = fileSystemRepository.isNodeOpenableTextFile(node)
}