package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.manager.model.ManagerState
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.entity.verification.VerificationStatus
import mega.privacy.android.domain.usecase.BroadcastUploadPauseState
import mega.privacy.android.domain.usecase.CheckCameraUpload
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetFullAccountInfo
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.HasInboxChildren
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorFinishActivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.SendStatisticsMediaDiscovery
import mega.privacy.android.domain.usecase.account.MonitorSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.account.SetLatestTargetPath
import mega.privacy.android.domain.usecase.billing.GetActiveSubscription
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMedia
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUserAlert
import timber.log.Timber
import javax.inject.Inject

/**
 * Manager view model
 *
 * @property monitorGlobalUpdates
 * @property getInboxNode
 * @property getNumUnreadUserAlerts
 * @property hasInboxChildren
 * @property sendStatisticsMediaDiscovery
 * @property savedStateHandle
 * @property monitorStorageStateEvent
 * @property monitorViewType
 * @property getPrimarySyncHandle
 * @property getSecondarySyncHandle
 * @property checkCameraUpload
 * @property getCloudSortOrder
 * @property monitorConnectivity
 * @property broadcastUploadPauseState
 * @property getExtendedAccountDetail
 * @property getPricing
 * @property getFullAccountInfo
 * @property getActiveSubscription
 * @property getFeatureFlagValue
 * @property getUnverifiedIncomingShares
 * @property getUnverifiedOutgoingShares
 * @property monitorVerificationStatus
 *
 * @param monitorNodeUpdates
 * @param monitorContactUpdates monitor contact update when credentials verification occurs to update shares count
 * @param monitorContactRequestUpdates
 * @param monitorFinishActivity
 */
@HiltViewModel
class ManagerViewModel @Inject constructor(
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorContactUpdates: MonitorContactUpdates,
    private val monitorGlobalUpdates: MonitorGlobalUpdates,
    monitorContactRequestUpdates: MonitorContactRequestUpdates,
    private val getInboxNode: GetInboxNode,
    private val getNumUnreadUserAlerts: GetNumUnreadUserAlerts,
    private val hasInboxChildren: HasInboxChildren,
    private val sendStatisticsMediaDiscovery: SendStatisticsMediaDiscovery,
    private val savedStateHandle: SavedStateHandle,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val monitorViewType: MonitorViewType,
    private val getPrimarySyncHandle: GetPrimarySyncHandle,
    private val getSecondarySyncHandle: GetSecondarySyncHandle,
    private val checkCameraUpload: CheckCameraUpload,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorConnectivity: MonitorConnectivity,
    private val broadcastUploadPauseState: BroadcastUploadPauseState,
    private val getExtendedAccountDetail: GetExtendedAccountDetail,
    private val getPricing: GetPricing,
    private val getFullAccountInfo: GetFullAccountInfo,
    private val getActiveSubscription: GetActiveSubscription,
    private val getFeatureFlagValue: GetFeatureFlagValue,
    private val getUnverifiedIncomingShares: GetUnverifiedIncomingShares,
    private val getUnverifiedOutgoingShares: GetUnverifiedOutgoingShares,
    monitorFinishActivity: MonitorFinishActivity,
    private val requireTwoFactorAuthenticationUseCase: RequireTwoFactorAuthenticationUseCase,
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val setLatestTargetPath: SetLatestTargetPath,
    private val monitorSecurityUpgradeInApp: MonitorSecurityUpgradeInApp,
    private val listenToNewMedia: ListenToNewMedia,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ManagerState())

    /**
     * public UI State
     */
    val state: StateFlow<ManagerState> = _state

    /**
     * private Inbox Node
     */
    private var inboxNode: MegaNode? = null

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.Eagerly)

    /**
     * Monitor Finish Activity event
     */
    val monitorFinishActivityEvent = monitorFinishActivity()

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    private val isFirstLogin = savedStateHandle.getStateFlow(
        key = isFirstLoginKey,
        initialValue = false
    )

    init {
        viewModelScope.launch {
            val order = getCloudSortOrder()
            combine(
                isFirstLogin,
                monitorVerificationStatus()
                    .onEach {
                            Timber.d("Verification status returned: $it")
                    },
                flowOf(
                    getUnverifiedIncomingShares(order) +
                            getUnverifiedOutgoingShares(order).filter { shareData -> !shareData.isVerified }
                ).map { it.size },
                flowOf(getEnabledFeatures()),
            ) { firstLogin: Boolean, verificationStatus: VerificationStatus, pendingShares: Int, features: Set<Feature> ->
                { state: ManagerState ->
                    state.copy(
                        isFirstLogin = firstLogin,
                        canVerifyPhoneNumber = verificationStatus is UnVerified && verificationStatus.canRequestOptInVerification,
                        pendingActionsCount = state.pendingActionsCount + pendingShares,
                        enabledFlags = features
                    )
                }
            }.collectLatest {
                _state.update(it)
            }
        }


        viewModelScope.launch {
            monitorNodeUpdates().collect {
                val nodeList = it.changes.keys.toList()
                checkItemForInbox(nodeList)
                onReceiveNodeUpdate(true)
                checkCameraUploadFolder(false, nodeList)
                checkUnverifiedSharesCount()
            }
        }
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { updates ->
                if (updates.changes.values.any { it.contains(UserChanges.AuthenticationInformation) }) {
                    checkUnverifiedSharesCount()
                }
            }
        }
        viewModelScope.launch {
            monitorSecurityUpgradeInApp().collect {
                if (it) {
                    setShouldAlertUserAboutSecurityUpgrade(true)
                }
            }
        }
        viewModelScope.launch {
            listenToNewMedia()
        }
    }

    private suspend fun getEnabledFeatures(): Set<Feature> {
        return setOfNotNull(
            AppFeatures.AndroidSync.takeIf { getFeatureFlagValue(it) },
            AppFeatures.MonitorPhoneNumber.takeIf { getFeatureFlagValue(it) }
        )
    }

    /**
     * Monitor all global updates
     */
    @Suppress("DEPRECATION")
    private val _updates = monitorGlobalUpdates()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Monitor contact requests
     */
    private val _updateContactRequests = monitorContactRequestUpdates()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Monitor user alerts updates and dispatch to observers
     */
    val updateUserAlerts: LiveData<Event<List<MegaUserAlert>>> =
        _updates
            .filterIsInstance<GlobalUpdate.OnUserAlertsUpdate>()
            .also { Timber.d("onUserAlertsUpdate") }
            .mapNotNull { it.userAlerts?.toList() }
            .map { Event(it) }
            .asLiveData()

    private fun checkItemForInbox(updatedNodes: List<Node>) {
        //Verify is it is a new item to the inbox
        inboxNode?.let { node ->
            updatedNodes.find { node.handle == it.parentId.longValue }
                ?.run { updateInboxSectionVisibility() }
        }
    }

    /**
     * Monitor contact request updates and dispatch to observers
     */
    val updateContactsRequests: LiveData<Event<List<ContactRequest>>> =
        _updateContactRequests
            .also { Timber.d("onContactRequestsUpdate") }
            .map { Event(it) }
            .asLiveData()

    /**
     * Flow that monitors the View Type
     */
    val onViewTypeChanged: Flow<ViewType>
        get() = monitorViewType()

    /**
     * Set a flag to know if the current navigation level is the first one
     *
     * @param isFirstNavigationLevel true if the current navigation level corresponds to the first level
     */
    fun setIsFirstNavigationLevel(isFirstNavigationLevel: Boolean) = viewModelScope.launch {
        _state.update { it.copy(isFirstNavigationLevel = isFirstNavigationLevel) }
    }

    /**
     * Set the current shares tab to the UI state
     *
     * @param tab shares tab to set
     */
    fun setSharesTab(tab: SharesTab) = viewModelScope.launch {
        _state.update { it.copy(sharesTab = tab) }
    }

    /**
     * Set the current transfers tab to the UI state
     *
     * @param tab transfer tab to set
     */
    fun setTransfersTab(tab: TransfersTab) = viewModelScope.launch {
        _state.update { it.copy(transfersTab = tab) }
    }

    /**
     * Notify that the node update has been handled by the UI
     */
    fun nodeUpdateHandled() {
        onReceiveNodeUpdate(false)
    }

    /**
     *  Get the unverified shares count and set state
     */
    private suspend fun checkUnverifiedSharesCount() {
        val sortOrder = getCloudSortOrder()
        val unverifiedIncomingShares = getUnverifiedIncomingShares(sortOrder).size
        val unverifiedOutgoingShares =
            getUnverifiedOutgoingShares(sortOrder).filter { shareData -> !shareData.isVerified }.size
        _state.update { it.copy(pendingActionsCount = unverifiedIncomingShares + unverifiedOutgoingShares) }
    }

    /**
     * Set the ui one-off event when a node update is received
     *
     * @param update true if a node update has been received
     */
    private fun onReceiveNodeUpdate(update: Boolean) = viewModelScope.launch {
        _state.update { it.copy(nodeUpdateReceived = update) }
    }

    private val numUnreadUserAlerts = SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>>()

    /**
     * Notifies about the number of unread user alerts once.
     *
     * @return [SingleLiveEvent] with the number of unread user alerts.
     */
    fun onGetNumUnreadUserAlerts(): SingleLiveEvent<Pair<UnreadUserAlertsCheckType, Int>> =
        numUnreadUserAlerts

    /**
     * Checks the number of unread user alerts.
     */
    fun checkNumUnreadUserAlerts(type: UnreadUserAlertsCheckType) {
        viewModelScope.launch {
            numUnreadUserAlerts.value = Pair(type, getNumUnreadUserAlerts())
        }
    }

    /**
     * Checks the Inbox section visibility.
     */
    fun updateInboxSectionVisibility() {
        viewModelScope.launch {
            _state.update {
                it.copy(hasInboxChildren = hasInboxChildren())
            }
        }
    }

    /**
     * Fire a Media Discovery stats event
     */
    fun onMediaDiscoveryOpened(mediaHandle: Long) {
        viewModelScope.launch {
            sendStatisticsMediaDiscovery(mediaHandle)
        }
    }

    /**
     * Set first login status
     */
    fun setIsFirstLogin(newIsFirstLogin: Boolean) {
        savedStateHandle[Companion.isFirstLoginKey] = newIsFirstLogin
    }

    /**
     * Set Inbox Node state in ViewModel initially
     */
    fun setInboxNode() {
        viewModelScope.launch {
            inboxNode = getInboxNode()
        }
    }

    /**
     * Get latest [StorageState]
     */
    fun getStorageState() = monitorStorageStateEvent.getState()

    /**
     * Get Cloud Sort Order
     */
    fun getOrder() = runBlocking { getCloudSortOrder() }


    /**
     * After nodes on Cloud Drive changed or some nodes are moved to rubbish bin,
     * need to check CU and MU folders' status.
     *
     * @param shouldDisable If CU or MU folder is deleted by current client, then CU should be disabled. Otherwise not.
     * @param updatedNodes  Nodes which have changed.
     */
    fun checkCameraUploadFolder(shouldDisable: Boolean, updatedNodes: List<Node>?) {
        viewModelScope.launch {
            val primaryHandle = getPrimarySyncHandle()
            val secondaryHandle = getSecondarySyncHandle()
            updatedNodes?.let {
                val nodeMap = it.associateBy { node -> node.id.longValue }
                // If CU and MU folder don't change then return.
                if (!nodeMap.containsKey(primaryHandle) && !nodeMap.containsKey(secondaryHandle)) {
                    Timber.d("Updated nodes don't include CU/MU, return.")
                    return@launch
                }
            }
            val result = checkCameraUpload(shouldDisable, primaryHandle, secondaryHandle)
            _state.update {
                it.copy(
                    shouldStopCameraUpload = result.shouldStopProcess,
                    shouldSendCameraBroadcastEvent = result.shouldSendEvent,
                )
            }
        }
    }

    /**
     * broadcast upload pause status
     */
    fun broadcastUploadPauseStatus() {
        viewModelScope.launch {
            broadcastUploadPauseState()
        }
    }

    /**
     * Get extended account detail
     */
    fun askForExtendedAccountDetails() {
        Timber.d("askForExtendedAccountDetails")
        viewModelScope.launch {
            getExtendedAccountDetail(
                forceRefresh = true,
                sessions = true,
                purchases = false,
                transactions = false,
            )
        }
    }

    /**
     * Get product accounts
     *
     */
    fun getProductAccounts(): List<Product> = runBlocking { getPricing(false).products }

    /**
     * Ask for full account info
     *
     */
    fun askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo")
        viewModelScope.launch {
            getFullAccountInfo()
        }
    }

    /**
     * Checks if 2FA alert should be shown
     * @param newAccount checks if its a new user
     * @param firstLogin checks if its a first login
     */
    fun checkToShow2FADialog(newAccount: Boolean, firstLogin: Boolean) {
        viewModelScope.launch {
            val result = requireTwoFactorAuthenticationUseCase(newAccount = newAccount, firstLogin = firstLogin)
            _state.update { it.copy(show2FADialog = result) }
        }
    }

    /**
     * Active subscription in local cache
     */
    val activeSubscription: MegaPurchase? get() = getActiveSubscription()

    /**
     * Set last used path of copy/move as target path for next copy/move
     */
    fun setTargetPath(path: Long) {
        viewModelScope.launch {
            setLatestTargetPath(path)
        }
    }

    /**
     * Set the security upgrade alert state
     *
     * @param shouldShow true if the security upgrade alert needs to be shown
     */
    fun setShouldAlertUserAboutSecurityUpgrade(shouldShow: Boolean) {
        _state.update {
            it.copy(shouldAlertUserAboutSecurityUpgrade = shouldShow)
        }
    }

    internal companion object {
        internal const val isFirstLoginKey = "EXTRA_FIRST_LOGIN"
    }
}
