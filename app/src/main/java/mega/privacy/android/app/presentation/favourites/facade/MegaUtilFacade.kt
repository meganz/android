package mega.privacy.android.app.presentation.favourites.facade

import android.content.Context
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The implementation of MegaUtilWrapper
 */
class MegaUtilFacade @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
): MegaUtilWrapper {

    override fun isOnline(context: Context) = Util.isOnline(context)

    override fun availableOffline(context: Context, node: MegaNode) =
        OfflineUtils.availableOffline(context, node)

    override fun manageURLNode(context: Context, node: MegaNode) {
        MegaNodeUtil.manageURLNode(context, megaApi, node)
    }
}