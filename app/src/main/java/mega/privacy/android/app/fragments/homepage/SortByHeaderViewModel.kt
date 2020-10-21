package mega.privacy.android.app.fragments.homepage

import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaApiJava.*

class SortByHeaderViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var order = ORDER_DEFAULT_ASC
        private set
    var isList = true
        private set

    private val _showDialogEvent = MutableLiveData<Event<Unit>>()
    val showDialogEvent: LiveData<Event<Unit>> = _showDialogEvent

    private val _orderChangeEvent = MutableLiveData<Event<Int>>()
    val orderChangeEvent: LiveData<Event<Int>> = _orderChangeEvent

    private val _listGridChangeEvent = MutableLiveData<Event<Boolean>>()
    val listGridChangeEvent: LiveData<Event<Boolean>> = _listGridChangeEvent

    private val orderChangeObserver = Observer<Int> {
        order = it
        _orderChangeEvent.value = Event(it)
    }

    private val listGridChangeObserver = Observer<Boolean> {
        isList = it
        _listGridChangeEvent.value = Event(it)
    }

    init {
        orderChange.observeForever(orderChangeObserver)
        listGridChange.observeForever(listGridChangeObserver)
    }

    fun showSortByDialog() {
        _showDialogEvent.value = Event(Unit)
    }

    fun switchListGrid() {
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_VIEW)
        intent.putExtra(Constants.INTENT_EXTRA_KEY_IS_LIST, !isList)
        context.sendBroadcast(intent)
    }

    override fun onCleared() {
        orderChange.removeObserver(orderChangeObserver)
        listGridChange.removeObserver(listGridChangeObserver)
    }

    companion object {
        val orderNameMap = hashMapOf(
            ORDER_DEFAULT_ASC to R.string.sortby_name,
            ORDER_DEFAULT_DESC to R.string.sortby_name,
            ORDER_MODIFICATION_ASC to R.string.sortby_date,
            ORDER_MODIFICATION_DESC to R.string.sortby_date,
            ORDER_SIZE_ASC to R.string.sortby_size,
            ORDER_SIZE_DESC to R.string.sortby_size
        )
    }
}
