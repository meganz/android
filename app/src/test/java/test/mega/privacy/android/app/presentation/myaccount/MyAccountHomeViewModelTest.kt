@file:OptIn(ExperimentalCoroutinesApi::class)

package test.mega.privacy.android.app.presentation.myaccount

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.myaccount.MyAccountHomeViewModel
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.verification.Verified
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarColorUseCase
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.GetUserFullNameUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.MonitorAccountDetail
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.shares.GetInSharesUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.TEST_USER_ACCOUNT
import java.io.File
import java.util.stream.Stream
import kotlin.random.Random

@ExtendWith(InstantTaskExecutorExtension::class)
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
    private val userUpdatesFlow = MutableStateFlow(UserChanges.Firstname)


    private val getAccountDetailsUseCase: GetAccountDetailsUseCase = mock()
    private val monitorAccountDetail: MonitorAccountDetail = mock {
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
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val getMyAvatarColorUseCase: GetMyAvatarColorUseCase = mock()
    private val getInSharesUseCase: GetInSharesUseCase = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val getUserFullNameUseCase: GetUserFullNameUseCase = mock()
    private val getMyAvatarFile: GetMyAvatarFile = mock()
    private val getAccountAchievements: GetAccountAchievements = mock()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())

        underTest = MyAccountHomeViewModel(
            getAccountDetailsUseCase,
            monitorAccountDetail,
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
            getMyAvatarFile,
            getAccountAchievements
        )
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
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
            val expected = mock<File>()
            myAvatarFileFlow.emit(expected)

            advanceUntilIdle()

            underTest.uiState.test {
                assertThat(awaitItem().avatar).isEqualTo(expected)
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
            whenever(getAccountDetailsUseCase(false)).thenReturn(expected)
            whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.accountType).isEqualTo(expected.accountTypeIdentifier)
                assertThat(state.isBusinessAccount).isEqualTo(expected.isBusinessAccount && expected.accountTypeIdentifier == AccountType.BUSINESS)
                assertThat(state.isBusinessStatusActive).isTrue()
            }
        }

    @ParameterizedTest(name = "test that isBusinessStatusActive should return false when business account status {0}")
    @MethodSource("provideInactiveBusinessAccountType")
    fun `test inactive business account status`(expected: BusinessAccountStatus) =
        runTest {
            whenever(getAccountDetailsUseCase(false)).thenReturn(TEST_USER_ACCOUNT)
            whenever(getBusinessStatusUseCase()).thenReturn(expected)

            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isBusinessStatusActive).isFalse()
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
    }
}