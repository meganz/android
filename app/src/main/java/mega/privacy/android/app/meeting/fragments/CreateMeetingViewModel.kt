package mega.privacy.android.app.meeting.fragments

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.R
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CreateMeetingViewModel @Inject constructor(
    private val getMyFullNameUseCase: GetMyFullNameUseCase,
) : ViewModel() {

    var meetingName: MutableLiveData<String> = MutableLiveData<String>()

    /**
     * Full name of the current user, null until it has been loaded
     */
    val myFullName: StateFlow<String?> by lazy(LazyThreadSafetyMode.NONE) {
        flow { emit(getMyFullNameUseCase()) }
            .catch { Timber.e(it, "Failed to get my full name") }
            .asUiStateFlow(viewModelScope, null)
    }

    /**
     * Sets the default value of meetingName
     */
    fun initMeetingName() {
        meetingName.value = ""
    }

    /**
     * Sets value of meetingName
     */
    fun updateMeetingName(newName: String) {
        meetingName.value = newName
    }

    /**
     * Sets the default value of hint
     *
     * @return the string of default meetingName
     */
    fun initHintMeetingName(context: Context): String {
        return context.getString(
            R.string.type_meeting_name, myFullName.value
        )
    }
}
