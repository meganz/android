package mega.privacy.android.app.myAccount

import android.content.Context
import androidx.annotation.StringRes
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.core.sharedcomponents.snackbar.MegaSnackbarDuration
import mega.privacy.android.core.sharedcomponents.snackbar.SnackBarHandler
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.PaymentMethodType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.UserAccount
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.AccountPlanDetail
import mega.privacy.android.domain.entity.account.AccountSubscriptionDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.billing.PaymentMethodFlags
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.verification.VerificationStatus
import mega.privacy.android.domain.exception.account.QueryCancelLinkException
import mega.privacy.android.domain.exception.account.QueryChangeEmailLinkException
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.BroadcastRefreshSessionUseCase
import mega.privacy.android.domain.usecase.account.ChangeEmailUseCase
import mega.privacy.android.domain.usecase.account.CheckVersionsUseCase
import mega.privacy.android.domain.usecase.account.ConfirmCancelAccountUseCase
import mega.privacy.android.domain.usecase.account.ConfirmChangeEmailUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.IsMultiFactorAuthEnabledUseCase
import mega.privacy.android.domain.usecase.account.KillOtherSessionsUseCase
import mega.privacy.android.domain.usecase.account.LegacyCancelSubscriptionsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.account.QueryCancelLinkUseCase
import mega.privacy.android.domain.usecase.account.QueryChangeEmailLinkUseCase
import mega.privacy.android.domain.usecase.account.UpdateCurrentUserName
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.avatar.SetAvatarUseCase
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import mega.privacy.android.domain.usecase.transfers.GetUsedTransferStatusUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import mega.privacy.android.domain.usecase.verification.ResetSMSVerifiedPhoneNumberUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.random.Random

/**
 * Test class for [MyAccountViewModel]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
internal class MyAccountViewModelTest {

    private lateinit var underTest: MyAccountViewModel

    private val context: Context = mock()
    private val myAccountInfo: MyAccountInfo = mock()
    private val megaApi: MegaApiAndroid = mock()
    private val setAvatarUseCase: SetAvatarUseCase = mock()
    private val isMultiFactorAuthEnabledUseCase: IsMultiFactorAuthEnabledUseCase = mock()
    private val checkVersionsUseCase: CheckVersionsUseCase = mock()
    private val killOtherSessionsUseCase: KillOtherSessionsUseCase = mock()
    private val legacyCancelSubscriptionsUseCase: LegacyCancelSubscriptionsUseCase = mock()
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase = mock()
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase = mock()
    private val resetSMSVerifiedPhoneNumberUseCase: ResetSMSVerifiedPhoneNumberUseCase = mock()
    private val getUserDataUseCase: GetUserDataUseCase = mock()
    private val getFileVersionsOption: GetFileVersionsOption = mock()
    private val queryCancelLinkUseCase: QueryCancelLinkUseCase = mock()
    private val queryChangeEmailLinkUseCase: QueryChangeEmailLinkUseCase = mock()
    private val isUrlMatchesRegexUseCase: IsUrlMatchesRegexUseCase = mock()
    private val confirmCancelAccountUseCase: ConfirmCancelAccountUseCase = mock()
    private val confirmChangeEmailUseCase: ConfirmChangeEmailUseCase = mock()
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase = mock()
    private val getExtendedAccountDetail: GetExtendedAccountDetail = mock()
    private val getNumberOfSubscription: GetNumberOfSubscription = mock()
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase = mock()
    private val getCurrentUserFullName: GetCurrentUserFullName = mock()
    private val monitorUserUpdates: MonitorUserUpdates = mock()
    private val changeEmailUseCase: ChangeEmailUseCase = mock()
    private val updateCurrentUserName: UpdateCurrentUserName = mock()
    private val getCurrentUserEmail: GetCurrentUserEmail = mock()
    private val monitorVerificationStatus: MonitorVerificationStatus = mock()
    private val getExportMasterKeyUseCase: GetExportMasterKeyUseCase = mock()
    private val broadcastRefreshSessionUseCase: BroadcastRefreshSessionUseCase = mock()
    private val monitorBackupFolder: MonitorBackupFolder = mock()
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val snackBarHandler: SnackBarHandler = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val getUsedTransferStatusUseCase: GetUsedTransferStatusUseCase = mock()
    private val accountDetailFlow = MutableStateFlow(AccountDetail())
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()

    private val userUpdatesFlow = MutableSharedFlow<UserChanges>()
    private val myAccountUpdateFlow =
        MutableStateFlow(MyAccountUpdate(MyAccountUpdate.Action.UPDATE_ACCOUNT_DETAILS))
    private val verificationStatusFlow = MutableSharedFlow<VerificationStatus>()
    private val backupFolderFlow = MutableSharedFlow<Result<NodeId>>()
    private val storageStateFlow = MutableStateFlow(StorageState.Unknown)
    private val monitorStorageStateUseCase = mock<MonitorStorageStateUseCase> {
        on { invoke() }.thenReturn(storageStateFlow)
    }
    private val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase = mock {
        on { invoke() }.thenReturn(myAccountUpdateFlow)
    }

    @BeforeEach
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)
        initializeStubbing()
        initializeViewModel()
    }

    private suspend fun initializeStubbing() {
        whenever(context.getString(any())).thenReturn("")
        whenever(getNumberOfSubscription(any())).thenReturn(Random.nextLong())
        whenever(
            getCurrentUserFullName(
                forceRefresh = any(),
                defaultFirstName = any(),
                defaultLastName = any(),
            )
        ).thenReturn("name")
        whenever(getCurrentUserEmail()).thenReturn(null)
        whenever(monitorUserUpdates()).thenReturn(userUpdatesFlow)
        whenever(monitorVerificationStatus()).thenReturn(verificationStatusFlow)
        whenever(monitorBackupFolder()).thenReturn(backupFolderFlow)
        whenever(
            getExtendedAccountDetail(
                anyBoolean(),
                anyBoolean(),
                anyBoolean(),
                anyBoolean()
            )
        ).thenReturn(Unit)
        whenever(getPaymentMethodUseCase(anyBoolean())).thenReturn(PaymentMethodFlags(0L))
        whenever(getBusinessStatusUseCase()).thenReturn(BusinessAccountStatus.Active)
        whenever(myAccountInfo.usedFormatted).thenReturn("")
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)
        storageStateFlow.value = StorageState.Unknown
    }

    private fun initializeViewModel() {
        underTest = MyAccountViewModel(
            context = context,
            myAccountInfo = myAccountInfo,
            megaApi = megaApi,
            setAvatarUseCase = setAvatarUseCase,
            isMultiFactorAuthEnabledUseCase = isMultiFactorAuthEnabledUseCase,
            checkVersionsUseCase = checkVersionsUseCase,
            killOtherSessionsUseCase = killOtherSessionsUseCase,
            legacyCancelSubscriptionsUseCase = legacyCancelSubscriptionsUseCase,
            getMyAvatarFileUseCase = getMyAvatarFileUseCase,
            checkPasswordReminderUseCase = checkPasswordReminderUseCase,
            resetSMSVerifiedPhoneNumberUseCase = resetSMSVerifiedPhoneNumberUseCase,
            getUserDataUseCase = getUserDataUseCase,
            getFileVersionsOption = getFileVersionsOption,
            queryCancelLinkUseCase = queryCancelLinkUseCase,
            queryChangeEmailLinkUseCase = queryChangeEmailLinkUseCase,
            isUrlMatchesRegexUseCase = isUrlMatchesRegexUseCase,
            confirmCancelAccountUseCase = confirmCancelAccountUseCase,
            confirmChangeEmailUseCase = confirmChangeEmailUseCase,
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            getExtendedAccountDetail = getExtendedAccountDetail,
            getNumberOfSubscription = getNumberOfSubscription,
            getPaymentMethodUseCase = getPaymentMethodUseCase,
            getCurrentUserFullName = getCurrentUserFullName,
            monitorUserUpdates = monitorUserUpdates,
            changeEmailUseCase = changeEmailUseCase,
            updateCurrentUserName = updateCurrentUserName,
            getCurrentUserEmail = getCurrentUserEmail,
            monitorVerificationStatus = monitorVerificationStatus,
            getExportMasterKeyUseCase = getExportMasterKeyUseCase,
            broadcastRefreshSessionUseCase = broadcastRefreshSessionUseCase,
            monitorBackupFolder = monitorBackupFolder,
            getFolderTreeInfo = getFolderTreeInfo,
            getNodeByIdUseCase = getNodeByIdUseCase,
            ioDispatcher = testDispatcher,
            snackBarHandler = snackBarHandler,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            monitorStorageStateUseCase = monitorStorageStateUseCase,
            getUsedTransferStatusUseCase = getUsedTransferStatusUseCase,
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase
        )
    }

    @Test
    fun `test that action invoked when successfully get user data and the phone number is modified`() =
        runTest {
            // Given
            whenever(resetSMSVerifiedPhoneNumberUseCase()).thenReturn(Unit)
            whenever(getUserDataUseCase()).thenReturn(Unit)

            // When
            underTest.resetPhoneNumber(
                isModify = true,
                snackbarShower = mock()
            )

            // Then
            underTest.state.test {
                assertThat(expectMostRecentItem().shouldNavigateToSmsVerification).isTrue()
            }
        }

    @Test
    fun `test that should not navigate to sms verification after navigated once the state`() =
        runTest {
            // Given
            whenever(resetSMSVerifiedPhoneNumberUseCase()).thenReturn(Unit)
            whenever(getUserDataUseCase()).thenReturn(Unit)

            // When
            underTest.resetPhoneNumber(
                isModify = true,
                snackbarShower = mock()
            )
            underTest.onNavigatedToSmsVerification()

            // Then
            underTest.state.test {
                assertThat(expectMostRecentItem().shouldNavigateToSmsVerification).isFalse()
            }
        }

    @Test
    fun `test that a success snackBar message is shown when successfully get user data and the phone number is not modified`() =
        runTest {
            // Given
            val snackBarShower = mock<SnackbarShower>()

            whenever(resetSMSVerifiedPhoneNumberUseCase()).thenReturn(Unit)
            whenever(getUserDataUseCase()).thenReturn(Unit)

            // When
            underTest.resetPhoneNumber(
                isModify = false,
                snackbarShower = snackBarShower
            )

            // Then
            verify(
                snackBarShower
            ).showSnackbar(
                type = SNACKBAR_TYPE,
                content = context.getString(R.string.remove_phone_number_success),
                chatId = MEGACHAT_INVALID_HANDLE
            )
        }

    @Test
    fun `test that getNumberOfSubscription is called when cancelSubscriptionsUseCase is invoked`() =
        runTest {
            val feedback = "feedback"
            val shouldClearCache = true
            whenever(legacyCancelSubscriptionsUseCase(feedback)).thenReturn(true)
            underTest.cancelSubscriptions(feedback)
            verify(getNumberOfSubscription, times(1)).invoke(shouldClearCache)
        }


    @Test
    fun `test that a failed snackBar message is shown when cancelSubscriptionsUseCase return false`() =
        runTest {
            val feedback = "feedback"
            whenever(legacyCancelSubscriptionsUseCase(feedback)).thenReturn(false)

            underTest.cancelSubscriptions(feedback)

            verify(
                snackBarHandler
            ).postSnackbarMessage(
                resId = R.string.cancel_subscription_error,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that a success snackBar message is shown when cancelSubscriptionsUseCase return true`() =
        runTest {
            val feedback = "feedback"
            whenever(legacyCancelSubscriptionsUseCase(feedback)).thenReturn(true)

            underTest.cancelSubscriptions(feedback)

            verify(
                snackBarHandler
            ).postSnackbarMessage(
                resId = R.string.cancel_subscription_ok,
                snackbarDuration = MegaSnackbarDuration.Long
            )
        }

    @Test
    fun `test that a failed snackBar message is shown when failed to get user data`() =
        runTest {
            // Given
            val snackBarShower = mock<SnackbarShower>()

            whenever(resetSMSVerifiedPhoneNumberUseCase()).thenReturn(Unit)
            whenever(getUserDataUseCase()).thenThrow(RuntimeException())

            // When
            underTest.resetPhoneNumber(
                isModify = Random.nextBoolean(),
                snackbarShower = snackBarShower
            )

            // Then
            verify(
                snackBarShower
            ).showSnackbar(
                type = SNACKBAR_TYPE,
                content = context.getString(R.string.remove_phone_number_fail),
                chatId = MEGACHAT_INVALID_HANDLE
            )
        }

    @Test
    fun `test that an error alert is shown when querying the account cancellation link throws an unrelated account cancellation link exception`() =
        runTest {
            testAccountCancellationError(
                queryCancelLinkException = QueryCancelLinkException.UnrelatedAccountCancellationLink(
                    errorCode = 20,
                    errorString = "The link is unrelated to this account",
                ),
                errorMessageRes = R.string.error_not_logged_with_correct_account,
            )
        }

    @Test
    fun `test that an error alert is shown when querying the account cancellation link throws an expired account cancellation link exception`() =
        runTest {
            testAccountCancellationError(
                queryCancelLinkException = QueryCancelLinkException.ExpiredAccountCancellationLink(
                    errorCode = 25,
                    errorString = "The link has expired",
                ),
                errorMessageRes = R.string.cancel_link_expired,
            )
        }

    @Test
    fun `test that an error alert is shown when querying the account cancellation link throws an unknown exception`() =
        runTest {
            testAccountCancellationError(
                queryCancelLinkException = QueryCancelLinkException.Unknown(
                    errorCode = 30,
                    errorString = "An unexpected issue occurred",
                ),
                errorMessageRes = R.string.invalid_link,
            )
        }

    private suspend fun testAccountCancellationError(
        queryCancelLinkException: QueryCancelLinkException,
        @StringRes errorMessageRes: Int,
    ) {
        whenever(queryCancelLinkUseCase(any())).thenAnswer {
            throw queryCancelLinkException
        }

        underTest.cancelAccount(accountCancellationLink = "link/to/cancel")
        underTest.state.test {
            assertThat(awaitItem().errorMessageRes).isEqualTo(errorMessageRes)
        }
    }

    @Test
    fun `test that an error alert is shown when checking the account cancellation link validity throws an exception`() =
        runTest {
            whenever(queryCancelLinkUseCase(any())).thenReturn("link/to/cancel")
            whenever(
                isUrlMatchesRegexUseCase(
                    url = any(),
                    patterns = any(),
                )
            ).thenThrow(RuntimeException())

            underTest.cancelAccount(accountCancellationLink = "link/to/cancel")
            underTest.state.test {
                assertThat(awaitItem().errorMessageRes).isEqualTo(R.string.general_error_word)
            }
        }

    @Test
    fun `test that an error alert is shown when checking the account cancellation link returns an invalid link`() =
        runTest {
            whenever(queryCancelLinkUseCase(any())).thenReturn("link/to/cancel")
            whenever(
                isUrlMatchesRegexUseCase(
                    url = any(),
                    patterns = any(),
                )
            ).thenReturn(false)

            underTest.cancelAccount(accountCancellationLink = "link/to/cancel")
            underTest.state.test {
                assertThat(awaitItem().errorMessageRes).isEqualTo(R.string.general_error_word)
            }
        }

    @Test
    fun `test that the invalid change email link prompt is shown when querying the change email link throws a link not generated exception`() =
        runTest {
            whenever(queryChangeEmailLinkUseCase(any())).thenAnswer {
                throw QueryChangeEmailLinkException.LinkNotGenerated(
                    errorCode = 20,
                    errorString = "The link was not generated",
                )
            }

            underTest.beginChangeEmailProcess("link/to/change/email")
            underTest.state.test {
                assertThat(awaitItem().showInvalidChangeEmailLinkPrompt).isTrue()
            }
        }

    @Test
    fun `test that an error alert is shown when querying the change email link throws an unknown exception`() =
        runTest {
            whenever(queryChangeEmailLinkUseCase(any())).thenAnswer {
                throw QueryChangeEmailLinkException.Unknown(
                    errorCode = 30,
                    errorString = "An unexpected issue occurred",
                )
            }

            underTest.beginChangeEmailProcess("link/to/change/email")
            underTest.state.test {
                assertThat(awaitItem().errorMessageRes).isEqualTo(R.string.general_error_word)
            }
        }

    @Test
    fun `test that an error alert is shown when checking the change email link validity throws an exception`() =
        runTest {
            whenever(queryChangeEmailLinkUseCase(any())).thenReturn("link/to/change/email")
            whenever(
                isUrlMatchesRegexUseCase(
                    url = any(),
                    patterns = any(),
                )
            ).thenThrow(RuntimeException())

            underTest.beginChangeEmailProcess("change/email/link")
            underTest.state.test {
                assertThat(awaitItem().errorMessageRes).isEqualTo(R.string.general_error_word)
            }
        }

    @Test
    fun `test that an error alert is shown when checking the change email link validity returns an invalid link`() =
        runTest {
            whenever(queryChangeEmailLinkUseCase(any())).thenReturn("link/to/change/email")
            whenever(
                isUrlMatchesRegexUseCase(
                    url = any(),
                    patterns = any(),
                )
            ).thenReturn(false)

            underTest.beginChangeEmailProcess("change/email/link")
            underTest.state.test {
                assertThat(awaitItem().errorMessageRes).isEqualTo(R.string.general_error_word)
            }
        }

    @Test
    fun `test that the change email confirmation is shown`() = runTest {
        whenever(queryChangeEmailLinkUseCase(any())).thenReturn("link/to/change/email")
        whenever(
            isUrlMatchesRegexUseCase(
                url = any(),
                patterns = any(),
            )
        ).thenReturn(true)

        underTest.beginChangeEmailProcess("change/email/link")
        underTest.state.test {
            assertThat(awaitItem().showChangeEmailConfirmation).isTrue()
        }
    }

    @Test
    fun `test that the invalid change email link prompt is hidden`() = runTest {
        underTest.resetInvalidChangeEmailLinkPrompt()

        underTest.state.test {
            assertThat(awaitItem().showInvalidChangeEmailLinkPrompt).isFalse()
        }
    }

    @Test
    fun `test that the change email confirmation is hidden`() = runTest {
        underTest.resetChangeEmailConfirmation()

        underTest.state.test {
            assertThat(awaitItem().showChangeEmailConfirmation).isFalse()
        }
    }

    private fun provideShowNewCancelSubscriptionFeatureParameters() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false)
    )

    private fun provideAccountDetailParameters() = Stream.of(
        Arguments.of(
            AccountType.PRO_I,
            accountDetailsWithValidSubscription(AccountType.PRO_I),
            true
        ),
        Arguments.of(
            AccountType.PRO_II,
            accountDetailsWithValidSubscription(AccountType.PRO_II),
            true
        ),
        Arguments.of(
            AccountType.PRO_III,
            accountDetailsWithValidSubscription(AccountType.PRO_III),
            true
        ),
        Arguments.of(
            AccountType.PRO_LITE,
            accountDetailsWithValidSubscription(AccountType.PRO_LITE),
            true
        ),
        Arguments.of(
            AccountType.PRO_I,
            accountDetailsWithInvalidSubscription(AccountType.PRO_I),
            false
        ),
        Arguments.of(
            AccountType.PRO_II,
            accountDetailsWithInvalidSubscription(AccountType.PRO_II),
            false
        ),
        Arguments.of(
            AccountType.PRO_III,
            accountDetailsWithInvalidSubscription(AccountType.PRO_III),
            false
        ),
        Arguments.of(
            AccountType.PRO_LITE,
            accountDetailsWithInvalidSubscription(AccountType.PRO_LITE),
            false
        ),
        Arguments.of(
            AccountType.PRO_FLEXI,
            accountDetailsWithValidSubscription(AccountType.PRO_FLEXI),
            false
        ),
        Arguments.of(
            AccountType.BUSINESS,
            accountDetailsWithValidSubscription(AccountType.BUSINESS),
            false
        ),
        Arguments.of(
            AccountType.STARTER,
            accountDetailsWithValidSubscription(AccountType.STARTER),
            false
        ),
        Arguments.of(
            AccountType.BASIC,
            accountDetailsWithValidSubscription(AccountType.BASIC),
            false
        ),
        Arguments.of(
            AccountType.ESSENTIAL,
            accountDetailsWithValidSubscription(AccountType.ESSENTIAL),
            false
        ),
        Arguments.of(
            AccountType.PRO_I,
            accountDetailsOneOffPlan(AccountType.PRO_I),
            false
        ),
        Arguments.of(
            AccountType.PRO_II,
            accountDetailsOneOffPlan(AccountType.PRO_II),
            false
        ),
        Arguments.of(
            AccountType.PRO_III,
            accountDetailsOneOffPlan(AccountType.PRO_III),
            true
        ),
        Arguments.of(
            AccountType.PRO_LITE,
            accountDetailsOneOffPlan(AccountType.PRO_LITE),
            false
        ),
        Arguments.of(AccountType.UNKNOWN, AccountDetail(), false),
    )

    @ParameterizedTest(name = "when account type is {0} and account details are {1}, then isProSubscription is {2}")
    @MethodSource("provideAccountDetailParameters")
    fun `test that isProSubscription is updated correctly when account details are provided`(
        accountType: AccountType,
        accountDetails: AccountDetail,
        expected: Boolean,
    ) = runTest {
        val userAccount = UserAccount(
            userId = UserId(0L),
            email = "email",
            fullName = "fullName",
            isBusinessAccount = false,
            isMasterBusinessAccount = false,
            accountTypeIdentifier = accountType,
            accountTypeString = "accountTypeString",
        )

        whenever(getAccountDetailsUseCase(anyBoolean())).thenReturn(userAccount)
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

        accountDetailFlow.emit(accountDetails)

        underTest.refreshAccountInfo()
        underTest.state.test {
            assertThat(awaitItem().isProSubscription).isEqualTo(expected)
        }
    }

    @Test
    fun `test that businessProFlexiStatus is updated correctly when getBusinessStatusUseCase returns the correct value`() =
        runTest {
            val expectedValue = BusinessAccountStatus.Expired
            whenever(getAccountDetailsUseCase(anyBoolean())).thenReturn(mock<UserAccount>())
            whenever(getBusinessStatusUseCase()).thenReturn(expectedValue)
            underTest.refreshAccountInfo()
            underTest.state.test {
                assertThat(awaitItem().businessProFlexiStatus).isEqualTo(expectedValue)
            }
        }

    @Test
    fun `test that subscriptionDetails is updated when monitorAccountDetailUseCase return the correct value`() =
        runTest {
            whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

            accountDetailFlow.emit(expectedAccountDetails)
            initializeViewModel()
            underTest.state.test {
                assertThat(awaitItem().subscriptionDetails).isEqualTo(
                    expectedAccountDetails.levelDetail
                )
            }
        }

    @Test
    fun `test that openTestPasswordScreenEvent is updated when logout is invoked and password reminder is true`() =
        runTest {
            whenever(checkPasswordReminderUseCase(true)).thenReturn(true)
            underTest.logout()
            underTest.state.test {
                assertThat(awaitItem().openTestPasswordScreenEvent).isTrue()
            }
        }

    @Test
    fun `test that showLogoutConfirmationDialog is updated when logout is invoked and password reminder is false`() =
        runTest {
            whenever(checkPasswordReminderUseCase(true)).thenReturn(false)
            underTest.logout()
            underTest.state.test {
                assertThat(awaitItem().showLogoutConfirmationDialog).isTrue()
            }
        }

    private fun provideAccountTypeParameters() = Stream.of(
        Arguments.of(accountDetailsWithValidSubscription(AccountType.PRO_I), AccountType.PRO_I),
        Arguments.of(accountDetailsWithValidSubscription(AccountType.PRO_II), AccountType.PRO_II),
        Arguments.of(accountDetailsWithValidSubscription(AccountType.PRO_III), AccountType.PRO_III),
        Arguments.of(
            accountDetailsWithValidSubscription(AccountType.PRO_LITE),
            AccountType.PRO_LITE
        ),
        Arguments.of(
            accountDetailsWithValidSubscription(AccountType.PRO_FLEXI),
            AccountType.PRO_FLEXI
        ),
        Arguments.of(
            accountDetailsWithInvalidSubscription(AccountType.BUSINESS),
            AccountType.BUSINESS
        ),
        Arguments.of(accountDetailsWithValidSubscription(AccountType.STARTER), AccountType.STARTER),
        Arguments.of(accountDetailsWithValidSubscription(AccountType.BASIC), AccountType.BASIC),
        Arguments.of(
            accountDetailsWithValidSubscription(AccountType.ESSENTIAL),
            AccountType.ESSENTIAL
        ),
        Arguments.of(AccountDetail(), AccountType.FREE),
    )

    @ParameterizedTest(name = "when account details are {1}, then accountType is {2}")
    @MethodSource("provideAccountTypeParameters")
    fun `test that accountType is updated correctly when account details are provided`(
        accountDetails: AccountDetail,
        expected: AccountType,
    ) = runTest {
        whenever(monitorAccountDetailUseCase()).thenReturn(accountDetailFlow)

        accountDetailFlow.emit(accountDetails)

        initializeViewModel()
        underTest.state.test {
            assertThat(awaitItem().accountType).isEqualTo(expected)
        }
    }

    @ParameterizedTest(name = " when usedTransferPercentage is {0} and usedTransferStatus is {1}")
    @MethodSource("provideTransferDetails")
    fun `test that used transfer status and percentage is returned correctly`(
        usedTransferPercentage: Int,
        usedTransferStatus: UsedTransferStatus,
    ) = runTest {
        whenever(myAccountInfo.usedTransferPercentage).thenReturn(usedTransferPercentage)
        whenever(getUsedTransferStatusUseCase(usedTransferPercentage)).thenReturn(usedTransferStatus)

        initializeViewModel()
        assertThat(underTest.getUsedTransferPercentage()).isEqualTo(usedTransferPercentage)
        assertThat(underTest.getUsedTransferStatus()).isEqualTo(usedTransferStatus)
    }

    private fun provideTransferDetails(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(50, UsedTransferStatus.NoTransferProblems),
            Arguments.of(90, UsedTransferStatus.AlmostFull),
            Arguments.of(100, UsedTransferStatus.Full),
        )
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(StorageState::class)
    fun `test that storage state should be updated when monitorStorageState emits`(state: StorageState) =
        runTest {
            storageStateFlow.emit(state)

            advanceUntilIdle()

            underTest.state.test {
                assertThat(awaitItem().storageState).isEqualTo(state)
            }
        }

    @Test
    fun `test that monitor my account update emits the correct value`() = runTest {
        val myAccountUpdate = MyAccountUpdate(MyAccountUpdate.Action.STORAGE_STATE_CHANGED)

        myAccountUpdateFlow.emit(myAccountUpdate)
        advanceUntilIdle()
        underTest.monitorMyAccountUpdate.test {
            assertThat(awaitItem()).isEqualTo(myAccountUpdate)
        }
    }

    private val expectedSubscriptionRenewTime = 1873874783274L
    private val expectedProExpirationTime = 378672463728467L
    private val expectedSubscriptionId = "subscriptionId"
    private val expectedAccountType = AccountType.PRO_II
    private fun expectedAccountPlanDetail(accountType: AccountType) = AccountPlanDetail(
        accountType = accountType,
        isProPlan = true,
        expirationTime = expectedProExpirationTime,
        subscriptionId = expectedSubscriptionId,
        featuresList = listOf("vpn", "pwm"),
        isFreeTrial = false,
    )

    private val expectedAccountPlanDetailOneOff = AccountPlanDetail(
        accountType = AccountType.PRO_III,
        isProPlan = true,
        expirationTime = expectedProExpirationTime,
        subscriptionId = "",
        featuresList = listOf("vpn", "pwm"),
        isFreeTrial = false,
    )

    private fun expectedAccountSubscriptionDetail(accountType: AccountType) =
        AccountSubscriptionDetail(
            subscriptionId = expectedSubscriptionId,
            subscriptionStatus = SubscriptionStatus.VALID,
            subscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            subscriptionLevel = accountType,
            paymentMethodType = PaymentMethodType.STRIPE2,
            renewalTime = expectedSubscriptionRenewTime,
            featuresList = listOf("vpn", "pwm"),
            isFreeTrial = false,
        )

    private val expectedAccountDetails = AccountDetail(
        storageDetail = null,
        sessionDetail = null,
        transferDetail = null,
        levelDetail = AccountLevelDetail(
            accountType = expectedAccountType,
            subscriptionStatus = SubscriptionStatus.VALID,
            subscriptionRenewTime = expectedSubscriptionRenewTime,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            proExpirationTime = expectedProExpirationTime,
            accountPlanDetail = null,
            accountSubscriptionDetailList = listOf()
        )
    )

    private fun accountDetailsWithValidSubscription(accountType: AccountType) = AccountDetail(
        storageDetail = null,
        sessionDetail = null,
        transferDetail = null,
        levelDetail = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = SubscriptionStatus.VALID,
            subscriptionRenewTime = expectedSubscriptionRenewTime,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            proExpirationTime = expectedProExpirationTime,
            accountPlanDetail = expectedAccountPlanDetail(accountType),
            accountSubscriptionDetailList = listOf(expectedAccountSubscriptionDetail(accountType))
        )
    )

    private fun accountDetailsWithInvalidSubscription(accountType: AccountType) = AccountDetail(
        storageDetail = null,
        sessionDetail = null,
        transferDetail = null,
        levelDetail = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = SubscriptionStatus.INVALID,
            subscriptionRenewTime = expectedSubscriptionRenewTime,
            accountSubscriptionCycle = AccountSubscriptionCycle.MONTHLY,
            proExpirationTime = expectedProExpirationTime,
            accountPlanDetail = expectedAccountPlanDetail(accountType),
            accountSubscriptionDetailList = listOf()
        )
    )

    private fun accountDetailsOneOffPlan(accountType: AccountType) = AccountDetail(
        storageDetail = null,
        sessionDetail = null,
        transferDetail = null,
        levelDetail = AccountLevelDetail(
            accountType = accountType,
            subscriptionStatus = SubscriptionStatus.NONE,
            subscriptionRenewTime = expectedSubscriptionRenewTime,
            accountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
            proExpirationTime = expectedProExpirationTime,
            accountPlanDetail = expectedAccountPlanDetailOneOff,
            accountSubscriptionDetailList = listOf(expectedAccountSubscriptionDetail(accountType))
        )
    )

    @AfterEach
    fun resetMocks() {
        Dispatchers.resetMain()
        reset(
            context,
            myAccountInfo,
            megaApi,
            setAvatarUseCase,
            isMultiFactorAuthEnabledUseCase,
            checkVersionsUseCase,
            killOtherSessionsUseCase,
            legacyCancelSubscriptionsUseCase,
            getMyAvatarFileUseCase,
            checkPasswordReminderUseCase,
            resetSMSVerifiedPhoneNumberUseCase,
            getUserDataUseCase,
            getFileVersionsOption,
            queryCancelLinkUseCase,
            queryChangeEmailLinkUseCase,
            isUrlMatchesRegexUseCase,
            confirmCancelAccountUseCase,
            confirmChangeEmailUseCase,
            getAccountDetailsUseCase,
            getExtendedAccountDetail,
            getNumberOfSubscription,
            getPaymentMethodUseCase,
            getCurrentUserFullName,
            monitorUserUpdates,
            changeEmailUseCase,
            updateCurrentUserName,
            getCurrentUserEmail,
            monitorVerificationStatus,
            getExportMasterKeyUseCase,
            broadcastRefreshSessionUseCase,
            monitorBackupFolder,
            getFolderTreeInfo,
            getNodeByIdUseCase,
            snackBarHandler,
            getBusinessStatusUseCase,
            monitorAccountDetailUseCase,
            getUsedTransferStatusUseCase,
        )
    }
}
