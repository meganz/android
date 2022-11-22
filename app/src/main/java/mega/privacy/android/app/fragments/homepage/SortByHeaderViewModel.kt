package mega.privacy.android.app.fragments.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_MEDIA_DISCOVERY
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_VIEW_MODE
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EVENT_LIST_GRID_CHANGE
import mega.privacy.android.app.utils.Constants.EVENT_ORDER_CHANGE
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCameraSortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.SetCameraSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.SetOfflineSortOrder
import mega.privacy.android.domain.usecase.SetOthersSortOrder
import javax.inject.Inject

/**
 * ViewModel in charge of manage actions from sub-headers in which view mode (list or grid)
 * and sort by options can be changed.
 */
@HiltViewModel
class SortByHeaderViewModel @Inject constructor(
    private val getCameraSortOrder: GetCameraSortOrder,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val getOfflineSortOrder: GetOfflineSortOrder,
    private val setCameraSortOrder: SetCameraSortOrder,
    private val setCloudSortOrder: SetCloudSortOrder,
    private val setOthersSortOrder: SetOthersSortOrder,
    private val setOfflineSortOrder: SetOfflineSortOrder,
) : ViewModel() {

    private val ordersDefault = SortOrder.ORDER_DEFAULT_ASC
    private val cameraOrderDefault = SortOrder.ORDER_MODIFICATION_DESC

    private val _cameraSortOrder = MutableStateFlow(cameraOrderDefault)

    /**
     * Camera Sort Order
     */
    val cameraSortOrder: StateFlow<SortOrder> = _cameraSortOrder

    private val _cloudSortOrder = MutableStateFlow(ordersDefault)

    /**
     * Cloud Sort Order
     */
    val cloudSortOrder: StateFlow<SortOrder> = _cloudSortOrder

    private val _othersSortOrder = MutableStateFlow(ordersDefault)

    /**
     * Others Sort Order
     */
    val othersSortOrder: StateFlow<SortOrder> = _othersSortOrder

    private val _offlineSortOrder = MutableStateFlow(ordersDefault)

    /**
     * Offline Sort Order
     */
    val offlineSortOrder: StateFlow<SortOrder> = _offlineSortOrder

    /**
     * Boolean for List or Grid
     */
    var isList = true
        private set

    private val _showDialogEvent = MutableLiveData<Event<Unit>>()

    /**
     * Show Dialog Event
     */
    val showDialogEvent: LiveData<Event<Unit>> = _showDialogEvent

    private val _listGridChangeEvent = MutableLiveData<Event<Boolean>>()

    /**
     * List/Grid Change Event
     */
    val listGridChangeEvent: LiveData<Event<Boolean>> = _listGridChangeEvent

    private val listGridChangeObserver = Observer<Boolean> {
        isList = it
        _listGridChangeEvent.value = Event(it)
    }

    /** Triple<SortOrder, SortOrder, SortOrder>:
    - First: Cloud order
    - Second: Others order (Incoming root)
    - Third: Offline order */
    var order = Triple(
        _cloudSortOrder.value,
        _othersSortOrder.value,
        _offlineSortOrder.value
    )
        private set


    private val _orderChangeEvent =
        MutableLiveData<Event<Triple<SortOrder, SortOrder, SortOrder>>>()

    /**
     * Order Change Event
     */
    val orderChangeEvent: LiveData<Event<Triple<SortOrder, SortOrder, SortOrder>>> =
        _orderChangeEvent

    private val orderChangeObserver = Observer<Triple<SortOrder, SortOrder, SortOrder>> {
        order = it
        _orderChangeEvent.value = Event(it)
    }

    private val _oldOrder = MutableStateFlow<SortOrder?>(null)

    init {
        // Use "sticky" to observe the value set in ManagerActivity onCreate()
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_ORDER_CHANGE)
            .observeStickyForever(orderChangeObserver as Observer<Any>)
        LiveEventBus.get(
            EVENT_LIST_GRID_CHANGE,
            Boolean::class.java
        ).observeStickyForever(listGridChangeObserver)

        viewModelScope.launch {
            _cameraSortOrder.value = getCameraSortOrder()
            _cloudSortOrder.value = getCloudSortOrder()
            _othersSortOrder.value = getOthersSortOrder()
            _offlineSortOrder.value = getOfflineSortOrder()
            setOldOrder()
        }
    }


    /**
     * Previously Selected Order
     */
    val oldOrder: StateFlow<SortOrder?> = _oldOrder

    private var orderType: Int = Constants.ORDER_CLOUD

    /**
     * Set Order Type
     */
    fun setOrderType(orderType: Int) {
        this.orderType = orderType
    }

    /**
     * Set Old Order
     */
    fun setOldOrder() {
        val order = when (this.orderType) {
            Constants.ORDER_CLOUD -> cloudSortOrder.value
            Constants.ORDER_CAMERA -> cameraSortOrder.value
            Constants.ORDER_OTHERS -> othersSortOrder.value
            Constants.ORDER_OFFLINE -> offlineSortOrder.value
            Constants.ORDER_FAVOURITES -> cloudSortOrder.value
            else -> SortOrder.ORDER_DEFAULT_ASC
        }
        _oldOrder.value = order
    }

    /**
     * Set Camera Sort Order
     */
    suspend fun setOrderCamera(order: SortOrder) = viewModelScope.launch {
        _cameraSortOrder.value = order
        setCameraSortOrder(order)
    }

    /**
     * Set Cloud Sort Order
     */
    suspend fun setOrderCloud(order: SortOrder) = viewModelScope.launch {
        _cloudSortOrder.value = order
        setCloudSortOrder(order)
    }

    /**
     * Set Others Sort Order
     */
    suspend fun setOrderOthers(order: SortOrder) = viewModelScope.launch {
        _othersSortOrder.value = order
        setOthersSortOrder(order)
    }

    /**
     * Set Offline Sort Order
     */
    suspend fun setOrderOffline(order: SortOrder) = viewModelScope.launch {
        _offlineSortOrder.value = order
        setOfflineSortOrder(order)
    }

    /**
     * Show Sort by Dialog
     */
    fun showSortByDialog() {
        _showDialogEvent.value = Event(Unit)
    }

    /**
     * Switch List/Grid
     */
    fun switchListGrid() {
        LiveEventBus.get(EVENT_UPDATE_VIEW_MODE, Boolean::class.java).post(!isList)
    }

    /**
     * Enter media discovery view.
     */
    fun enterMediaDiscovery() {
        LiveEventBus.get(EVENT_SHOW_MEDIA_DISCOVERY, Unit::class.java).post(Unit)
    }

    /**
     * onCleared()
     */
    override fun onCleared() {
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_ORDER_CHANGE)
            .removeObserver(orderChangeObserver as Observer<Any>)
        LiveEventBus.get(
            EVENT_LIST_GRID_CHANGE,
            Boolean::class.java
        ).removeObserver(listGridChangeObserver)
    }

    companion object {
        /**
         * SortOrder to Display Name Map
         */
        @JvmStatic
        val orderNameMap = hashMapOf(
            SortOrder.ORDER_NONE to R.string.sortby_name,
            SortOrder.ORDER_DEFAULT_ASC to R.string.sortby_name,
            SortOrder.ORDER_DEFAULT_DESC to R.string.sortby_name,
            SortOrder.ORDER_MODIFICATION_ASC to R.string.sortby_date,
            SortOrder.ORDER_MODIFICATION_DESC to R.string.sortby_date,
            SortOrder.ORDER_SIZE_ASC to R.string.sortby_size,
            SortOrder.ORDER_SIZE_DESC to R.string.sortby_size,
            SortOrder.ORDER_FAV_ASC to R.string.file_properties_favourite,
            SortOrder.ORDER_LABEL_ASC to R.string.title_label
        )
    }
}
