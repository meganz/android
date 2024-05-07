package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx3.rxSingle
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.filenode.MoveNodeToRubbishBinUseCase
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for copying MegaNodes.
 *
 * @property megaApiGateway             MegaApiGateway instance to copy nodes.
 * @property getChatMessageUseCase      Required for getting chat [MegaNode]s.
 * @property copyNodeListUseCase        copy list of mega nodes
 * @property moveNodeToRubbishBinUseCase
 * @property ioDispatcher
 */
@Deprecated("Should be removed when ChatActivity is removed from the codebase")
class LegacyCopyNodeUseCase @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val getChatMessageUseCase: GetChatMessageUseCase,
    private val copyNodeListUseCase: CopyNodeListUseCase,
    private val moveNodeToRubbishBinUseCase: MoveNodeToRubbishBinUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Copies nodes.
     *
     * Should be removed when [mega.privacy.android.app.main.megachat.ChatActivity] is removed from the codebase
     *
     * @param nodes         List of MegaNodes to copy.
     * @param parentHandle  Parent MegaNode handle in which the nodes have to be copied.
     * @return Single with the [CopyRequestResult].
     */
    fun copy(nodes: List<MegaNode>, parentHandle: Long): Single<CopyRequestResult> =
        rxSingle { copyNodeListUseCase(nodes, parentHandle) }
}
