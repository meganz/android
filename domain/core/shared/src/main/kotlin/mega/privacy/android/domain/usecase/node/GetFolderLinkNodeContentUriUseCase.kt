package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiFolderUseCase
import mega.privacy.android.domain.usecase.GetLocalFolderLinkFromMegaApiUseCase
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiFolderHttpServerStartUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import javax.inject.Inject

/**
 * The Use case is used to get the node content uri of a folder link
 */
class GetFolderLinkNodeContentUriUseCase @Inject constructor(
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerIsRunningUseCase: MegaApiFolderHttpServerIsRunningUseCase,
    private val megaApiFolderHttpServerStartUseCase: MegaApiFolderHttpServerStartUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val getLocalFolderLinkFromMegaApiUseCase: GetLocalFolderLinkFromMegaApiUseCase,
    private val getLocalFolderLinkFromMegaApiFolderUseCase: GetLocalFolderLinkFromMegaApiFolderUseCase,
) {
    /**
     * Invoke
     *
     * @param fileNode [TypedFileNode]
     * @return [NodeContentUri]
     *
     */
    suspend operator fun invoke(fileNode: TypedFileNode): NodeContentUri {
        val shouldStopHttpSever: Boolean
        return if (hasCredentialsUseCase()) {
            shouldStopHttpSever = if (megaApiHttpServerIsRunningUseCase() == 0) {
                megaApiHttpServerStartUseCase()
                true
            } else false
            getLocalFolderLinkFromMegaApiUseCase(fileNode.id.longValue)
        } else {
            shouldStopHttpSever = if (megaApiFolderHttpServerIsRunningUseCase() == 0) {
                megaApiFolderHttpServerStartUseCase()
                true
            } else false
            getLocalFolderLinkFromMegaApiFolderUseCase(fileNode.id.longValue)
        }?.let { url ->
            NodeContentUri.RemoteContentUri(url, shouldStopHttpSever)
        } ?: run {
            getNodeContentUriUseCase(fileNode)
        }
    }
}