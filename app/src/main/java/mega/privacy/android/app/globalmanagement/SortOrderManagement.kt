package mega.privacy.android.app.globalmanagement

import mega.privacy.android.app.DatabaseHandler
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SortOrderManagement @Inject constructor(
    private val dbH: DatabaseHandler
) {

    private var orderCloud: Int = ORDER_DEFAULT_ASC
    private var orderContacts: Int = ORDER_DEFAULT_ASC
    private var orderOthers: Int = ORDER_DEFAULT_ASC
    private var orderCamera: Int = ORDER_MODIFICATION_DESC

    init {
        initialize()
    }

    /**
     * Initializes all the available orders getting their values from database if exist.
     */
    fun initialize() {
        resetDefaults()

        dbH.preferences?.apply {
            preferredSortCloud?.toInt()?.let { orderCloud = it }
            preferredSortContacts?.toInt()?.let { orderContacts = it }
            preferredSortCameraUpload?.toInt()?.let { orderCamera = it }
            preferredSortOthers?.toInt()?.let { orderOthers = it }
        }
    }

    /**
     * Sets all the available orders to the value by default
     */
    fun resetDefaults() {
        orderCloud = ORDER_DEFAULT_ASC
        orderContacts = ORDER_DEFAULT_ASC
        orderOthers = ORDER_DEFAULT_ASC
        orderCamera = ORDER_MODIFICATION_DESC
    }

    fun getOrderCloud(): Int =
        orderCloud

    fun setOrderCloud(newOrderCloud: Int) {
        orderCloud = newOrderCloud
        dbH.setPreferredSortCloud(orderCloud.toString())
    }

    fun getOrderContacts(): Int =
        orderContacts

    fun setOrderContacts(newOrderContacts: Int) {
        orderContacts = newOrderContacts
        dbH.setPreferredSortContacts(orderContacts.toString())
    }

    fun getOrderCamera(): Int =
        orderCamera

    fun setOrderCamera(newOrderCamera: Int) {
        orderCamera = newOrderCamera
        dbH.setPreferredSortCameraUpload(orderCamera.toString())
    }

    fun getOrderOthers(): Int =
        orderOthers

    fun setOrderOthers(newOrderOthers: Int) {
        orderOthers = newOrderOthers
        dbH.setPreferredSortOthers(orderOthers.toString())
    }
}
