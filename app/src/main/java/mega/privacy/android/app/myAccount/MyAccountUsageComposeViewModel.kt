package mega.privacy.android.app.myAccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import mega.privacy.android.domain.usecase.account.CheckVersionsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.transfers.GetUsedTransferStatusUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for MyAccountUsageComposeFragment.
 *
 * Initial UI: [MyAccountUsageUiState.isUsageContentReady] stays false until account bootstrap
 * (successful [GetAccountDetailsUseCase]), versions slice, first backup slice, and a non-null
 * [AccountDetail.storageDetail] / [AccountDetail.levelDetail] from [MonitorAccountDetailUseCase] are all
 * available — then this flag becomes true so the UI can show real usage, breakdown, and CTA.
 * If bootstrap or account-detail monitoring fails, [MyAccountUsageUiState.usageLoadFailed] is set
 * so the UI can show a message and navigate back (no retry). A new screen instance gets a fresh ViewModel.
 * The Compose usage screen may still keep skeleton placeholders for a minimum duration before
 * swapping to real content. Later emissions from monitors only patch fields; the ready flag stays true.
 *
 * @property fileSizeStringMapper Mapper to format file size
 * @property getFileVersionsOption UseCase to get file versions option
 * @property checkVersionsUseCase UseCase to check versions
 * @property getAccountDetailsUseCase UseCase to get account details
 * @property monitorAccountDetailUseCase UseCase to monitor account details
 * @property monitorStorageStateUseCase UseCase to monitor storage state
 * @property getUsedTransferStatusUseCase UseCase to get transfer status
 * @property monitorBackupFolder UseCase to monitor backup folder
 * @property getFolderTreeInfo UseCase to get folder tree info
 * @property getNodeByIdUseCase UseCase to get node by ID
 * @property getBusinessStatusUseCase UseCase to get business status
 */
@HiltViewModel
internal class MyAccountUsageComposeViewModel @Inject constructor(
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val getFileVersionsOption: GetFileVersionsOption,
    private val checkVersionsUseCase: CheckVersionsUseCase,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val getUsedTransferStatusUseCase: GetUsedTransferStatusUseCase,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val getFolderTreeInfo: GetFolderTreeInfo,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {

    /**
     * Screen state built from bootstrap, monitored account detail, storage state, backup folder, and versions.
     */
    val uiState: StateFlow<MyAccountUsageUiState> = combine(
        getAccountBootstrapFlow(),
        monitorAccountDetailFlow(),
        monitorStorageStateUseCase()
            .onStart { emit(StorageState.Unknown) }
            .catch { Timber.e(it) },
        monitorBackupFolderFlow(),
        getVersionsFlow(),
    ) { bootstrap, accountDetailSlice, storageStateValue, backup, versions ->
        AccountUsageInputs(
            bootstrap = bootstrap,
            accountDetail = accountDetailSlice.detail,
            accountDetailFlowFailed = accountDetailSlice.flowFailed,
            storageState = storageStateValue,
            backup = backup,
            versions = versions,
        )
    }
        .scan(MyAccountUsageUiState()) { prev, inputs ->
            updateToUiState(prev, inputs)
        }
        .catch { e -> Timber.e(e, "MyAccountUsageCompose uiState pipeline failed") }
        .asUiStateFlow(viewModelScope, MyAccountUsageUiState())

    /**
     * Wraps [MonitorAccountDetailUseCase] so [combine] always receives a value: on failure emits an empty
     * [AccountDetail] with [AccountDetailSlice.flowFailed] true (distinct from “not yet loaded”).
     */
    private fun monitorAccountDetailFlow() = monitorAccountDetailUseCase()
        .map { AccountDetailSlice(detail = it, flowFailed = false) }
        .catch { e ->
            Timber.w(e, "Exception monitoring account details")
            emit(AccountDetailSlice(detail = AccountDetail(), flowFailed = true))
        }

    /**
     * One-shot bootstrap from [GetAccountDetailsUseCase] (and business status when applicable).
     * On success emits flags for business / Pro Flexi UI; on failure emits [AccountUsageBootstrap.loadFailed] true
     * so downstream state can show [MyAccountUsageUiState.usageLoadFailed].
     */
    private fun getAccountBootstrapFlow() = flow {
        runCatching {
            getAccountDetailsUseCase(forceRefresh = false)
        }.onSuccess { accountDetails ->
            val isBusinessAccount = accountDetails.isBusinessAccount &&
                    accountDetails.accountTypeIdentifier == AccountType.BUSINESS
            val isProFlexiAccount = accountDetails.isBusinessAccount &&
                    accountDetails.accountTypeIdentifier == AccountType.PRO_FLEXI
            val businessStatus = if (isBusinessAccount || isProFlexiAccount) {
                runCatching {
                    getBusinessStatusUseCase()
                }.getOrNull()
            } else {
                null
            }
            emit(
                AccountUsageBootstrap(
                    isBusinessAccount = isBusinessAccount,
                    isProFlexiAccount = isProFlexiAccount,
                    isMasterBusinessAccount = accountDetails.isMasterBusinessAccount,
                    businessStatus = businessStatus,
                    isLoaded = true,
                    loadFailed = false,
                ),
            )
        }.onFailure {
            Timber.e(it)
            emit(
                AccountUsageBootstrap(
                    isBusinessAccount = false,
                    isProFlexiAccount = false,
                    isMasterBusinessAccount = false,
                    businessStatus = null,
                    isLoaded = false,
                    loadFailed = true,
                ),
            )
        }
    }

    /**
     * Resolves backup folder size from [MonitorBackupFolder]: maps node id to tree size, formats with
     * [fileSizeStringMapper], and falls back to zero/empty on any error so this leg never stalls [combine].
     */
    private fun monitorBackupFolderFlow() = channelFlow {
        monitorBackupFolder()
            .catch { e ->
                Timber.w(e, "Exception monitoring backups folder")
                send(BackupUsageSlice(0L, ""))
            }
            .collect { result ->
                result.fold(
                    onSuccess = { nodeId ->
                        runCatching {
                            when (val node = getNodeByIdUseCase(nodeId)) {
                                is TypedFolderNode -> {
                                    getFolderTreeInfo(node).let { folderTreeInfo ->
                                        val backupSizeInBytes =
                                            folderTreeInfo.totalCurrentSizeInBytes
                                        val backupSize = if (backupSizeInBytes > 0) {
                                            fileSizeStringMapper(backupSizeInBytes)
                                        } else {
                                            ""
                                        }
                                        send(BackupUsageSlice(backupSizeInBytes, backupSize))
                                    }
                                }

                                else -> send(BackupUsageSlice(0L, ""))
                            }
                        }.onFailure {
                            Timber.w(it)
                            send(BackupUsageSlice(0L, ""))
                        }
                    },
                    onFailure = {
                        send(BackupUsageSlice(0L, ""))
                    },
                )
            }
    }

    /**
     * Loads file-version settings and previous-versions size; always emits so [combine] does not wait forever.
     */
    private fun getVersionsFlow() = flow {
        runCatching {
            // Force refresh to get the latest file versioning option from the server.
            getFileVersionsOption(forceRefresh = true)
        }.fold(
            onSuccess = { isDisableFileVersions ->
                val folderTreeInfo = runCatching {
                    checkVersionsUseCase()
                }.getOrNull()
                val versionsInfo = folderTreeInfo?.sizeOfPreviousVersionsInBytes?.let { size ->
                    if (size >= 0) fileSizeStringMapper(size) else ""
                } ?: ""
                emit(
                    VersionsUsageSlice(
                        versionsInfo = versionsInfo,
                        isFileVersioningEnabled = isDisableFileVersions.not(),
                        isLoaded = true,
                    ),
                )
            },
            onFailure = { error ->
                Timber.e(error)
                emit(
                    VersionsUsageSlice(
                        versionsInfo = "",
                        isFileVersioningEnabled = false,
                        isLoaded = true,
                    ),
                )
            },
        )
    }

    /**
     * Calculate payment alert type based on account status
     *
     * @param isBusinessAccount Whether this is a business account
     * @param isProFlexiAccount Whether this is a Pro Flexi account
     * @param isMasterBusinessAccount Whether this is the business master admin (same guard as MyAccountViewUtil.businessOrProFlexiUpdate)
     * @param businessStatus The business account status
     * @param hasRenewableSubscription Whether the account has a renewable subscription
     * @param hasExpirableSubscription Whether the account has an expirable subscription
     * @return The payment alert type to display
     */
    private fun calculatePaymentAlertType(
        isBusinessAccount: Boolean,
        isProFlexiAccount: Boolean,
        isMasterBusinessAccount: Boolean,
        businessStatus: BusinessAccountStatus?,
        hasRenewableSubscription: Boolean,
        hasExpirableSubscription: Boolean,
    ): PaymentAlertType {
        if (isBusinessAccount && !isProFlexiAccount && !isMasterBusinessAccount) {
            return PaymentAlertType.None
        }

        if ((isBusinessAccount || isProFlexiAccount) && businessStatus != null) {
            when (businessStatus) {
                BusinessAccountStatus.Expired -> return PaymentAlertType.BusinessExpired
                BusinessAccountStatus.GracePeriod -> return PaymentAlertType.BusinessGracePeriod
                else -> {}
            }
        }

        return when {
            hasRenewableSubscription -> PaymentAlertType.AccountRenewsOn
            hasExpirableSubscription -> PaymentAlertType.AccountExpiresOn
            else -> PaymentAlertType.None
        }
    }

    /**
     * Maps the latest [AccountUsageInputs] into [MyAccountUsageUiState], preserving
     * [MyAccountUsageUiState.isUsageContentReady] once all prerequisites for the real UI have been met.
     */
    private fun updateToUiState(
        prev: MyAccountUsageUiState,
        inputs: AccountUsageInputs,
    ): MyAccountUsageUiState {
        val bootstrap = inputs.bootstrap
        val accountDetail = inputs.accountDetail
        val levelDetail = accountDetail.levelDetail
        val storageDetail = accountDetail.storageDetail
        val transferDetail = accountDetail.transferDetail

        val hasRenewableSubscription = levelDetail?.accountType != AccountType.FREE
                && levelDetail?.subscriptionStatus == SubscriptionStatus.VALID
                && (levelDetail?.subscriptionRenewTime ?: 0) > 0
        val hasExpirableSubscription = levelDetail?.accountType != AccountType.FREE
                && (levelDetail?.proExpirationTime ?: 0) > 0
        val renewTime = levelDetail?.subscriptionRenewTime ?: 0L
        val proExpirationTime = levelDetail?.proExpirationTime ?: 0L

        val paymentAlertType = calculatePaymentAlertType(
            isBusinessAccount = bootstrap.isBusinessAccount,
            isProFlexiAccount = bootstrap.isProFlexiAccount,
            isMasterBusinessAccount = bootstrap.isMasterBusinessAccount,
            businessStatus = bootstrap.businessStatus,
            hasRenewableSubscription = hasRenewableSubscription,
            hasExpirableSubscription = hasExpirableSubscription,
        )

        val paymentAlertDate = when (paymentAlertType) {
            PaymentAlertType.AccountRenewsOn -> renewTime
            PaymentAlertType.AccountExpiresOn -> proExpirationTime
            else -> 0L
        }

        val usedTransferStatus = transferDetail?.usedTransferPercentage?.let {
            getUsedTransferStatusUseCase(it)
        } ?: prev.usedTransferStatus

        val usageLoadFailed = bootstrap.loadFailed || inputs.accountDetailFlowFailed

        val contentReady = bootstrap.isLoaded &&
                !bootstrap.loadFailed &&
                !inputs.accountDetailFlowFailed &&
                inputs.versions.isLoaded &&
                accountDetail.storageDetail != null &&
                accountDetail.levelDetail != null

        return MyAccountUsageUiState(
            usageLoadFailed = usageLoadFailed,
            isUsageContentReady = prev.isUsageContentReady || contentReady,
            isFileVersioningEnabled = inputs.versions.isFileVersioningEnabled,
            versionsInfo = inputs.versions.versionsInfo,
            accountType = levelDetail?.accountType ?: AccountType.FREE,
            storageState = inputs.storageState,
            isBusinessAccount = bootstrap.isBusinessAccount,
            isProFlexiAccount = bootstrap.isProFlexiAccount,
            isMasterBusinessAccount = bootstrap.isMasterBusinessAccount,
            usedStoragePercentage = storageDetail?.usedPercentage ?: 0,
            usedStorage = storageDetail?.usedStorage?.let { fileSizeStringMapper(it) } ?: "",
            totalStorage = storageDetail?.totalStorage?.let { fileSizeStringMapper(it) } ?: "",
            usedTransferPercentage = transferDetail?.usedTransferPercentage ?: 0,
            usedTransfer = transferDetail?.usedTransfer?.let { fileSizeStringMapper(it) } ?: "",
            totalTransfer = transferDetail?.totalTransfer?.let { fileSizeStringMapper(it) } ?: "",
            usedTransferStatus = usedTransferStatus,
            cloudStorage = storageDetail?.usedCloudDrive?.let { fileSizeStringMapper(it) } ?: "",
            incomingStorage = storageDetail?.usedIncoming?.let { fileSizeStringMapper(it) } ?: "",
            rubbishStorage = storageDetail?.usedRubbish?.let { fileSizeStringMapper(it) } ?: "",
            backupStorageSize = inputs.backup.sizeBytes,
            backupStorage = inputs.backup.formatted,
            renewTime = renewTime,
            proExpirationTime = proExpirationTime,
            hasRenewableSubscription = hasRenewableSubscription,
            hasExpirableSubscription = hasExpirableSubscription,
            businessStatus = bootstrap.businessStatus,
            paymentAlertType = paymentAlertType,
            paymentAlertDate = paymentAlertDate,
        )
    }
}

/**
 * Latest value from the account-detail monitor, plus whether the underlying flow failed and emitted a fallback [detail].
 */
private data class AccountDetailSlice(
    val detail: AccountDetail,
    val flowFailed: Boolean,
)

/** Result of the initial [GetAccountDetailsUseCase] (and related) pass for usage UI routing. */
private data class AccountUsageBootstrap(
    val isBusinessAccount: Boolean,
    val isProFlexiAccount: Boolean,
    val isMasterBusinessAccount: Boolean,
    val businessStatus: BusinessAccountStatus?,
    val isLoaded: Boolean,
    val loadFailed: Boolean,
)

/** Backup folder total size for the usage breakdown row. */
private data class BackupUsageSlice(
    val sizeBytes: Long,
    val formatted: String,
)

/** File versioning toggle and formatted previous-versions size string. */
private data class VersionsUsageSlice(
    val versionsInfo: String,
    val isFileVersioningEnabled: Boolean,
    val isLoaded: Boolean,
)

/**
 * One combined tick of all inputs feeding [MyAccountUsageComposeViewModel.updateToUiState].
 */
private data class AccountUsageInputs(
    val bootstrap: AccountUsageBootstrap,
    val accountDetail: AccountDetail,
    val accountDetailFlowFailed: Boolean,
    val storageState: StorageState,
    val backup: BackupUsageSlice,
    val versions: VersionsUsageSlice,
)
