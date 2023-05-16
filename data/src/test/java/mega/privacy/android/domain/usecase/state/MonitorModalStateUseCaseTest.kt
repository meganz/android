package mega.privacy.android.domain.usecase.state

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.state.ModalState
import mega.privacy.android.domain.entity.verification.UnVerified
import mega.privacy.android.domain.entity.verification.VerificationStatus
import mega.privacy.android.domain.entity.verification.Verified
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.BusinessRepository
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorModalStateUseCaseTest {
    private lateinit var underTest: MonitorModalStateUseCase

    private val monitorVerificationStatus = MutableStateFlow<VerificationStatus>(
        UnVerified(
            canRequestUnblockSms = false,
            canRequestOptInVerification = false
        )
    )

    private val storageStateEvent = mock<StorageStateEvent> {
        on { storageState }.thenReturn(StorageState.Unknown)
    }

    private val monitorStorageState = mock<MonitorStorageStateEventUseCase> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(storageStateEvent)
        )
    }

    private val isFirstLaunchUseCase =
        mock<IsFirstLaunchUseCase> { onBlocking { invoke() }.thenReturn(false) }

    private val requiresTwoFactorAuthentication = mock<RequireTwoFactorAuthenticationUseCase> {
        onBlocking { invoke(any(), any()) }.thenReturn(false)
    }

    private val firsLoginState = MutableStateFlow(false)
    private val askPermissionState = MutableStateFlow(false)
    private val newAccountState = MutableStateFlow(false)
    private val getUpgradeAccount: () -> Boolean? = mock()
    private val getAccountType: () -> Int? = mock()
    private val businessRepository = mock<BusinessRepository>()

    @BeforeEach
    fun setUp() {
        underTest = MonitorModalStateUseCase(
            monitorVerificationStatus = { monitorVerificationStatus },
            monitorStorageStateEventUseCase = monitorStorageState,
            isFirstLaunchUseCase = isFirstLaunchUseCase,
            requireTwoFactorAuthenticationUseCase = requiresTwoFactorAuthentication,
            businessRepository = businessRepository,
        )
    }

    @Test
    internal fun `test that modal state is VerifyPhoneNumber if unverified and canRequestOptInVerification is true`() {
        runTest {
            setSavedStateVariables(firstLogin = true)
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }
            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestUnblockSms = false,
                    canRequestOptInVerification = true
                )
            )
            testScheduler.advanceUntilIdle()

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                assertThat(awaitItem()).isEqualTo(ModalState.VerifyPhoneNumber)
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    internal fun `test that modal state is not VerifyPhoneNumber if a phone number has already been verified`() =
        runTest {
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                Verified(
                    phoneNumber = VerifiedPhoneNumber.PhoneNumber("766543"),
                    canRequestUnblockSms = false,
                    canRequestOptInVerification = true
                )
            )
            testScheduler.advanceUntilIdle()

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                awaitComplete()
            }
        }

    @Test
    internal fun `test that a non free account requiring update returns require update modal state with the account type id`() =
        runTest {
            val expectedAccountType = 3
            setSavedStateVariables(
                upgradeAccount = true,
                accountType = expectedAccountType,
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            )
                .test {
                    assertThat(awaitItem()).isEqualTo(
                        ModalState.UpgradeRequired(
                            expectedAccountType
                        )
                    )
                    awaitComplete()
                }
        }

    @Test
    internal fun `test that a free account requiring update, with a storages state of paywall, and on first login, returns require update modal with null account type id`() {
        runTest {
            setSavedStateVariables(
                upgradeAccount = true,
                accountType = 0,
                firstLogin = true,
            )

            monitorStorageState.stub {
                onBlocking { invoke() }.thenReturn(
                    MutableStateFlow(
                        StorageStateEvent(
                            0L,
                            "",
                            0L,
                            "",
                            EventType.Storage,
                            StorageState.PayWall
                        )
                    )
                )
            }


            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                assertThat(awaitItem()).isEqualTo(
                    ModalState.UpgradeRequired(null)
                )
                cancelAndConsumeRemainingEvents()
            }
        }
    }

    @Test
    internal fun `test that if no update is required and two factor is required, two factor required modal is returned`() =
        runTest {
            setSavedStateVariables()
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(true)
            }

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                assertThat(awaitItem()).isEqualTo(
                    ModalState.RequestTwoFactorAuthentication
                )
                awaitComplete()
            }
        }

    @Test
    internal fun `test that verify phone number modal is returned for a non new account on first launch `() =
        runTest {
            setSavedStateVariables(newAccount = false)
            isFirstLaunchUseCase.stub { onBlocking { invoke() }.thenReturn(true) }
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestOptInVerification = true,
                    canRequestUnblockSms = true
                )
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                assertThat(awaitItem()).isEqualTo(
                    ModalState.VerifyPhoneNumber
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    internal fun `test that verify phone number modal is returned for a non new account on first login `() =
        runTest {
            setSavedStateVariables(
                newAccount = false,
                firstLogin = true
            )
            isFirstLaunchUseCase.stub { onBlocking { invoke() }.thenReturn(false) }
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestOptInVerification = true,
                    canRequestUnblockSms = true
                )
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                assertThat(awaitItem()).isEqualTo(
                    ModalState.VerifyPhoneNumber
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    internal fun `test that verify phone number modal is returned for a non new account when ask permission is true `() =
        runTest {
            setSavedStateVariables(
                newAccount = false,
                askPermission = true
            )
            isFirstLaunchUseCase.stub { onBlocking { invoke() }.thenReturn(false) }
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestOptInVerification = true,
                    canRequestUnblockSms = true
                )
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                assertThat(awaitItem()).isEqualTo(
                    ModalState.VerifyPhoneNumber
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    internal fun `test that request permission modal state is returned and drawer item set to ASK_PERMISSIONS if first launch but can verify phone is false`() =
        runTest {
            setSavedStateVariables(
                newAccount = false,
                askPermission = false
            )
            isFirstLaunchUseCase.stub { onBlocking { invoke() }.thenReturn(true) }
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestOptInVerification = false,
                    canRequestUnblockSms = true
                )
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                val state = awaitItem()
                assertThat(state).isEqualTo(
                    ModalState.RequestInitialPermissions
                )
                awaitComplete()
            }
        }

    @Test
    internal fun `test that request permission modal state is returned and drawer item set to ASK_PERMISSIONS if first launch and a new account`() =
        runTest {
            setSavedStateVariables(
                newAccount = true,
                askPermission = false
            )
            isFirstLaunchUseCase.stub { onBlocking { invoke() }.thenReturn(true) }
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestOptInVerification = true,
                    canRequestUnblockSms = true
                )
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                val state = awaitItem()
                assertThat(state).isEqualTo(
                    ModalState.RequestInitialPermissions
                )
                awaitComplete()
            }
        }

    @Test
    internal fun `test that request permission modal state is returned and drawer item set to ASK_PERMISSIONS if askPermission is true and new account is true`() =
        runTest {
            setSavedStateVariables(
                newAccount = true,
                askPermission = true
            )
            isFirstLaunchUseCase.stub { onBlocking { invoke() }.thenReturn(false) }
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestOptInVerification = true,
                    canRequestUnblockSms = true
                )
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                val state = awaitItem()
                assertThat(state).isEqualTo(
                    ModalState.RequestInitialPermissions
                )
                awaitComplete()
            }
        }

    @Test
    internal fun `test that first login modal state is returned if no other saved instance values are set and can verify us false`() =
        runTest {
            setSavedStateVariables(
                firstLogin = true,
            )
            isFirstLaunchUseCase.stub { onBlocking { invoke() }.thenReturn(false) }
            requiresTwoFactorAuthentication.stub {
                onBlocking { invoke(any(), any()) }.thenReturn(false)
            }

            monitorVerificationStatus.emit(
                UnVerified(
                    canRequestOptInVerification = false,
                    canRequestUnblockSms = false
                )
            )

            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                val state = awaitItem()
                assertThat(state).isEqualTo(
                    ModalState.FirstLogin
                )
                awaitComplete()
            }
        }

    @Test
    internal fun `test that expired business status is returned on first login if business account status is expired`() =
        runTest {
            businessRepository.stub {
                onBlocking { getBusinessStatus() }.thenReturn(BusinessAccountStatus.Expired)
            }
            setSavedStateVariables(firstLogin = true)
            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                val state = awaitItem()
                assertThat(state).isEqualTo(
                    ModalState.ExpiredBusinessAccount
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    internal fun `test that expired business account grace period status is returned on first login if status is grace period and account is master business account`() =
        runTest {
            businessRepository.stub {
                onBlocking { getBusinessStatus() }.thenReturn(BusinessAccountStatus.GracePeriod)
                onBlocking { isMasterBusinessAccount() }.thenReturn(true)
            }
            setSavedStateVariables(firstLogin = true)
            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                val state = awaitItem()
                assertThat(state).isEqualTo(
                    ModalState.ExpiredBusinessAccountGracePeriod
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    internal fun `test that expired business account grace period status is NOT returned if account is not master business account`() =
        runTest {
            businessRepository.stub {
                onBlocking { getBusinessStatus() }.thenReturn(BusinessAccountStatus.GracePeriod)
                onBlocking { isMasterBusinessAccount() }.thenReturn(false)
            }
            setSavedStateVariables(firstLogin = true)
            underTest(
                firsLoginState = firsLoginState,
                askPermissionState = askPermissionState,
                newAccountState = newAccountState,
                getUpgradeAccount = getUpgradeAccount,
                getAccountType = getAccountType,
            ).test {
                val events = cancelAndConsumeRemainingEvents()
                assertThat(events).doesNotContain(Event.Item(ModalState.ExpiredBusinessAccountGracePeriod))
            }
        }

    @Test
    internal fun `test that exception when checking 2FA is not propagated`() = runTest {
        requiresTwoFactorAuthentication.stub {
            onBlocking { invoke(any(), any()) }
                .thenAnswer { throw MegaException(-1, "It's broken") }
        }

        underTest(
            firsLoginState = firsLoginState,
            askPermissionState = askPermissionState,
            newAccountState = newAccountState,
            getUpgradeAccount = getUpgradeAccount,
            getAccountType = getAccountType,
        ).test {
            val events = cancelAndConsumeRemainingEvents()
            assertThat(events).doesNotContain(Event.Item(ModalState.RequestTwoFactorAuthentication))
        }
    }

    private fun setSavedStateVariables(
        upgradeAccount: Boolean = false,
        accountType: Int = 0,
        firstLogin: Boolean = false,
        askPermission: Boolean = false,
        newAccount: Boolean = false,
    ) {

        firsLoginState.tryEmit(firstLogin)
        askPermissionState.tryEmit(askPermission)
        newAccountState.tryEmit(newAccount)
        getUpgradeAccount.stub { on { invoke() }.thenReturn(upgradeAccount) }
        getAccountType.stub { on { invoke() }.thenReturn(accountType) }
    }
}