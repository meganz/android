package mega.privacy.android.app.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.usecase.exception.NotEnoughQuotaMegaException
import mega.privacy.android.app.usecase.exception.QuotaExceededMegaException
import mega.privacy.android.app.usecase.exception.toMegaException
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Copy node list use case
 *
 * @property megaApi
 * @property megaApiFolder
 * @property ioDispatcher
 * @constructor Create empty Copy node list use case
 */
class CopyNodeListUseCase @Inject constructor(
    private val megaApi: MegaApiGateway,
    private val megaApiFolder: MegaApiFolderGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param nodes
     * @param parentHandle
     */
    suspend operator fun invoke(
        nodes: List<MegaNode>,
        parentHandle: Long,
    ) = withContext(ioDispatcher) {
        val parentNode = megaApi.getMegaNodeByHandle(parentHandle)
            ?: getSharedNode(parentHandle)
            ?: throw MegaNodeException.ParentDoesNotExistException()

        var errorCount = 0

        for (node in nodes) {
            runCatching { copy(node, parentNode) }
                .onFailure {
                    if (shouldThrowError(it)) throw it
                    errorCount++
                }
        }

        CopyRequestResult(
            nodes.size,
            errorCount
        ).also { resetAccountDetailsIfNeeded(it) }
    }

    private suspend fun getSharedNode(parentHandle: Long) =
        megaApiFolder.getMegaNodeByHandle(parentHandle)
            ?.let { megaApiFolder.authorizeNode(it) }

    private suspend fun copy(
        node: MegaNode,
        parentNode: MegaNode,
    ) {

        val parentIsForeign = megaApi.isForeignNode(parentNode.handle)
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(onRequestFinish = { _, error ->
                when {
                    error.errorCode == MegaError.API_OK -> continuation.resumeWith(
                        Result.success(
                            Unit
                        )
                    )

                    error.errorCode == MegaError.API_EOVERQUOTA && parentIsForeign -> continuation.resumeWith(
                        Result.failure(ForeignNodeException())
                    )

                    else -> continuation.resumeWith(Result.failure(error.toMegaException()))
                }
            })

            megaApi.copyNode(
                nodeToCopy = node,
                newNodeParent = parentNode,
                newNodeName = null,
                listener = listener
            )
        }

    }

    private fun shouldThrowError(throwable: Throwable): Boolean =
        when (throwable) {
            is QuotaExceededMegaException, is NotEnoughQuotaMegaException, is ForeignNodeException -> true
            else -> false
        }

    private suspend fun resetAccountDetailsIfNeeded(result: CopyRequestResult) {
        if (result.successCount > 0) {
            accountRepository.resetAccountDetailsTimeStamp()
        }
    }
}