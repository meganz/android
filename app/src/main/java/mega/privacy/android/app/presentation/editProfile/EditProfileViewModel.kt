package mega.privacy.android.app.presentation.editProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserFirstName
import mega.privacy.android.domain.usecase.contact.GetCurrentUserLastName
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View model to handle load user avatar
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val getCurrentUserFirstName: GetCurrentUserFirstName,
    private val getCurrentUserLastName: GetCurrentUserLastName,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val hasOfflineFilesUseCase: HasOfflineFilesUseCase,
    private val ongoingTransfersExistUseCase: OngoingTransfersExistUseCase
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
            updateMyAvatarFile(getMyAvatarFileUseCase(isForceRefresh = false))
        }
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.w("Exception monitoring user updates: $it") }
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
        checkOfflineFiles()
        checkOngoingTransfers()
    }

    private fun getUserFistName(forceRefresh: Boolean) = viewModelScope.launch {
        runCatching { getCurrentUserFirstName(forceRefresh) }
            .onSuccess { firstName -> 
                _state.update { state -> state.copy(firstName = firstName ?: "") } 
            }
            .onFailure { Timber.w("Exception getting user first name.", it) }
    }

    private fun getUserLastName(forceRefresh: Boolean) = viewModelScope.launch {
        runCatching { getCurrentUserLastName(forceRefresh) }
            .onSuccess { lastName -> 
                _state.update { state -> state.copy(lastName = lastName ?: "") } 
            }
            .onFailure { Timber.w("Exception getting user last name.", it) }
    }

    /**
     * Check if there are offline files
     */
    fun checkOfflineFiles() = viewModelScope.launch {
        runCatching { hasOfflineFilesUseCase() }
            .onSuccess { _state.update { state -> state.copy(offlineFilesExist = it) } }
            .onFailure { Timber.w("Exception while checking offline files.", it) }
    }

    /**
     * Check if there are ongoing transfers
     */
    fun checkOngoingTransfers() = viewModelScope.launch {
        runCatching { ongoingTransfersExistUseCase() }
            .onSuccess { _state.update { state -> state.copy(transfersExist = it) } }
            .onFailure { Timber.w("Exception while checking ongoing transfers.", it) }
    }


    /**
     * Update my avatar file
     *
     * @param avatarFile
     */
    private suspend fun updateMyAvatarFile(avatarFile: File?) {
        val color = getMyAvatarColorUseCase()
        _state.update { state ->
            state.copy(
                avatarFile = avatarFile,
                avatarColor = color,
                avatarFileLastModified = avatarFile?.lastModified() ?: 0L
            )
        }
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