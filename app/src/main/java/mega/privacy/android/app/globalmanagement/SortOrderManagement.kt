package mega.privacy.android.app.globalmanagement

import mega.privacy.android.app.DatabaseHandler
import nz.mega.sdk.MegaApiJava.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SortOrderManagement @Inject constructor(
    private val dbH: DatabaseHandler
) {

    private var orderCloud: Int = ORDER_DEFAULT_ASC
    private var orderOthers: Int = ORDER_DEFAULT_ASC
    private var orderCamera: Int = ORDER_MODIFICATION_DESC

    /* Same order than orderCloud except when it is ORDER_LABEL_ASC or ORDER_FAV_ASC, then it keeps
    the previous selected order */
    private var orderOffline: Int = ORDER_DEFAULT_ASC

    init {
        dbH.preferences?.apply {
            preferredSortCloud?.toInt()?.let {
                orderCloud = it

                if (canUpdateOfflineOrder(it)) {
                    orderOffline = it
                }
            }

            preferredSortOthers?.toInt()?.let { orderOthers = it }
            preferredSortCameraUpload?.toInt()?.let { orderCamera = it }
        }
    }

    /**
     * Sets all the available orders to the value by default
     */
    fun resetDefaults() {
        orderCloud = ORDER_DEFAULT_ASC
        orderOthers = ORDER_DEFAULT_ASC
        orderCamera = ORDER_MODIFICATION_DESC
        orderOffline = ORDER_DEFAULT_ASC
    }

    fun getOrderCloud(): Int = orderCloud

    fun setOrderCloud(newOrderCloud: Int) {
        orderCloud = newOrderCloud
        dbH.setPreferredSortCloud(orderCloud.toString())

        if (canUpdateOfflineOrder(newOrderCloud)) {
            orderOffline = newOrderCloud
        }
    }

    fun getOrderOthers(): Int = orderOthers

    fun setOrderOthers(newOrderOthers: Int) {
        orderOthers = newOrderOthers
        dbH.setPreferredSortOthers(orderOthers.toString())
    }

    fun getOrderCamera(): Int = orderCamera

    fun setOrderCamera(newOrderCamera: Int) {
        orderCamera = newOrderCamera
        dbH.setPreferredSortCameraUpload(orderCamera.toString())
    }

    fun getOrderOffline(): Int = orderOffline

    fun setOrderOffline(newOrderOffline: Int) {
        orderOffline = newOrderOffline
        orderCloud = newOrderOffline
        dbH.setPreferredSortCloud(orderCloud.toString())
    }

    /**
     * Checks if can update offline order.
     * Since offline nodes cannot be ordered by labels and favorites, the offline order will be only
     * updated if the new order is different than ORDER_LABEL_ASC and ORDER_FAV_ASC.
     *
     * @param newOrder New order chosen.
     * @return True if can update offline order, false otherwise.
     */
    private fun canUpdateOfflineOrder(newOrder: Int): Boolean =
        newOrder != ORDER_LABEL_ASC && newOrder != ORDER_FAV_ASC
}
