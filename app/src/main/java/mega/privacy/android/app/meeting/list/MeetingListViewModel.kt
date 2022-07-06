package mega.privacy.android.app.meeting.list

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase
import mega.privacy.android.app.contacts.usecase.GetContactsUseCase
import mega.privacy.android.app.contacts.usecase.RemoveContactUseCase
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.call.StartCallUseCase
import mega.privacy.android.app.usecase.meeting.GetMeetingListUseCase
import mega.privacy.android.app.utils.RxUtil.debounceImmediate
import mega.privacy.android.app.utils.notifyObserver
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val getMeetingListUseCase: GetMeetingListUseCase,
) : BaseRxViewModel() {

    companion object {
        private const val REQUEST_TIMEOUT_IN_MS = 100L
    }

    private var queryString: String? = null
    private val contacts: MutableLiveData<List<ContactItem.Data>> = MutableLiveData()

    init {
        retrieveContacts()
    }

    private fun retrieveContacts() {
        getMeetingListUseCase.get()
            .debounceImmediate(REQUEST_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
//                    contacts.value = items.toList()
                },
                onError = Timber::e
            )
            .addTo(composite)
    }

    fun setQuery(query: String?) {
        queryString = query
        contacts.notifyObserver()
    }
}
