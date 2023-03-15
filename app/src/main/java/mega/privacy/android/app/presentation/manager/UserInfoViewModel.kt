package mega.privacy.android.app.presentation.manager

import android.content.Context
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.manager.model.UserInfoUiState
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetMyAvatarColor
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.UpdateMyAvatarWithNewEmail
import mega.privacy.android.domain.usecase.contact.GetContactEmail
import mega.privacy.android.domain.usecase.contact.GetCurrentUserAliases
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import mega.privacy.android.domain.usecase.contact.GetUserLastName
import mega.privacy.android.domain.usecase.contact.ReloadContactDatabase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class UserInfoViewModel @Inject constructor(
    private val getCurrentUserFullName: GetCurrentUserFullName,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val updateMyAvatarWithNewEmail: UpdateMyAvatarWithNewEmail,
    private val monitorContactUpdates: MonitorContactUpdates,
    private val getUserFirstName: GetUserFirstName,
    private val getUserLastName: GetUserLastName,
    private val getCurrentUserAliases: GetCurrentUserAliases,
    private val reloadContactDatabase: ReloadContactDatabase,
    private val getContactEmail: GetContactEmail,
    private val getMyAvatarFile: GetMyAvatarFile,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val getMyAvatarColor: GetMyAvatarColor,
    private val avatarContentMapper: AvatarContentMapper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(UserInfoUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.e(it) }
                .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email }
                .collect {
                    when (it) {
                        UserChanges.Email -> handleEmailChange()
                        UserChanges.Firstname,
                        UserChanges.Lastname,
                        -> {
                            getUserFullName()
                            getUserAvatarOrDefault(isForceRefresh = false)
                        }
                        else -> Unit
                    }
                }
        }
        viewModelScope.launch {
            monitorContactUpdates()
                .catch { Timber.e(it) }
                .collect {
                    handleOtherUsersUpdate(it)
                }
        }
        viewModelScope.launch {
            monitorMyAvatarFile()
                .catch { Timber.e(it) }
                .collect {
                    getUserAvatarOrDefault(isForceRefresh = false)
                }
        }
    }

    private suspend fun getUserAvatarOrDefault(isForceRefresh: Boolean) {
        viewModelScope.launch {
            val avatarFile = runCatching { getMyAvatarFile(isForceRefresh) }
                .onFailure { Timber.e(it) }.getOrNull()
            val avatarContent = avatarContentMapper(
                fullName = _state.value.fullName,
                localFile = avatarFile,
                backgroundColor = { getMyAvatarColor() },
                showBorder = false,
                textSize = 36.sp
            )
            _state.update {
                it.copy(
                    avatarContent = avatarContent,
                )
            }
        }
    }

    private suspend fun handleOtherUsersUpdate(userUpdate: UserUpdate) {
        userUpdate.changes.forEach { entry ->
            if (entry.value.contains(UserChanges.Firstname)) {
                Timber.d("The user: ${entry.key.id} changed his first name")
                runCatching {
                    getUserFirstName(
                        handle = entry.key.id,
                        skipCache = true,
                        shouldNotify = true
                    )
                }.onFailure {
                    Timber.e(it)
                }
            }

            if (entry.value.contains(UserChanges.Lastname)) {
                Timber.d("The user: ${entry.key.id} changed his last name")
                runCatching {
                    getUserLastName(
                        handle = entry.key.id,
                        skipCache = true,
                        shouldNotify = true
                    )
                }.onFailure {
                    Timber.e(it)
                }
            }

            if (entry.value.contains(UserChanges.Alias)) {
                Timber.d("I changed the user: ${entry.key.id} nickname")
                runCatching { getCurrentUserAliases() }.onFailure {
                    Timber.e(it)
                }
            }

            if (entry.value.contains(UserChanges.Email)) {
                Timber.d("The contact: ${entry.key.id} changes the mail.")
                runCatching { getContactEmail(entry.key.id) }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    private suspend fun handleEmailChange() {
        val oldEmail = _state.value.email
        getMyEmail()
        val newEmail = _state.value.email
        runCatching { updateMyAvatarWithNewEmail(oldEmail, newEmail) }
            .onSuccess { success -> if (success) Timber.d("The avatar file was correctly renamed") }
            .onFailure {
                Timber.e(it, "EXCEPTION renaming the avatar on changing email")
            }
    }

    fun getUserInfo() {
        viewModelScope.launch {
            getMyEmail()
            getUserFullName()
            getUserAvatarOrDefault(true)
        }
    }

    private suspend fun getMyEmail() {
        _state.update { it.copy(email = getCurrentUserEmail().orEmpty()) }
    }

    private suspend fun getUserFullName() {
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

    /**
     * Reload contact database
     * Make it run in application scope so it still running when activity destroyed
     */
    fun refreshContactDatabase(isForce: Boolean) {
        applicationScope.launch {
            reloadContactDatabase(isForce)
        }
    }
}