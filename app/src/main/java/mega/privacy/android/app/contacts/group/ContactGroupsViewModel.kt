package mega.privacy.android.app.contacts.group

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.usecase.GetContactGroupsUseCase
import mega.privacy.android.app.utils.notifyObserver

class ContactGroupsViewModel @ViewModelInject constructor(
    getContactGroupsUseCase: GetContactGroupsUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactGroupsViewModel"
    }

    private val groups: MutableLiveData<List<ContactGroupItem>> = MutableLiveData()
    private var queryString: String? = null

    init {
        getContactGroupsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    groups.value = items.toList()
                },
                onError = { error ->
                    Log.e(TAG, error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun getGroups(): LiveData<List<ContactGroupItem>> =
        groups.map { items ->
            if (!queryString.isNullOrBlank()) {
                items.filter { it.title.contains(queryString!!, true) }
            } else {
                items
            }
        }

    fun setQuery(query: String?) {
        queryString = query
        groups.notifyObserver()
    }
}
