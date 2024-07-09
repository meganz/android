package mega.privacy.android.app.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Main use case to retrieve Mega Node information.
 *
 * @property context            Context needed to get offline node files.
 * @property megaApi            Mega API needed to call node information.
 * @property megaApiFolder      Mega API folder needed to authorize node.
 * @property megaChatApi        Mega Chat API needed to get nodes from chat messages.
 * @property databaseHandler    Database Handler needed to retrieve offline nodes.
 */
class GetNodeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    @MegaApiFolder private val megaApiFolder: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val databaseHandler: LegacyDatabaseHandler,
) {

    /**
     * Get a MegaNode given a Node Handle.
     *
     * @param nodeHandle    Mega node handle
     * @return              Single with Mega Node
     */
    fun get(nodeHandle: Long): Single<MegaNode> =
        Single.fromCallable { nodeHandle.getMegaNode() ?: throw NullPointerException() }

    /**
     * Get a MegaNode given a Long handle in a synchronous way.
     * This will also authorize the Node if required.
     *
     * @return  MegaNode
     */
    private fun Long.getMegaNode(): MegaNode? =
        megaApi.getNodeByHandle(this)
            ?: megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(this))
}
