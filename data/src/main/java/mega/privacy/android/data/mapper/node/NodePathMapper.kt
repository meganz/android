package mega.privacy.android.data.mapper.node

import mega.privacy.android.data.wrapper.StringWrapper
import nz.mega.sdk.MegaNode
import java.io.File
import javax.inject.Inject

class NodePathMapper @Inject constructor(
    private val stringWrapper: StringWrapper,
) {

    suspend operator fun invoke(
        node: MegaNode,
        rootParent: MegaNode,
        getRootNode: suspend () -> MegaNode?,
        getRubbishBinNode: suspend () -> MegaNode?,
        nodePath: String,
    ) = when {
        node.isInShare || rootParent.isInShare ->
            stringWrapper.getTitleIncomingSharesExplorer() + File.separator +
                    nodePath.substring(nodePath.indexOf(":") + 1)

        rootParent.handle == getRootNode()?.handle ->
            stringWrapper.getCloudDriveSection() + nodePath

        rootParent.handle == getRubbishBinNode()?.handle -> {
            stringWrapper.getRubbishBinSection() +
                    nodePath.replace("${File.separator}${File.separator}bin", "")
        }

        else -> nodePath
    }.removeSuffix(File.separator)
}