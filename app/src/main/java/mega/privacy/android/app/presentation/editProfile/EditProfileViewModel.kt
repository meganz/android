package mega.privacy.android.app.presentation.editProfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.IoDispatcher
import mega.privacy.android.domain.usecase.GetMyAvatarColor
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import java.io.File
import javax.inject.Inject

/**
 * View model to handle load user avatar
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getMyAvatarFile: GetMyAvatarFile,
    private val getMyAvatarColor: GetMyAvatarColor,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
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
            updateMyAvatarFile(getMyAvatarFile())
        }
    }

    /**
     * Update my avatar file
     *
     * @param avatarFile
     */
    private suspend fun updateMyAvatarFile(avatarFile: File?) {
        _state.value = EditProfileState(avatarFile, getMyAvatarColor())
    }
}