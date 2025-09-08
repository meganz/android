package mega.privacy.android.app.presentation.myaccount

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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.myaccount.mapper.AccountNameMapper
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.IsAchievementsEnabled
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.shares.GetInSharesUseCase
import mega.privacy.android.domain.usecase.transfers.GetUsedTransferStatusUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for MyAccountFragment
 * @see MyAccountFragment
 */
@HiltViewModel
class MyAccountHomeViewModel @Inject constructor(
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getInSharesUseCase: GetInSharesUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val getUserFullNameUseCase: GetUserFullNameUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val getUsedTransferStatusUseCase: GetUsedTransferStatusUseCase,
    private val accountNameMapper: AccountNameMapper,
    private val avatarContentMapper: AvatarContentMapper,
    private val isAchievementsEnabled: IsAchievementsEnabled,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(MyAccountHomeUIState(accountTypeNameResource = accountNameMapper(null)))

    /**
     * My Account Home Fragment Ui State
     * @see MyAccountFragment
     */
    val uiState = _uiState.asStateFlow()

    init {
        refreshAccountInfo()
        refreshUserName(false)
        refreshCurrentUserEmail()
        getVisibleContacts()

        viewModelScope.launch {
            monitorConnectivityUseCase()
                .collectLatest { isConnected ->
                    _uiState.update {
                        it.copy(isConnectedToNetwork = isConnected)
                    }
                }
        }
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.w("Exception monitoring user updates: $it") }
                .filter {
                    it == UserChanges.Firstname ||
                            it == UserChanges.Lastname ||
                            it == UserChanges.Email
                }.collect {
                    when (it) {
                        UserChanges.Email -> refreshCurrentUserEmail()
                        UserChanges.Firstname, UserChanges.Lastname -> refreshUserName(true)
                        else -> Unit
                    }
                }
        }
        combine(
            uiState.map { it.name }.distinctUntilChanged(),
            monitorMyAvatarFile().onStart { emit(getMyAvatarFileUseCase(isForceRefresh = false)) },
            transform = { name, file ->
                if (!name.isNullOrEmpty()) {
                    val avatarContent = avatarContentMapper(
                        fullName = name,
                        localFile = file,
                        backgroundColor = getMyAvatarColorUseCase(),
                        showBorder = false,
                        textSize = 36.sp
                    )
                    _uiState.update {
                        it.copy(avatarContent = avatarContent)
                    }
                }
            }
        ).launchIn(viewModelScope)
        viewModelScope.launch {
            monitorVerificationStatus()
                .collectLatest { status ->
                    _uiState.update {
                        it.copy(
                            verifiedPhoneNumber = (status.phoneNumber as? VerifiedPhoneNumber.PhoneNumber)?.phoneNumberString,
                            canVerifyPhoneNumber = status.canRequestOptInVerification
                        )
                    }
                }
        }
        viewModelScope.launch {
            monitorAccountDetailUseCase()
                .catch { Timber.e(it) }
                .collectLatest { accountDetail ->
                    _uiState.update {
                        it.copy(
                            hasRenewableSubscription = accountDetail.levelDetail?.accountType !== AccountType.FREE && accountDetail.levelDetail?.subscriptionStatus == SubscriptionStatus.VALID
                                    && (accountDetail.levelDetail?.subscriptionRenewTime ?: 0) > 0,
                            hasExpireAbleSubscription = accountDetail.levelDetail?.accountType !== AccountType.FREE && (accountDetail.levelDetail?.proExpirationTime
                                ?: 0) > 0,
                            lastSession = (accountDetail.sessionDetail?.mostRecentSessionTimeStamp)
                                ?: 0,
                            usedStorage = accountDetail.storageDetail?.usedStorage ?: 0,
                            usedStoragePercentage = accountDetail.storageDetail?.usedPercentage
                                ?: 0,
                            usedTransfer = accountDetail.transferDetail?.usedTransfer ?: 0,
                            usedTransferPercentage = accountDetail.transferDetail?.usedTransferPercentage
                                ?: 0,
                            usedTransferStatus = accountDetail.transferDetail?.usedTransferPercentage?.let { usedTransferPercentage ->
                                getUsedTransferStatusUseCase(
                                    usedTransferPercentage
                                )
                            } ?: UsedTransferStatus.NoTransferProblems,
                            totalStorage = accountDetail.storageDetail?.totalStorage ?: 0,
                            totalTransfer = accountDetail.transferDetail?.totalTransfer ?: 0,
                            subscriptionRenewTime = accountDetail.levelDetail?.subscriptionRenewTime
                                ?: 0,
                            proExpirationTime = accountDetail.levelDetail?.proExpirationTime ?: 0
                        )
                    }
                }
        }
        viewModelScope.launch {
            runCatching {
                isAchievementsEnabled()
            }.onSuccess { isEnabled ->
                _uiState.update {
                    it.copy(isAchievementsAvailable = isEnabled)
                }
            }.onFailure {
                Timber.e(it, "Error checking if achievements is enabled")
            }
        }
    }

    private fun getVisibleContacts() {
        viewModelScope.launch {
            val contactsSize = getVisibleContactsUseCase().filter { contact ->
                contact.visibility == UserVisibility.Visible || getInSharesUseCase(contact.email).isNotEmpty()
            }.size

            _uiState.update { it.copy(visibleContacts = contactsSize) }
        }
    }

    private fun refreshCurrentUserEmail() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(email = getCurrentUserEmail().orEmpty())
            }
        }
    }

    private fun refreshUserName(forceRefresh: Boolean) {
        viewModelScope.launch {
            val fullName = getUserFullNameUseCase(forceRefresh)
            _uiState.update { it.copy(name = fullName) }
        }
    }

    /**
     * Refresh account info
     */
    fun refreshAccountInfo() {
        viewModelScope.launch {
            runCatching {
                val accountDetails = getAccountDetailsUseCase(false)
                _uiState.update {
                    it.copy(
                        accountType = accountDetails.accountTypeIdentifier,
                        isBusinessAccount = accountDetails.isBusinessAccount && accountDetails.accountTypeIdentifier == AccountType.BUSINESS,
                        isProFlexiAccount = accountDetails.accountTypeIdentifier == AccountType.PRO_FLEXI,
                        isMasterBusinessAccount = accountDetails.isMasterBusinessAccount,
                        accountTypeNameResource = accountNameMapper(accountDetails.accountTypeIdentifier)
                    )
                }

                val businessStatus = getBusinessStatusUseCase()
                val isBusinessStatusActive =
                    (businessStatus == BusinessAccountStatus.GracePeriod || businessStatus == BusinessAccountStatus.Expired).not()

                _uiState.update {
                    it.copy(
                        isBusinessProFlexiStatusActive = isBusinessStatusActive,
                        businessProFlexiStatus = businessStatus
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Trigger navigation event to Achievements screen if connected to network
     * if not connected, show message
     */
    fun navigateToAchievements() {
        viewModelScope.launch {
            if (uiState.value.isConnectedToNetwork) {
                _uiState.update {
                    it.copy(navigateToAchievements = triggered)
                }
            } else {
                _uiState.update {
                    it.copy(userMessage = triggered(R.string.error_server_connection_problem))
                }
            }
        }
    }

    /**
     * Reset Achievements navigation event to consumed
     */
    fun resetNavigationToAchievements() {
        viewModelScope.launch {
            _uiState.update { it.copy(navigateToAchievements = consumed) }
        }
    }

    /**
     * Reset user message event to consumed
     */
    fun resetUserMessage() {
        viewModelScope.launch {
            _uiState.update { it.copy(userMessage = consumed()) }
        }
    }
}