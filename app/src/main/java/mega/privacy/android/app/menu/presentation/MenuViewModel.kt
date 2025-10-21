package mega.privacy.android.app.menu.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.menu.navigation.AchievementsItem
import mega.privacy.android.app.menu.navigation.CurrentPlanItem
import mega.privacy.android.app.menu.navigation.RubbishBinItem
import mega.privacy.android.app.menu.navigation.StorageItem
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.myaccount.mapper.AccountNameMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.notifications.MonitorNotSeenUserAlertsCountUseCase
import mega.privacy.android.navigation.contract.NavDrawerItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    val menuItems: Map<Int, @JvmSuppressWildcards NavDrawerItem>,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val accountNameMapper: AccountNameMapper,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val isAchievementsEnabledUseCase: IsAchievementsEnabledUseCase,
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase,
    private val monitorNotSeenUserAlertsCountUseCase: MonitorNotSeenUserAlertsCountUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    // Flows for items that need dynamic subtitles
    private val currentPlanSubtitleFlow = MutableStateFlow<String?>(null)
    private val storageSubtitleFlow = MutableStateFlow<String?>(null)
    private val rubbishBinSubtitleFlow = MutableStateFlow<String?>(null)

    private val privacySuiteItems: Map<Int, NavDrawerItem.PrivacySuite> = menuItems
        .filterValues { it is NavDrawerItem.PrivacySuite }
        .mapValues { it.value as NavDrawerItem.PrivacySuite }

    private val _uiState = MutableStateFlow(
        MenuUiState(
            privacySuiteItems = privacySuiteItems
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        setMyAccountItems()
        monitorConnectivity()
        monitorUserDataAndAvatar()
        monitorAccountDetails()
        refreshUserName(false)
        refreshCurrentUserEmail()
        monitorUserChanges()
        monitorUnreadNotificationsCount()
    }

    private fun setMyAccountItems() {
        viewModelScope.launch {
            val isAchievementsEnabled =
                runCatching { isAchievementsEnabledUseCase() }.getOrNull() == true
            val myAccountItems: Map<Int, NavDrawerItem.Account> =
                filterMyAccountItems(isAchievementsEnabled)
            _uiState.update {
                it.copy(myAccountItems = myAccountItems)
            }
        }
    }

    private fun filterMyAccountItems(isAchievementsEnabled: Boolean): Map<Int, NavDrawerItem.Account> =
        menuItems
            .filterValues { it is NavDrawerItem.Account && (it !is AchievementsItem || it is AchievementsItem && isAchievementsEnabled) }
            .mapValues {
                val item = it.value as NavDrawerItem.Account
                when (item) {
                    is CurrentPlanItem -> {
                        NavDrawerItem.Account(
                            destination = item.destination,
                            icon = item.icon,
                            title = item.title,
                            subTitle = currentPlanSubtitleFlow,
                            actionLabel = item.actionLabel
                        )
                    }

                    is StorageItem -> {
                        NavDrawerItem.Account(
                            destination = item.destination,
                            icon = item.icon,
                            title = item.title,
                            subTitle = storageSubtitleFlow,
                            actionLabel = item.actionLabel
                        )
                    }

                    is RubbishBinItem -> {
                        NavDrawerItem.Account(
                            destination = item.destination,
                            icon = item.icon,
                            title = item.title,
                            subTitle = rubbishBinSubtitleFlow,
                            actionLabel = item.actionLabel
                        )
                    }

                    else -> item
                }
            }

    private fun monitorUserChanges() {
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.w("Exception monitoring user updates: $it") }
                .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email }
                .collect {
                    when (it) {
                        UserChanges.Email -> refreshCurrentUserEmail()
                        UserChanges.Firstname,
                        UserChanges.Lastname,
                            -> {
                            refreshUserName(true)
                            getUserAvatarOrDefault(true)
                        }

                        else -> Unit
                    }
                }
        }
    }

    private fun monitorUnreadNotificationsCount() {
        viewModelScope.launch {
            monitorNotSeenUserAlertsCountUseCase()
                .catch { Timber.w("Exception monitoring user updates: $it") }
                .collect { unreadCount ->
                    _uiState.update {
                        it.copy(unreadNotificationsCount = unreadCount)
                    }
                }
        }
    }

    private suspend fun getUserAvatarOrDefault(isForceRefresh: Boolean) {
        val avatarFile = runCatching { getMyAvatarFileUseCase(isForceRefresh) }
            .onFailure { Timber.e(it) }.getOrNull()
        val color = runCatching { getMyAvatarColorUseCase() }.getOrNull()
        _uiState.update {
            it.copy(
                avatar = avatarFile,
                avatarColor = color?.let { color -> Color(color) } ?: Color.Unspecified,
            )
        }
    }

    private fun monitorUserDataAndAvatar() {
        viewModelScope.launch {
            monitorMyAvatarFile().onStart {
                // emit from cache first and then from remote
                emit(getMyAvatarFileUseCase(isForceRefresh = false))
                emit(getMyAvatarFileUseCase(isForceRefresh = true))
            }.map { file ->
                file to (file?.lastModified() ?: 0L)
            }.flowOn(ioDispatcher).catch { Timber.e(it) }
                .collectLatest { (avatarFile, lastModified) ->
                    val color = runCatching { getMyAvatarColorUseCase() }.getOrNull()
                    _uiState.update {
                        it.copy(
                            avatar = avatarFile,
                            avatarColor = color?.let { color -> Color(color) }
                                ?: Color.Unspecified,
                            lastModifiedTime = lastModified
                        )
                    }
                }
        }
    }


    private fun monitorAccountDetails() {
        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.e(it) }
                .collectLatest { accountDetail ->
                    val usedStorage = accountDetail.storageDetail?.usedStorage ?: 0
                    val totalStorage = accountDetail.storageDetail?.totalStorage ?: 0
                    val usedRubbish = accountDetail.storageDetail?.usedRubbish ?: 0
                    val accountType = accountDetail.levelDetail?.accountType ?: AccountType.FREE
                    val accountTypeName = accountNameMapper(accountType)
                    storageSubtitleFlow.value =
                        "${fileSizeStringMapper(usedStorage)}/${fileSizeStringMapper(totalStorage)}"
                    currentPlanSubtitleFlow.value = getStringFromStringResMapper(accountTypeName)
                    rubbishBinSubtitleFlow.value = fileSizeStringMapper(usedRubbish)
                }
        }
    }


    private fun monitorConnectivity() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .collectLatest { isConnected ->
                    _uiState.update {
                        it.copy(isConnectedToNetwork = isConnected)
                    }
                }
        }
    }

    private fun refreshUserName(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    name = runCatching {
                        getUserFullNameUseCase(
                            forceRefresh = forceRefresh,
                        )
                    }.getOrNull()
                )
            }
        }
    }

    private fun refreshCurrentUserEmail() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(email = runCatching { getCurrentUserEmail() }.getOrNull())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            runCatching { checkPasswordReminderUseCase(true) }
                .onSuccess { show ->
                    if (show) {
                        _uiState.update {
                            it.copy(showTestPasswordScreenEvent = triggered, isLoggingOut = false)
                        }
                    } else {
                        _uiState.update {
                            it.copy(showLogoutConfirmationEvent = triggered, isLoggingOut = false)
                        }
                    }
                }.onFailure { error ->
                    Timber.e(error, "Error checking password reminder requirement")
                    _uiState.update { it.copy(isLoggingOut = false) }
                }
        }
    }

    fun resetTestPasswordScreenEvent() {
        _uiState.update {
            it.copy(showTestPasswordScreenEvent = consumed)
        }
    }

    fun resetLogoutConfirmationEvent() {
        _uiState.update {
            it.copy(showLogoutConfirmationEvent = consumed)
        }
    }
}
