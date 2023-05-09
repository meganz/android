package mega.privacy.android.app.presentation.myaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.GetUserFullName
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorAccountDetail
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.shares.GetInSharesUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import mega.privacy.android.domain.usecase.verification.MonitorVerifiedPhoneNumber
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for MyAccountFragment
 * @see MyAccountFragment
 */
@HiltViewModel
class MyAccountHomeViewModel @Inject constructor(
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val monitorAccountDetail: MonitorAccountDetail,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase,
    private val getInSharesUseCase: GetInSharesUseCase,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val getUserFullName: GetUserFullName,
    private val getMyAvatarFile: GetMyAvatarFile,
    private val getAccountAchievements: GetAccountAchievements,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyAccountHomeUIState())

    /**
     * My Account Home Fragment Ui State
     * @see MyAccountFragment
     */
    val uiState = _uiState.asStateFlow()

    init {
        refreshAccountInfo()
        getDefaultAvatarColor()
        refreshUserName(false)
        refreshCurrentUserEmail()
        getVisibleContacts()

        viewModelScope.launch {
            flow {
                emitAll(monitorConnectivityUseCase())
            }.collectLatest { isConnected ->
                _uiState.update {
                    it.copy(isConnectedToNetwork = isConnected)
                }
            }
        }

        viewModelScope.launch {
            flow {
                emitAll(monitorUserUpdates().filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email })
            }.collectLatest {
                when (it) {
                    UserChanges.Email -> refreshCurrentUserEmail()
                    UserChanges.Firstname, UserChanges.Lastname -> refreshUserName(true)
                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            flow {
                emit(getMyAvatarFile(isForceRefresh = false))
                emitAll(monitorMyAvatarFile())
            }.collectLatest { file ->
                _uiState.update {
                    it.copy(avatar = file)
                }
            }
        }
        viewModelScope.launch {
            monitorVerificationStatus().collectLatest { status ->
                _uiState.update {
                    it.copy(
                        verifiedPhoneNumber = (status.phoneNumber as? VerifiedPhoneNumber.PhoneNumber)?.phoneNumberString,
                        canVerifyPhoneNumber = status.canRequestOptInVerification
                    )
                }
            }
        }

        viewModelScope.launch {
            flow {
                emitAll(monitorAccountDetail())
            }.collectLatest { accountDetail ->
                _uiState.update {
                    it.copy(
                        hasRenewableSubscription = accountDetail.levelDetail?.subscriptionStatus == SubscriptionStatus.VALID
                                && (accountDetail.levelDetail?.subscriptionRenewTime ?: 0) > 0,
                        hasExpireAbleSubscription = (accountDetail.levelDetail?.proExpirationTime
                            ?: 0) > 0,
                        lastSession = (accountDetail.sessionDetail?.mostRecentSessionTimeStamp)
                            ?: 0,
                        usedStorage = accountDetail.storageDetail?.usedStorage ?: 0,
                        usedStoragePercentage = accountDetail.storageDetail?.usedPercentage ?: 0,
                        usedTransfer = accountDetail.transferDetail?.usedTransfer ?: 0,
                        usedTransferPercentage = accountDetail.transferDetail?.usedTransferPercentage
                            ?: 0,
                        totalStorage = accountDetail.storageDetail?.totalStorage ?: 0,
                        totalTransfer = accountDetail.transferDetail?.totalTransfer ?: 0,
                        subscriptionRenewTime = accountDetail.levelDetail?.subscriptionRenewTime
                            ?: 0,
                        proExpirationTime = accountDetail.levelDetail?.proExpirationTime ?: 0
                    )
                }
            }
        }
    }

    private fun getDefaultAvatarColor() {
        viewModelScope.launch {
            _uiState.update { it.copy(avatarColor = getMyAvatarColorUseCase()) }
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
            _uiState.update {
                it.copy(
                    name = getUserFullName(forceRefresh = forceRefresh)
                )
            }
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
                        isMasterBusinessAccount = accountDetails.isMasterBusinessAccount,
                    )
                }

                val isBusinessStatusActive = getBusinessStatusUseCase().let { status ->
                    (status == BusinessAccountStatus.GracePeriod || status == BusinessAccountStatus.Expired).not()
                }
                _uiState.update {
                    it.copy(isBusinessStatusActive = isBusinessStatusActive)
                }

                val achievements = getAccountAchievements(
                    AchievementType.MEGA_ACHIEVEMENT_ADD_PHONE,
                    awardIndex = 0,
                )
                _uiState.update {
                    it.copy(
                        isAchievementsEnabled = achievements != null,
                        bonusStorageSms = achievements?.grantedStorage ?: 0
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}