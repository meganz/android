package mega.privacy.android.app.myAccount

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountSubscriptionDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.domain.entity.account.AccountTransferDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.domain.entity.user.UserId
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

/**
 * Tests for [MyAccountUsageComposeViewModel].
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
internal class MyAccountUsageComposeViewModelTest {

    private val fileSizeStringMapper: FileSizeStringMapper = mock()
    private val getFileVersionsOption: GetFileVersionsOption = mock()
    private val checkVersionsUseCase: CheckVersionsUseCase = mock()
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase = mock()
    private val accountDetailFlow = MutableStateFlow(defaultFreeAccountDetail())
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val storageStateFlow = MutableStateFlow(StorageState.Unknown)
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase = mock()
    private val getUsedTransferStatusUseCase: GetUsedTransferStatusUseCase = mock()
    private val monitorBackupFolder: MonitorBackupFolder = mock()
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()

    private lateinit var underTest: MyAccountUsageComposeViewModel

    @BeforeEach
    fun setUp() {
        reset(
            fileSizeStringMapper,
            getFileVersionsOption,
            checkVersionsUseCase,
            getAccountDetailsUseCase,
            monitorAccountDetailUseCase,
            monitorStorageStateUseCase,
            getUsedTransferStatusUseCase,
            monitorBackupFolder,
            getFolderTreeInfo,
            getNodeByIdUseCase,
            getBusinessStatusUseCase,
        )
        whenever(fileSizeStringMapper(any())).thenAnswer { invocation ->
            "fmt:${invocation.getArgument<Long>(0)}"
        }
        accountDetailFlow.value = defaultFreeAccountDetail()
        storageStateFlow.value = StorageState.Unknown

        wheneverBlocking { getAccountDetailsUseCase(any()) }.thenReturn(freeUserAccount())
        wheneverBlocking { getBusinessStatusUseCase() }.thenReturn(BusinessAccountStatus.Active)
        wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(accountDetailFlow)
        wheneverBlocking { monitorStorageStateUseCase() }.thenReturn(storageStateFlow)
        wheneverBlocking { monitorBackupFolder() }.thenReturn(
            flowOf(Result.failure(RuntimeException("test: no backup folder"))),
        )
        wheneverBlocking { getFileVersionsOption(any()) }.thenReturn(false)
        wheneverBlocking { checkVersionsUseCase() }.thenReturn(null)
        wheneverBlocking { getUsedTransferStatusUseCase(any()) }.thenReturn(UsedTransferStatus.NoTransferProblems)

        underTest = MyAccountUsageComposeViewModel(
            fileSizeStringMapper = fileSizeStringMapper,
            getFileVersionsOption = getFileVersionsOption,
            checkVersionsUseCase = checkVersionsUseCase,
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            getUsedTransferStatusUseCase = getUsedTransferStatusUseCase,
            monitorBackupFolder = monitorBackupFolder,
            getFolderTreeInfo = getFolderTreeInfo,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
        )
    }

    @Test
    fun `test that uiState sets usageLoadFailed when getAccountDetailsUseCase throws`() = runTest {
        wheneverBlocking { getAccountDetailsUseCase(any()) }.thenThrow(RuntimeException("bootstrap failed"))

        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitUsageLoadFailed()
            assertThat(state.isUsageContentReady).isFalse()
            assertThat(state.usageLoadFailed).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState sets usageLoadFailed when monitorAccountDetailUseCase throws`() =
        runTest {
            wheneverBlocking { monitorAccountDetailUseCase() }.thenReturn(
                flow { throw RuntimeException("monitor failed") },
            )

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitUsageLoadFailed()
                assertThat(state.isUsageContentReady).isFalse()
                assertThat(state.usageLoadFailed).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState reflects free account from getAccountDetailsUseCase`() = runTest {
        underTest.uiState.test {
            val state = awaitUntilUsageReady()
            assertThat(state.isUsageContentReady).isTrue()
            assertThat(state.isBusinessAccount).isFalse()
            assertThat(state.isProFlexiAccount).isFalse()
            assertThat(state.isMasterBusinessAccount).isFalse()
            assertThat(state.accountType).isEqualTo(AccountType.FREE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState sets paymentAlertType NONE for business sub-account`() = runTest {
        wheneverBlocking { getAccountDetailsUseCase(any()) }.thenReturn(businessSubUserAccount())
        accountDetailFlow.value = accountDetailForBusinessAccount(AccountType.BUSINESS)

        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitUntilUsageReady()
            assertThat(state.isUsageContentReady).isTrue()
            assertThat(state.isBusinessAccount).isTrue()
            assertThat(state.isMasterBusinessAccount).isFalse()
            assertThat(state.paymentAlertType).isEqualTo(PaymentAlertType.None)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState sets paymentAlertType BUSINESS_EXPIRED when business master account is expired`() =
        runTest {
            wheneverBlocking { getAccountDetailsUseCase(any()) }.thenReturn(
                businessMasterUserAccount()
            )
            wheneverBlocking { getBusinessStatusUseCase() }.thenReturn(BusinessAccountStatus.Expired)
            accountDetailFlow.value = accountDetailForBusinessAccount(AccountType.BUSINESS)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitUntilUsageReady()
                assertThat(state.isUsageContentReady).isTrue()
                assertThat(state.isBusinessAccount).isTrue()
                assertThat(state.isMasterBusinessAccount).isTrue()
                assertThat(state.businessStatus).isEqualTo(BusinessAccountStatus.Expired)
                assertThat(state.paymentAlertType).isEqualTo(PaymentAlertType.BusinessExpired)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState sets paymentAlertType BUSINESS_EXPIRED when pro flexi account is expired`() =
        runTest {
            wheneverBlocking { getAccountDetailsUseCase(any()) }.thenReturn(proFlexiUserAccount())
            wheneverBlocking { getBusinessStatusUseCase() }.thenReturn(BusinessAccountStatus.Expired)
            accountDetailFlow.value = accountDetailForBusinessAccount(AccountType.PRO_FLEXI)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitUntilUsageReady()
                assertThat(state.isUsageContentReady).isTrue()
                assertThat(state.isProFlexiAccount).isTrue()
                assertThat(state.businessStatus).isEqualTo(BusinessAccountStatus.Expired)
                assertThat(state.paymentAlertType).isEqualTo(PaymentAlertType.BusinessExpired)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState sets paymentAlertType BUSINESS_GRACE_PERIOD when pro flexi account is in grace period`() =
        runTest {
            wheneverBlocking { getAccountDetailsUseCase(any()) }.thenReturn(proFlexiUserAccount())
            wheneverBlocking { getBusinessStatusUseCase() }.thenReturn(BusinessAccountStatus.GracePeriod)
            accountDetailFlow.value = accountDetailForBusinessAccount(AccountType.PRO_FLEXI)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitUntilUsageReady()
                assertThat(state.isUsageContentReady).isTrue()
                assertThat(state.isProFlexiAccount).isTrue()
                assertThat(state.businessStatus).isEqualTo(BusinessAccountStatus.GracePeriod)
                assertThat(state.paymentAlertType).isEqualTo(PaymentAlertType.BusinessGracePeriod)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState updates storage and cloud fields when account detail changes`() =
        runTest {
            underTest.uiState.test {
                awaitUntilUsageReady()
                accountDetailFlow.value = accountDetailWithStorage(
                    usedStorage = 100L,
                    totalStorage = 1000L,
                    usedCloud = 40L,
                    usedIncoming = 10L,
                    usedRubbish = 5L,
                )
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.usedStorage).isEqualTo("fmt:100")
                assertThat(state.totalStorage).isEqualTo("fmt:1000")
                assertThat(state.usedStoragePercentage).isEqualTo(10)
                assertThat(state.cloudStorage).isEqualTo("fmt:40")
                assertThat(state.incomingStorage).isEqualTo("fmt:10")
                assertThat(state.rubbishStorage).isEqualTo("fmt:5")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState sets ACCOUNT_RENEWS_ON when subscription is valid`() = runTest {
        val renewTime = 9_999L
        accountDetailFlow.value = AccountDetail(
            levelDetail = accountLevelDetail(
                accountType = AccountType.PRO_I,
                subscriptionStatus = SubscriptionStatus.VALID,
                subscriptionRenewTime = renewTime,
                proExpirationTime = 0L,
            ),
            storageDetail = emptyStorageDetail(),
            transferDetail = defaultTransferDetail(),
        )
        underTest.uiState.test {
            val state = awaitUntilUsageReady()
            assertThat(state.hasRenewableSubscription).isTrue()
            assertThat(state.paymentAlertType).isEqualTo(PaymentAlertType.AccountRenewsOn)
            assertThat(state.paymentAlertDate).isEqualTo(renewTime)
            assertThat(state.renewTime).isEqualTo(renewTime)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState sets ACCOUNT_EXPIRES_ON when pro expiration is set`() = runTest {
        val expiry = 50_000L
        accountDetailFlow.value = AccountDetail(
            levelDetail = accountLevelDetail(
                accountType = AccountType.PRO_I,
                subscriptionStatus = SubscriptionStatus.INVALID,
                subscriptionRenewTime = 0L,
                proExpirationTime = expiry,
            ),
            storageDetail = emptyStorageDetail(),
            transferDetail = defaultTransferDetail(),
        )
        underTest.uiState.test {
            val state = awaitUntilUsageReady()
            assertThat(state.hasExpirableSubscription).isTrue()
            assertThat(state.paymentAlertType).isEqualTo(PaymentAlertType.AccountExpiresOn)
            assertThat(state.paymentAlertDate).isEqualTo(expiry)
            assertThat(state.proExpirationTime).isEqualTo(expiry)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState maps storage fields correctly for different storage values`() = runTest {
        underTest.uiState.test {
            awaitUntilUsageReady()
            accountDetailFlow.value = accountDetailWithStorage(
                usedStorage = 500L,
                totalStorage = 2000L,
                usedCloud = 300L,
                usedIncoming = 150L,
                usedRubbish = 50L,
            )
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.usedStorage).isEqualTo("fmt:500")
            assertThat(state.totalStorage).isEqualTo("fmt:2000")
            assertThat(state.usedStoragePercentage).isEqualTo(25)
            assertThat(state.cloudStorage).isEqualTo("fmt:300")
            assertThat(state.incomingStorage).isEqualTo("fmt:150")
            assertThat(state.rubbishStorage).isEqualTo("fmt:50")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState updates storageState when storage monitor emits`() = runTest {
        underTest.uiState.test {
            awaitUntilUsageReady()
            storageStateFlow.value = StorageState.Red
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.storageState).isEqualTo(StorageState.Red)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState updates versionsInfo when checkVersionsUseCase returns versions data`() =
        runTest {
            wheneverBlocking { getFileVersionsOption(any()) }.thenReturn(false)
            val versionsFolderInfo = FolderTreeInfo(
                numberOfFiles = 1,
                numberOfFolders = 1,
                totalCurrentSizeInBytes = 0L,
                numberOfVersions = 3,
                sizeOfPreviousVersionsInBytes = 1024L,
            )
            wheneverBlocking { checkVersionsUseCase() }.thenReturn(versionsFolderInfo)

            val vm = createViewModel()

            vm.uiState.test {
                val state = awaitUntilUsageReady()
                assertThat(state.isFileVersioningEnabled).isTrue()
                assertThat(state.versionsInfo).isEqualTo("fmt:1024")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState updates backupStorage when backup folder size is positive`() = runTest {
        val nodeId = NodeId(42L)
        val folderNode: TypedFolderNode = mock()
        wheneverBlocking { monitorBackupFolder() }.thenReturn(flowOf(Result.success(nodeId)))
        wheneverBlocking { getNodeByIdUseCase(nodeId) }.thenReturn(folderNode)
        wheneverBlocking { getFolderTreeInfo(folderNode) }.thenReturn(
            FolderTreeInfo(
                numberOfFiles = 0,
                numberOfFolders = 0,
                totalCurrentSizeInBytes = 2048L,
                numberOfVersions = 0,
                sizeOfPreviousVersionsInBytes = 0L,
            ),
        )

        val vm = createViewModel()

        vm.uiState.test {
            val state = awaitUntilUsageReady()
            assertThat(state.backupStorageSize).isEqualTo(2048L)
            assertThat(state.backupStorage).isEqualTo("fmt:2048")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel() = MyAccountUsageComposeViewModel(
        fileSizeStringMapper = fileSizeStringMapper,
        getFileVersionsOption = getFileVersionsOption,
        checkVersionsUseCase = checkVersionsUseCase,
        getAccountDetailsUseCase = getAccountDetailsUseCase,
        monitorAccountDetailUseCase = monitorAccountDetailUseCase,
        monitorStorageStateUseCase = monitorStorageStateUseCase,
        getUsedTransferStatusUseCase = getUsedTransferStatusUseCase,
        monitorBackupFolder = monitorBackupFolder,
        getFolderTreeInfo = getFolderTreeInfo,
        getNodeByIdUseCase = getNodeByIdUseCase,
        getBusinessStatusUseCase = getBusinessStatusUseCase,
    )

    private suspend fun ReceiveTurbine<MyAccountUsageUiState>.awaitUntilUsageReady(): MyAccountUsageUiState {
        while (true) {
            val item = awaitItem()
            if (item.isUsageContentReady) return item
        }
    }

    private suspend fun ReceiveTurbine<MyAccountUsageUiState>.awaitUsageLoadFailed(): MyAccountUsageUiState {
        while (true) {
            val item = awaitItem()
            if (item.usageLoadFailed) return item
        }
    }
}

private fun emptyStorageDetail() = AccountStorageDetail(
    usedCloudDrive = 0L,
    usedRubbish = 0L,
    usedIncoming = 0L,
    totalStorage = 0L,
    usedStorage = 0L,
)

private fun defaultTransferDetail() = AccountTransferDetail(
    totalTransfer = 1L,
    usedTransfer = 0L,
)

private fun defaultFreeAccountDetail() = AccountDetail(
    levelDetail = accountLevelDetail(
        accountType = AccountType.FREE,
        subscriptionStatus = SubscriptionStatus.INVALID,
        subscriptionRenewTime = 0L,
        proExpirationTime = 0L,
    ),
    storageDetail = emptyStorageDetail(),
    transferDetail = defaultTransferDetail(),
)

private fun accountDetailForBusinessAccount(accountType: AccountType) = AccountDetail(
    levelDetail = accountLevelDetail(
        accountType = accountType,
        subscriptionStatus = SubscriptionStatus.VALID,
        subscriptionRenewTime = 0L,
        proExpirationTime = 0L,
    ),
    storageDetail = emptyStorageDetail(),
    transferDetail = defaultTransferDetail(),
)

private fun freeUserAccount() = UserAccount(
    userId = UserId(1L),
    email = "a@b.c",
    fullName = null,
    isBusinessAccount = false,
    isMasterBusinessAccount = false,
    accountTypeIdentifier = AccountType.FREE,
    accountTypeString = "Free",
)

private fun businessSubUserAccount() = UserAccount(
    userId = UserId(2L),
    email = "biz@b.c",
    fullName = null,
    isBusinessAccount = true,
    isMasterBusinessAccount = false,
    accountTypeIdentifier = AccountType.BUSINESS,
    accountTypeString = "Business",
)

private fun businessMasterUserAccount() = UserAccount(
    userId = UserId(3L),
    email = "master@b.c",
    fullName = null,
    isBusinessAccount = true,
    isMasterBusinessAccount = true,
    accountTypeIdentifier = AccountType.BUSINESS,
    accountTypeString = "Business",
)

private fun proFlexiUserAccount() = UserAccount(
    userId = UserId(4L),
    email = "proflexi@b.c",
    fullName = null,
    isBusinessAccount = true,
    isMasterBusinessAccount = false,
    accountTypeIdentifier = AccountType.PRO_FLEXI,
    accountTypeString = "Pro Flexi",
)

private fun accountLevelDetail(
    accountType: AccountType,
    subscriptionStatus: SubscriptionStatus,
    subscriptionRenewTime: Long,
    proExpirationTime: Long,
) = AccountLevelDetail(
    accountType = accountType,
    subscriptionStatus = subscriptionStatus,
    subscriptionRenewTime = subscriptionRenewTime,
    accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
    proExpirationTime = proExpirationTime,
    accountPlanDetail = null,
    accountSubscriptionDetailList = listOf(
        AccountSubscriptionDetail(
            subscriptionId = "id",
            subscriptionStatus = subscriptionStatus,
            subscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            paymentMethodType = PaymentMethodType.GOOGLE_WALLET,
            renewalTime = subscriptionRenewTime,
            subscriptionLevel = accountType,
            featuresList = emptyList(),
            isFreeTrial = false,
        ),
    ),
)

private fun accountDetailWithStorage(
    usedStorage: Long,
    totalStorage: Long,
    usedCloud: Long,
    usedIncoming: Long,
    usedRubbish: Long,
) = AccountDetail(
    levelDetail = accountLevelDetail(
        accountType = AccountType.FREE,
        subscriptionStatus = SubscriptionStatus.INVALID,
        subscriptionRenewTime = 0L,
        proExpirationTime = 0L,
    ),
    storageDetail = AccountStorageDetail(
        usedCloudDrive = usedCloud,
        usedRubbish = usedRubbish,
        usedIncoming = usedIncoming,
        totalStorage = totalStorage,
        usedStorage = usedStorage,
    ),
    transferDetail = AccountTransferDetail(
        totalTransfer = 1L,
        usedTransfer = 0L,
    ),
)
