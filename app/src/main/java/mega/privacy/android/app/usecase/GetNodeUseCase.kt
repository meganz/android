package mega.privacy.android.app.usecase

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.StringUtils.toThrowable
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Use case for getting MegaNodes.
 *
 * @property megaApi MegaApiAndroid instance to move nodes..
 */
class GetNodeUseCase @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) {

    /**
     * Gets a MegaNode if exists.
     *
     * @param handle    Handle of the node to get.
     * @return The MegaNode if exists.
     */
    fun get(handle: Long): Single<MegaNode> =
        Single.create {emitter ->
            val node = megaApi.getNodeByHandle(handle)

            if (node == null) {
                emitter.onError("Node is null".toThrowable())
            } else {
                emitter.onSuccess(node)
            }
        }
}