package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get an array with all nested parent folders of this node, from deepest to less deep.
 */
class GetNestedParentFoldersUseCase @Inject constructor(
    private val getParentNodeUseCase: GetParentNodeUseCase,

    ) {
    /**
     * returns an array with all nested parent folders of this node, from deepest to less deep.
     * @param node the [Node] from which its parents will be returned
     */
    suspend operator fun invoke(node: Node): List<Node> {
        val nodes = ArrayList<Node>()
        var nodeToCheck = node
        while (true) {
            nodeToCheck = getParentNodeUseCase(nodeToCheck.id) ?: break
            nodes.add(nodeToCheck)
        }
        return nodes.reversed()
    }
}

/**
 * Util extension to join the returned value of this use-case as a single string path.
 */
fun List<Node>.joinAsPath(): String =
    this.map { it.name }
        .filterNot { it == File.separator }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(
            separator = File.separator,
            prefix = File.separator,
            postfix = File.separator,
        ) {
            it.removePrefix(File.separator).removeSuffix(File.separator)
        } ?: File.separator