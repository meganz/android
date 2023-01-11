package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.FileNode
import java.io.File

fun interface GetLocalFileForNode {
    suspend operator fun invoke(fileNode: FileNode): File?
}