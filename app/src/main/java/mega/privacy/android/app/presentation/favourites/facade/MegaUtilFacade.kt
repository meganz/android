package mega.privacy.android.app.presentation.favourites.facade

import android.content.Context
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The implementation of MegaUtilWrapper
 */
class MegaUtilFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val fetchNode: FetchNodeWrapper,
    private val megaNodeUtilWrapper: MegaNodeUtilWrapper,
): MegaUtilWrapper {

    override fun isOnline(context: Context) = Util.isOnline(context)

    override suspend fun availableOffline(context: Context, nodeId: Long) =
        fetchNode(nodeId)?.let { OfflineUtils.availableOffline(context, it) } ?: false

    override fun manageURLNode(context: Context, node: MegaNode) {
        megaNodeUtilWrapper.manageURLNode(context, megaApi, node)
    }
}