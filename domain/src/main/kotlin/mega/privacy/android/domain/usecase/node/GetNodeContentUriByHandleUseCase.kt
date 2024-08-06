package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetAlbumPhotoFileUrlByNodeIdUseCase
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import javax.inject.Inject

/**
 * Get NodeContentUri By Id Use Case
 */
class GetNodeContentUriByHandleUseCase @Inject constructor(
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
    private val getAlbumPhotoFileUrlByNodeIdUseCase: GetAlbumPhotoFileUrlByNodeIdUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val addNodeType: AddNodeType,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(handle: Long): NodeContentUri {
        val shouldStopHttpSever = if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            true
        } else false
        return getFileUrlByNodeHandleUseCase(handle)?.let { url ->
            NodeContentUri.RemoteContentUri(url, shouldStopHttpSever)
        } ?: run {
            getAlbumPhotoFileUrlByNodeIdUseCase(NodeId(handle))?.let { url ->
                NodeContentUri.RemoteContentUri(url, shouldStopHttpSever)
            } ?: run {
                if (hasCredentialsUseCase()) {
                    getLocalFolderLinkFromMegaApiUseCase(handle)
                } else {
                    getLocalFolderLinkFromMegaApiFolderUseCase(handle)
                }?.let { url ->
                    NodeContentUri.RemoteContentUri(url, shouldStopHttpSever)
                } ?: run {
                    getNodeByHandleUseCase(handle)?.let { node ->
                        val typedNode = addNodeType(node)
                        if (typedNode is TypedFileNode) {
                            getNodeContentUriUseCase(typedNode)
                        } else {
                            throw IllegalStateException("node is not a file")
                        }
                    } ?: throw IllegalStateException("cannot find node")
                }
            }
        }
    }
}