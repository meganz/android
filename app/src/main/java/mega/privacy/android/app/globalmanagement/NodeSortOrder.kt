package mega.privacy.android.app.globalmanagement

interface NodeSortOrder {
    /**
     * Sets all the available orders to the value by default
     */
    fun resetDefaults()
    fun getOrderCloud(): Int
    fun setOrderCloud(newOrderCloud: Int)
    fun getOrderOthers(): Int
    fun setOrderOthers(newOrderOthers: Int)
    fun getOrderCamera(): Int
    fun setOrderCamera(newOrderCamera: Int)
    fun getOrderOffline(): Int
    fun setOrderOffline(newOrderOffline: Int)
}