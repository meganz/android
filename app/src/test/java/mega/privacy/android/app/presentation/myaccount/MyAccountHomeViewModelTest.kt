@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.myaccount

import androidx.compose.ui.unit.sp
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TEST_USER_ACCOUNT
import mega.privacy.android.app.presentation.avatar.mapper.AvatarContentMapper
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.myaccount.mapper.AccountNameMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountTransferDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.verification.Verified
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import java.io.File
import java.util.stream.Stream
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MyAccountHomeViewModelTest {
    private lateinit var underTest: MyAccountHomeViewModel

    private val accountDetailFlow = MutableStateFlow(AccountDetail())
    private val myAvatarFileFlow = MutableStateFlow<File?>(null)
    private val verifiedPhoneNumberFlow =
        MutableStateFlow(
            Verified(
                phoneNumber = VerifiedPhoneNumber.PhoneNumber("123"),
                canRequestUnblockSms = true,
                canRequestOptInVerification = true
            )
        )
    private val connectivityFlow = MutableStateFlow(false)
    private val userUpdatesFlow = MutableStateFlow<UserChanges>(UserChanges.Firstname)
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase = mock()
    private val getUsedTransferStatusUseCase: GetUsedTransferStatusUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock {
        onBlocking { invoke() }.thenReturn(accountDetailFlow)
    }
    private val monitorMyAvatarFile: MonitorMyAvatarFile = mock {
        onBlocking { invoke() }.thenReturn(myAvatarFileFlow)
    }
    private val monitorVerificationStatus: MonitorVerificationStatus = mock {
        onBlocking { invoke() }.thenReturn(verifiedPhoneNumberFlow)
    }
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase = mock {
        onBlocking { invoke() }.thenReturn(connectivityFlow)
    }
    private val monitorUserUpdates: MonitorUserUpdates = mock {
        onBlocking { invoke() }.thenReturn(userUpdatesFlow)
    }
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase = mock {
        onBlocking { invoke() }.thenReturn(emptyList())
    }
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase = mock {
        onBlocking { invoke() }.thenReturn(1)
    }
    private val getInSharesUseCase: GetInSharesUseCase = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val getUserFullNameUseCase: GetUserFullNameUseCase = mock()
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase = mock()
    private val avatarContentMapper: AvatarContentMapper = mock()
    private val isAchievementsEnabled: IsAchievementsEnabled = mock()

    @BeforeEach
    fun setup() {
        initViewModel()
    }

    private fun initViewModel(accountDetailsValue: UserAccount = TEST_USER_ACCOUNT) {
        getAccountDetailsUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(accountDetailsValue)
        }

        getUserFullNameUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(TEST_USER_ACCOUNT.fullName)
        }

        underTest = MyAccountHomeViewModel(
            getAccountDetailsUseCase,
            monitorAccountDetailUseCase,
            monitorMyAvatarFile,
            monitorVerificationStatus,
            monitorConnectivityUseCase,
            monitorUserUpdates,
            getVisibleContactsUseCase,
            getBusinessStatusUseCase,
            getMyAvatarColorUseCase,
            getInSharesUseCase,
            getCurrentUserEmail,
            getUserFullNameUseCase,
            getMyAvatarFileUseCase,
            getUsedTransferStatusUseCase,
            accountNameMapper = AccountNameMapper(),
            avatarContentMapper,
            isAchievementsEnabled
        )
    }

    @Test
    fun `test that isConnectedToNetwork should be updated when monitorConnectivity emits value`() =
        runTest {
            val isConnected = Random.nextBoolean()
            connectivityFlow.emit(isConnected)

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().isConnectedToNetwork).isEqualTo(isConnected)
            }
        }

    @Test
    fun `test that email should be updated when monitorUserUpdates emits Email typed value`() =
        runTest {
            val mockEmail = "Test123@mega.co.nz"
            whenever(getCurrentUserEmail()).thenReturn(mockEmail)

            userUpdatesFlow.emit(UserChanges.Email)

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().email).isEqualTo(mockEmail)
            }
        }

    @ParameterizedTest(name = "test that name should be updated when monitorUserUpdates emits {0} typed value")
    @MethodSource("provideUserUpdatesType")
    fun `test uiState name updates`(expected: UserChanges) =
        runTest {
            val mockName = "Dog"
            whenever(getUserFullNameUseCase(true)).thenReturn(mockName)

            userUpdatesFlow.emit(expected)

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().name).isEqualTo(mockName)
            }
        }

    @Test
    fun `test that avatar should be updated when monitorMyAvatarFile emits avatar file`() =
        runTest {
            val file = mock<File>()
            val expected = mock<TextAvatarContent>()
            whenever(getMyAvatarFileUseCase(any())).thenReturn(file)
            myAvatarFileFlow.emit(file)
            whenever(
                avatarContentMapper(
                    fullName = TEST_USER_ACCOUNT.fullName,
                    localFile = file,
                    backgroundColor = 1,
                    showBorder = false,
                    textSize = 36.sp
                )
            ).thenReturn(expected)

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().avatarContent).isEqualTo(expected)
            }
        }

    @Test
    fun `test that verifiedPhoneNumber should be updated when monitorVerifiedPhoneNumber emits phone number`() =
        runTest {
            val number = Random.nextInt(10000, 100000).toString()
            val expectedPhoneNumber = VerifiedPhoneNumber.PhoneNumber(number)
            val expectedVerification = Random.nextBoolean()
            verifiedPhoneNumberFlow.emit(
                Verified(
                    phoneNumber = VerifiedPhoneNumber.PhoneNumber(number),
                    canRequestUnblockSms = true,
                    canRequestOptInVerification = expectedVerification
                )
            )

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.verifiedPhoneNumber).isEqualTo(expectedPhoneNumber.phoneNumberString)
                assertThat(state.canVerifyPhoneNumber).isEqualTo(expectedVerification)
            }
        }

    @Test
    fun `test that account details state should be updated when monitorAccountDetail emits data`() =
        runTest {
            val expected = AccountDetail()
            accountDetailFlow.emit(expected)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.hasRenewableSubscription).isEqualTo(
                    expected.levelDetail?.subscriptionStatus == SubscriptionStatus.VALID
                            && (expected.levelDetail?.subscriptionRenewTime ?: 0) > 0
                )
                assertThat(state.hasExpireAbleSubscription).isEqualTo(
                    (expected.levelDetail?.proExpirationTime ?: 0) > 0
                )
                assertThat(state.lastSession).isEqualTo(
                    (expected.sessionDetail?.mostRecentSessionTimeStamp) ?: 0
                )
                assertThat(state.usedStorage).isEqualTo(
                    expected.storageDetail?.usedStorage ?: 0,
                )
                assertThat(state.usedStoragePercentage).isEqualTo(
                    expected.storageDetail?.usedPercentage ?: 0
                )
                assertThat(state.usedTransfer).isEqualTo(
                    expected.transferDetail?.usedTransfer ?: 0
                )
                assertThat(state.usedTransferPercentage).isEqualTo(
                    expected.transferDetail?.usedTransferPercentage ?: 0
                )
                assertThat(state.totalStorage).isEqualTo(
                    expected.storageDetail?.totalStorage ?: 0
                )
                assertThat(state.totalTransfer).isEqualTo(
                    expected.transferDetail?.totalTransfer ?: 0
                )
                assertThat(state.subscriptionRenewTime).isEqualTo(
                    expected.levelDetail?.subscriptionRenewTime ?: 0
                )
                assertThat(state.proExpirationTime).isEqualTo(
                    expected.levelDetail?.proExpirationTime ?: 0
                )
            }
        }

    @ParameterizedTest(name = " with usedTransfer as {0} and totalTransfer as {1} and UsedTransferStatus is {2}")
    @MethodSource("provideTransferDetails")
    fun `test that account details state should be updated when monitorAccountDetail emits data related to transfers`(
        usedTransfer: Long,
        totalTransfer: Long,
        usedTransferStatus: UsedTransferStatus,
    ) =
        runTest {
            val accountDetail =
                AccountDetail(transferDetail = AccountTransferDetail(totalTransfer, usedTransfer))
            whenever(accountDetail.transferDetail?.usedTransferPercentage?.let {
                getUsedTransferStatusUseCase(
                    it
                )
            }).thenReturn(
                usedTransferStatus
            )
            accountDetailFlow.emit(accountDetail)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.usedTransferPercentage).isEqualTo(accountDetail.transferDetail?.usedTransferPercentage)
                assertThat(state.usedTransferStatus).isEqualTo(usedTransferStatus)
            }
        }

    @Test
    fun `test that account type should be business when refreshAccountInfo calls returns business type account`() =
        runTest {
            val expected = UserAccount(
                userId = UserId(1),
                email = "asd@mega.co.nz",
                fullName = "test",
                isBusinessAccount = true,
                isMasterBusinessAccount = true,
                accountTypeIdentifier = AccountType.BUSINESS,
                accountTypeString = "business"
            )
            initViewModel(accountDetailsValue = expected)
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.accountType).isEqualTo(expected.accountTypeIdentifier)
                assertThat(state.isBusinessAccount).isEqualTo(expected.isBusinessAccount && expected.accountTypeIdentifier == AccountType.BUSINESS)
                assertThat(state.isBusinessProFlexiStatusActive).isTrue()
            }
        }

    @Test
    fun `test that account type should be Pro Flexi when refreshAccountInfo calls returns Pro Flexi type account`() =
        runTest {
            val expected = UserAccount(
                userId = UserId(1),
                email = "asd@mega.co.nz",
                fullName = "test",
                isBusinessAccount = true,
                isMasterBusinessAccount = false,
                accountTypeIdentifier = AccountType.PRO_FLEXI,
                accountTypeString = "proflexi"
            )
            initViewModel(accountDetailsValue = expected)
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.accountType).isEqualTo(expected.accountTypeIdentifier)
                assertThat(state.isProFlexiAccount).isEqualTo(expected.accountTypeIdentifier == AccountType.PRO_FLEXI)
                assertThat(state.isBusinessProFlexiStatusActive).isTrue()
            }
        }

    @ParameterizedTest(name = "test that isBusinessProFlexiStatusActive should return false when business account status {0}")
    @MethodSource("provideInactiveBusinessAccountType")
    fun `test inactive business account status`(expected: BusinessAccountStatus) =
        runTest {
            initViewModel()
            whenever(getBusinessStatusUseCase()).thenReturn(expected)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isBusinessProFlexiStatusActive).isFalse()
            }
        }

    @Test
    fun `test that isAchievementsAvailable should be true when isAchievementsEnabled returns true`() =
        runTest {
            whenever(isAchievementsEnabled()).thenReturn(true)
            initViewModel()

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().isAchievementsAvailable).isTrue()
            }
        }

    @Test
    fun `test that isAchievementsAvailable should be false when isAchievementsEnabled returns false`() =
        runTest {
            whenever(isAchievementsEnabled()).thenReturn(false)
            initViewModel()

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().isAchievementsAvailable).isFalse()
            }
        }

    @Test
    fun `test that isAchievementsAvailable should be false when isAchievementsEnabled throws exception`() =
        runTest {
            whenever(isAchievementsEnabled()).doThrow(RuntimeException("Test exception"))
            initViewModel()

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().isAchievementsAvailable).isFalse()
            }
        }

    companion object {
        @JvmStatic
        private fun provideUserUpdatesType(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(UserChanges.Firstname),
                Arguments.of(UserChanges.Lastname),
            )
        }

        @JvmStatic
        private fun provideInactiveBusinessAccountType(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(BusinessAccountStatus.Expired),
                Arguments.of(BusinessAccountStatus.GracePeriod),
            )
        }

        @JvmStatic
        private fun provideTransferDetails(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(50L, 100L, UsedTransferStatus.NoTransferProblems),
                Arguments.of(90L, 100L, UsedTransferStatus.AlmostFull),
                Arguments.of(100L, 100L, UsedTransferStatus.Full),
            )
        }


        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}