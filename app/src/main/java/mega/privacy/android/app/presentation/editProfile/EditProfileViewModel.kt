package mega.privacy.android.app.presentation.editProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.contact.GetCurrentUserFirstName
import mega.privacy.android.domain.usecase.contact.GetCurrentUserLastName
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View model to handle load user avatar
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getMyAvatarFile: GetMyAvatarFile,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val getCurrentUserFirstName: GetCurrentUserFirstName,
    private val getCurrentUserLastName: GetCurrentUserLastName,
    private val monitorUserUpdates: MonitorUserUpdates,
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileState())

    /**
     * State of Edit Profile
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            monitorMyAvatarFile()
                .collect { avatarFile ->
                    updateMyAvatarFile(avatarFile)
                }
        }
        viewModelScope.launch(ioDispatcher) {
            updateMyAvatarFile(getMyAvatarFile(isForceRefresh = false))
        }
        viewModelScope.launch {
            monitorUserUpdates()
                .filter { it == UserChanges.Firstname || it == UserChanges.Lastname }
                .collect {
                    if (it == UserChanges.Firstname) {
                        getUserFistName(true)
                    } else {
                        getUserLastName(true)
                    }
                }
        }
        getUserFistName(false)
        getUserLastName(false)
    }

    private fun getUserFistName(forceRefresh: Boolean) = viewModelScope.launch {
        runCatching { getCurrentUserFirstName(forceRefresh) }
            .onSuccess { _state.update { state -> state.copy(firstName = it) } }
            .onFailure { Timber.w("Exception getting user first name.", it) }
    }

    private fun getUserLastName(forceRefresh: Boolean) = viewModelScope.launch {
        runCatching { getCurrentUserLastName(forceRefresh) }
            .onSuccess { _state.update { state -> state.copy(lastName = it) } }
            .onFailure { Timber.w("Exception getting user last name.", it) }
    }

    /**
     * Update my avatar file
     *
     * @param avatarFile
     */
    private suspend fun updateMyAvatarFile(avatarFile: File?) {
        _state.value = EditProfileState(avatarFile, getMyAvatarColorUseCase())
    }

    /**
     * Whether my avatar exists or not
     */
    fun existsMyAvatar(): Boolean = state.value.avatarFile?.exists() == true

    /**
     * Get first name
     *
     */
    fun getFirstName(): String = _state.value.firstName

    /**
     * Get last name
     *
     */
    fun getLastName(): String = _state.value.lastName
}