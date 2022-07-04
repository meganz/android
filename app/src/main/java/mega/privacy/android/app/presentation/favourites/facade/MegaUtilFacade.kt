package mega.privacy.android.app.presentation.favourites.facade

import android.content.Context
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.FetchNodeWrapper
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The implementation of MegaUtilWrapper
 */
class MegaUtilFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val fetchNode: FetchNodeWrapper,
): MegaUtilWrapper {

    override fun isOnline(context: Context) = Util.isOnline(context)

    override suspend fun availableOffline(context: Context, nodeId: Long) =
        fetchNode(nodeId)?.let { OfflineUtils.availableOffline(context, it) } ?: false

    override fun manageURLNode(context: Context, node: MegaNode) {
        MegaNodeUtil.manageURLNode(context, megaApi, node)
    }
}