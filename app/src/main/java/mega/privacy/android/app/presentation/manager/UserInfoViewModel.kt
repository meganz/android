package mega.privacy.android.app.presentation.manager

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.manager.model.UserInfoUiState
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import javax.inject.Inject

@HiltViewModel
internal class UserInfoViewModel @Inject constructor(
    private val getCurrentUserFullName: GetCurrentUserFullName,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorUserUpdates: MonitorUserUpdates,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(UserInfoUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorUserUpdates()
                .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email }
                .collect {
                    when (it) {
                        UserChanges.Email -> getMyEmail()
                        UserChanges.Firstname,
                        UserChanges.Lastname,
                        -> getUserFullName()
                        else -> Unit
                    }
                }
        }
    }

    fun getUserInfo() {
        getUserFullName()
        getMyEmail()
    }

    private fun getMyEmail() {
        _state.update { it.copy(email = getCurrentUserEmail().orEmpty()) }
    }

    private fun getUserFullName() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    fullName = getCurrentUserFullName(
                        forceRefresh = true,
                        defaultFirstName = context.getString(R.string.first_name_text),
                        defaultLastName = context.getString(R.string.lastname_text),
                    )
                )
            }
        }
    }
}