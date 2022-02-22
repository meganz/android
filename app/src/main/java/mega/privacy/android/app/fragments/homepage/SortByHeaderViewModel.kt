package mega.privacy.android.app.fragments.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_MEDIA_DISCOVERY
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_VIEW_MODE
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.utils.Constants.EVENT_LIST_GRID_CHANGE
import mega.privacy.android.app.utils.Constants.EVENT_ORDER_CHANGE
import nz.mega.sdk.MegaApiJava.*
import javax.inject.Inject

/**
 * ViewModel in charge of manage actions from sub-headers in which view mode (list or grid)
 * and sort by options can be changed.
 */
@HiltViewModel
class SortByHeaderViewModel @Inject constructor(
    sortOrderManagement: SortOrderManagement
) : ViewModel() {

    /* Triple<Int, Int, Int>:
        - First: Cloud order
        - Second: Others order (Incoming root)
        - Third: Offline order */
    var order = Triple(
        sortOrderManagement.getOrderCloud(),
        sortOrderManagement.getOrderOthers(),
        sortOrderManagement.getOrderOffline()
    )
        private set
    var isList = true
        private set

    private val _showDialogEvent = MutableLiveData<Event<Unit>>()
    val showDialogEvent: LiveData<Event<Unit>> = _showDialogEvent

    private val _orderChangeEvent = MutableLiveData<Event<Triple<Int, Int, Int>>>()
    val orderChangeEvent: LiveData<Event<Triple<Int, Int, Int>>> = _orderChangeEvent

    private val _listGridChangeEvent = MutableLiveData<Event<Boolean>>()
    val listGridChangeEvent: LiveData<Event<Boolean>> = _listGridChangeEvent

    private val orderChangeObserver = Observer<Triple<Int, Int, Int>> {
        order = it
        _orderChangeEvent.value = Event(it)
    }

    private val listGridChangeObserver = Observer<Boolean> {
        isList = it
        _listGridChangeEvent.value = Event(it)
    }

    init {
        // Use "sticky" to observe the value set in ManagerActivity onCreate()
        @Suppress("UNCHECKED_CAST")
        LiveEventBus.get(EVENT_ORDER_CHANGE)
            .observeStickyForever(orderChangeObserver as Observer<Any>)
        LiveEventBus.get(
            EVENT_LIST_GRID_CHANGE,
            Boolean::class.java
        ).observeStickyForever(listGridChangeObserver)
    }

    fun showSortByDialog() {
        _showDialogEvent.value = Event(Unit)
    }

    fun switchListGrid() {
        LiveEventBus.get(EVENT_UPDATE_VIEW_MODE, Boolean::class.java).post(!isList)
    }

    /**
     * Enter media discovery view.
     */
    fun enterMediaDiscovery() {
        LiveEventBus.get(EVENT_SHOW_MEDIA_DISCOVERY, Unit::class.java).post(Unit)
    }

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
        @JvmStatic
        val orderNameMap = hashMapOf(
            ORDER_DEFAULT_ASC to R.string.sortby_name,
            ORDER_DEFAULT_DESC to R.string.sortby_name,
            ORDER_MODIFICATION_ASC to R.string.sortby_date,
            ORDER_MODIFICATION_DESC to R.string.sortby_date,
            ORDER_SIZE_ASC to R.string.sortby_size,
            ORDER_SIZE_DESC to R.string.sortby_size,
            ORDER_FAV_ASC to R.string.file_properties_favourite,
            ORDER_LABEL_ASC to R.string.title_label
        )
    }
}
