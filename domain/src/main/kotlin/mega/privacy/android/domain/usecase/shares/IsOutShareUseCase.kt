package mega.privacy.android.domain.usecase.shares

import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import javax.inject.Inject

class IsOutShareUseCase @Inject constructor() {
    operator fun invoke(node: Node) = node is FolderNode && (node.isPendingShare || node.isShared)
}