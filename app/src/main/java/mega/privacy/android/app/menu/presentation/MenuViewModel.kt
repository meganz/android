package mega.privacy.android.app.menu.presentation

import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.menu.navigation.AchievementsItem
import mega.privacy.android.app.menu.navigation.CurrentPlanItem
import mega.privacy.android.app.menu.navigation.RubbishBinItem
import mega.privacy.android.app.menu.navigation.StorageItem
import mega.privacy.android.app.presentation.mapper.AccountTypeIconMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetRubbishNodeUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabledUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.notifications.MonitorNotSeenUserAlertsCountUseCase
import mega.privacy.android.feature.myaccount.presentation.mapper.AccountTypeNameMapper
import mega.privacy.android.feature.myaccount.presentation.mapper.AvatarContentMapper
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
    private val accountTypeNameMapper: AccountTypeNameMapper,
    private val accountTypeIconMapper: AccountTypeIconMapper,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val isAchievementsEnabledUseCase: IsAchievementsEnabledUseCase,
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase,
    private val monitorNotSeenUserAlertsCountUseCase: MonitorNotSeenUserAlertsCountUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val getRubbishNodeUseCase: GetRubbishNodeUseCase,
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase,
    private val avatarContentMapper: AvatarContentMapper,
) : ViewModel() {
    // Flows for items that need dynamic subtitles
    private val currentPlanSubtitleFlow = MutableStateFlow<String?>(null)
    private val storageSubtitleFlow = MutableStateFlow<String?>(null)
    private val rubbishBinSubtitleFlow = MutableStateFlow<String?>(null)

    private val privacySuiteItems: Map<Int, NavDrawerItem.PrivacySuite> = menuItems
        .filterValues { it is NavDrawerItem.PrivacySuite }
        .mapValues { it.value as NavDrawerItem.PrivacySuite }

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState = _uiState.asStateFlow()

    init {
        setMenuItems()
        monitorConnectivity()
        monitorUserDataAndAvatar()
        refreshAccountStorageDetails()
        monitorAccountDetails()
        refreshUserName()
        refreshCurrentUserEmail()
        monitorUserChanges()
        monitorUnreadNotificationsCount()
        monitorNodeUpdatesForRubbishBin()
    }

    private fun setMenuItems() {
        viewModelScope.launch {
            combine(
                flow { emit(isAchievementsEnabledUseCase()) }.catch { emit(false) },
                monitorAccountDetailUseCase().map { it.levelDetail?.accountType }
                    .catch { emit(null) }
                    .distinctUntilChanged()
            ) { isAchievementsEnabled, accountType ->
                filterMyAccountItems(isAchievementsEnabled, accountType ?: AccountType.FREE)
            }.collect { myAccountItems ->
                _uiState.update {
                    it.copy(myAccountItems = myAccountItems, privacySuiteItems = privacySuiteItems)
                }
            }
        }
    }

    private fun filterMyAccountItems(
        isAchievementsEnabled: Boolean,
        accountType: AccountType?,
    ): Map<Int, NavDrawerItem.Account> =
        menuItems
            .filterValues { it is NavDrawerItem.Account && (it !is AchievementsItem || isAchievementsEnabled) }
            .mapValues {
                val item = it.value as NavDrawerItem.Account
                when (item) {
                    is CurrentPlanItem -> {
                        NavDrawerItem.Account(
                            destination = item.destination,
                            icon = accountTypeIconMapper(
                                accountType = accountType
                            ),
                            title = item.title,
                            subTitle = currentPlanSubtitleFlow,
                            actionLabel = if (accountType != null && accountType != AccountType.PRO_FLEXI && accountType != AccountType.BUSINESS) item.actionLabel else 0,
                            analyticsEventIdentifier = item.analyticsEventIdentifier,
                        )
                    }

                    is StorageItem -> {
                        NavDrawerItem.Account(
                            destination = item.destination,
                            icon = item.icon,
                            title = item.title,
                            subTitle = storageSubtitleFlow,
                            actionLabel = item.actionLabel,
                            analyticsEventIdentifier = item.analyticsEventIdentifier,
                        )
                    }

                    is RubbishBinItem -> {
                        NavDrawerItem.Account(
                            destination = item.destination,
                            icon = item.icon,
                            title = item.title,
                            subTitle = rubbishBinSubtitleFlow,
                            actionLabel = item.actionLabel,
                            analyticsEventIdentifier = item.analyticsEventIdentifier,
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
                            refreshUserName()
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

    private fun monitorNodeUpdatesForRubbishBin() {
        viewModelScope.launch {
            runCatching {
                getRubbishNodeUseCase()?.id?.longValue
            }.onSuccess { rubbishBinNodeId ->
                rubbishBinNodeId?.let {
                    monitorNodeUpdatesUseCase()
                        .catch { Timber.w("Exception monitoring node updates: $it") }
                        .collectLatest { nodeUpdate ->
                            val hasRubbishBinUpdate = nodeUpdate.changes.keys.any { node ->
                                node.id.longValue == rubbishBinNodeId || node.parentId.longValue == rubbishBinNodeId
                            }
                            if (hasRubbishBinUpdate) {
                                refreshAccountStorageDetails()
                            }
                        }
                }
            }.onFailure {
                Timber.e(it, "Error getting rubbish bin node id")
            }
        }
    }

    private fun refreshAccountStorageDetails() {
        viewModelScope.launch {
            getSpecificAccountDetail()
        }
    }

    private suspend fun getSpecificAccountDetail() {
        runCatching {
            getSpecificAccountDetailUseCase(storage = true, transfer = false, pro = false)
        }.onFailure {
            Timber.e(it, "Error refreshing account storage details")
        }
    }

    private fun monitorUserDataAndAvatar() {
        combine(
            uiState.map { it.name }.distinctUntilChanged(),
            monitorMyAvatarFile().onStart {
                // emit from cache first and then from remote
                emit(runCatching { getMyAvatarFileUseCase(isForceRefresh = false) }.getOrNull())
                emit(runCatching { getMyAvatarFileUseCase(isForceRefresh = true) }.getOrNull())
            }.catch { e ->
                Timber.e(e, "Error monitoring avatar file: $e")
                emit(null)
            }, transform = { name, file ->
                if (!name.isNullOrEmpty()) {
                    val avatarColor =
                        runCatching { getMyAvatarColorUseCase() }.getOrDefault(0)
                    val avatarContent = avatarContentMapper(
                        fullName = name,
                        localFile = file,
                        showBorder = false,
                        textSize = 18.sp,
                        backgroundColor = avatarColor,
                    )
                    _uiState.update {
                        it.copy(
                            avatarContent = avatarContent,
                            lastModifiedTime = file?.lastModified() ?: 0L,
                        )
                    }
                }
            }
        ).catch { e ->
            Timber.e(e, "Error loading avatar data")
        }.launchIn(viewModelScope)
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
                    val accountTypeName = accountTypeNameMapper(accountType)
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
                    if (!isConnected) {
                        refreshCurrentUserEmail(false)
                        refreshUserName(false)
                    }
                }
        }
    }

    private fun refreshUserName(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            val name =
                runCatching { getUserFullNameUseCase(forceRefresh = forceRefresh) }.getOrNull()
            _uiState.update {
                it.copy(name = name ?: it.name)
            }
        }
    }

    private fun refreshCurrentUserEmail(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            val email = runCatching { getCurrentUserEmail(forceRefresh) }.getOrNull()
            _uiState.update {
                it.copy(email = email ?: it.email)
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
