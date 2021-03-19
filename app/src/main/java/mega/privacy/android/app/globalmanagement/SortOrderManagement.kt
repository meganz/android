package mega.privacy.android.app.globalmanagement

import mega.privacy.android.app.DatabaseHandler
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SortOrderManagement @Inject constructor(private val dbH: DatabaseHandler) {

    private var orderCloud: Int = ORDER_DEFAULT_ASC
    private var orderContacts: Int = ORDER_DEFAULT_ASC
    private var orderOthers: Int = ORDER_DEFAULT_ASC
    private var orderCamera: Int = ORDER_MODIFICATION_DESC

    init {
        initInstance()
    }

    /**
     * Initializes all the available orders getting their values from database if exist.
     */
    private fun initInstance() {
        val prefs = dbH.preferences

        if (prefs != null) {
            if (prefs.preferredSortCloud != null) {
                orderCloud = prefs.preferredSortCloud.toInt()
            }

            if (prefs.preferredSortContacts != null) {
                orderContacts = prefs.preferredSortContacts.toInt()
            }

            if (prefs.preferredSortCameraUpload != null) {
                orderCamera = prefs.preferredSortCameraUpload.toInt()
            }

            if (prefs.preferredSortOthers != null) {
                orderOthers = prefs.preferredSortOthers.toInt()
            }
        }
    }

    /**
     * Sets all the available orders to the value by default
     * and initializes them getting their values from database if exist.
     */
    fun createInstance() {
        destroyInstance()
        initInstance()
    }

    /**
     * Sets all the available orders to the value by default.
     */
    fun destroyInstance() {
        orderCloud = ORDER_DEFAULT_ASC
        orderContacts = ORDER_DEFAULT_ASC
        orderOthers = ORDER_DEFAULT_ASC
        orderCamera = ORDER_MODIFICATION_DESC
    }

    fun getOrderCloud(): Int {
        return orderCloud
    }

    fun setOrderCloud(newOrderCloud: Int) {
        orderCloud = newOrderCloud
        dbH.setPreferredSortCloud(orderCloud.toString())
    }

    fun getOrderContacts(): Int {
        return orderContacts
    }

    fun setOrderContacts(newOrderContacts: Int) {
        orderContacts = newOrderContacts
        dbH.setPreferredSortContacts(orderContacts.toString())
    }

    fun getOrderCamera(): Int {
        return orderCamera
    }

    fun setOrderCamera(newOrderCamera: Int) {
        orderCamera = newOrderCamera
        dbH.setPreferredSortCameraUpload(orderCamera.toString())
    }

    fun getOrderOthers(): Int {
        return orderOthers
    }

    fun setOrderOthers(newOrderOthers: Int) {
        orderOthers = newOrderOthers
        dbH.setPreferredSortOthers(orderOthers.toString())
    }
}
