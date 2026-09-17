package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaDownloadItemList
import nz.mega.sdk.MegaNode

/**
 * In-memory stub of [MegaDownloadItemList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Items added are kept in memory
 * and readable back through the same getters the real list exposes.
 */
class StubMegaDownloadItemList : MegaDownloadItemList(0, false) {

    /**
     * One item of a batch download, as given to [add].
     *
     * @property node the node to download
     * @property localPath the item's destination
     * @property customName the local name to store the node under, null to use the node's own
     * @property appData the app data to save in the transfers this item produces
     * @property startFirst whether the item goes on top of the download queue
     * @property undelete whether the item is a request for a completely deleted node
     */
    data class Item(
        val node: MegaNode?,
        val localPath: String?,
        val customName: String? = null,
        val appData: String? = null,
        val startFirst: Boolean = false,
        val undelete: Boolean = false,
    )

    private val addedItems = mutableListOf<Item>()

    /**
     * The items added so far, in order.
     */
    val items: List<Item> get() = addedItems.toList()

    override fun delete() = Unit

    override fun add(
        node: MegaNode?,
        localPath: String?,
        customName: String?,
        appData: String?,
        startFirst: Boolean,
        undelete: Boolean,
    ) {
        addedItems += Item(node, localPath, customName, appData, startFirst, undelete)
    }

    override fun add(
        node: MegaNode?,
        localPath: String?,
        customName: String?,
        appData: String?,
        startFirst: Boolean,
    ) = add(node, localPath, customName, appData, startFirst, false)

    override fun add(
        node: MegaNode?,
        localPath: String?,
        customName: String?,
        appData: String?,
    ) = add(node, localPath, customName, appData, false, false)

    override fun add(
        node: MegaNode?,
        localPath: String?,
        customName: String?,
    ) = add(node, localPath, customName, null, false, false)

    override fun add(
        node: MegaNode?,
        localPath: String?,
    ) = add(node, localPath, null, null, false, false)

    override fun size(): Int = addedItems.size

    override fun getNode(p0: Int): MegaNode? = addedItems.getOrNull(p0)?.node

    override fun getLocalPath(p0: Int): String? = addedItems.getOrNull(p0)?.localPath

    override fun getCustomName(p0: Int): String? = addedItems.getOrNull(p0)?.customName

    override fun getAppData(p0: Int): String? = addedItems.getOrNull(p0)?.appData

    override fun getStartFirst(p0: Int): Boolean = addedItems.getOrNull(p0)?.startFirst == true

    override fun getUndelete(p0: Int): Boolean = addedItems.getOrNull(p0)?.undelete == true
}
