package mega.privacy.android.app.menu.presentation

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.navigation.contract.NavDrawerItem
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    menuItems: Map<Int, @JvmSuppressWildcards NavDrawerItem>,
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    // Flows for items that need dynamic subtitles
    private val currentPlanSubtitleFlow = MutableStateFlow<String?>(null)
    private val storageSubtitleFlow = MutableStateFlow<String?>(null)
    private val rubbishBinSubtitleFlow = MutableStateFlow<String?>(null)

    private val myAccountItems: Map<Int, NavDrawerItem.Account> = menuItems
        .filterValues { it is NavDrawerItem.Account }
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

    private val privacySuiteItems: Map<Int, NavDrawerItem.PrivacySuite> = menuItems
        .filterValues { it is NavDrawerItem.PrivacySuite }
        .mapValues { it.value as NavDrawerItem.PrivacySuite }

    private val _uiState = MutableStateFlow(
        MenuUiState(
            myAccountItems = myAccountItems,
            privacySuiteItems = privacySuiteItems
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        monitorConnectivity()
        monitorUserDataAndAvatar()
        monitorAccountDetails()
        refreshUserName(false)
        refreshCurrentUserEmail()
        monitorUserChanges()
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
}
