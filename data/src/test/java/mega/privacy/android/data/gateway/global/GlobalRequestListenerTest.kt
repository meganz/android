package mega.privacy.android.data.gateway.global

import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.facade.AccountInfoWrapper
import mega.privacy.android.data.facade.security.SetLogoutFlagWrapper
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.usecase.IsUseHttpsEnabledUseCase
import mega.privacy.android.domain.usecase.SetUseHttpsUseCase
import mega.privacy.android.domain.usecase.account.GetFullAccountInfoUseCase
import mega.privacy.android.domain.usecase.account.ResetAccountDetailsTimeStampUseCase
import mega.privacy.android.domain.usecase.account.SetLoggedOutFromAnotherLocationUseCase
import mega.privacy.android.domain.usecase.account.SetUnverifiedBusinessAccountUseCase
import mega.privacy.android.domain.usecase.backup.SetupDeviceNameUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.chat.UpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.chat.link.IsRichPreviewsEnabledUseCase
import mega.privacy.android.domain.usecase.chat.link.ShouldShowRichLinkWarningUseCase
import mega.privacy.android.domain.usecase.login.BroadcastFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.network.BroadcastSslVerificationFailedUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GlobalRequestListenerTest {

    private lateinit var underTest: GlobalRequestListener

    // Dependencies
    private val appEventGateway = mock<AppEventGateway>()
    private val setLogoutFlagWrapper = mock<SetLogoutFlagWrapper>()
    private val userAttributeDatabaseUpdater = mock<UserAttributeDatabaseUpdater>()
    private val setupMegaChatApiWrapper = mock<SetupMegaChatApiWrapper>()
    private val accountInfoWrapper = mock<AccountInfoWrapper>()
    private val megaChatApi = mock<MegaChatApiAndroid>()
    private val dbH = mock<Lazy<DatabaseHandler>>()
    private val databaseHandler = mock<DatabaseHandler>()
    private val megaApi = mock<MegaApiAndroid>()
    private val applicationScope = TestScope(UnconfinedTestDispatcher())
    private val getFullAccountInfoUseCase = mock<GetFullAccountInfoUseCase>()
    private val broadcastFetchNodesFinishUseCase = mock<BroadcastFetchNodesFinishUseCase>()
    private val localLogoutAppUseCase = mock<LocalLogoutAppUseCase>()
    private val setupDeviceNameUseCase = mock<SetupDeviceNameUseCase>()
    private val broadcastBusinessAccountExpiredUseCase =
        mock<BroadcastBusinessAccountExpiredUseCase>()
    private val loginMutex = mock<Mutex>()
    private val updatePushNotificationSettingsUseCase =
        mock<UpdatePushNotificationSettingsUseCase>()
    private val shouldShowRichLinkWarningUseCase = mock<ShouldShowRichLinkWarningUseCase>()
    private val isRichPreviewsEnabledUseCase = mock<IsRichPreviewsEnabledUseCase>()
    private val isUseHttpsEnabledUseCase = mock<IsUseHttpsEnabledUseCase>()
    private val setUseHttpsUseCase = mock<SetUseHttpsUseCase>()
    private val resetAccountDetailsTimeStampUseCase = mock<ResetAccountDetailsTimeStampUseCase>()
    private val broadcastSslVerificationFailedUseCase =
        mock<BroadcastSslVerificationFailedUseCase>()
    private val setLoggedOutFromAnotherLocationUseCase =
        mock<SetLoggedOutFromAnotherLocationUseCase>()
    private val setIsUnverifiedBusinessAccountUseCase = mock<SetUnverifiedBusinessAccountUseCase>()

    // Test data
    private val testEmail = "test@example.com"
    private val testFirstName = "John"
    private val testLastName = "Doe"
    private val testUserHandle = 12345L
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        whenever(dbH.get()).thenReturn(databaseHandler)
        whenever(databaseHandler.myChatFilesFolderHandle).thenReturn(MegaApiJava.INVALID_HANDLE)

        underTest = GlobalRequestListener(
            appEventGateway = appEventGateway,
            setLogoutFlagWrapper = setLogoutFlagWrapper,
            userAttributeDatabaseUpdater = userAttributeDatabaseUpdater,
            setupMegaChatApiWrapper = setupMegaChatApiWrapper,
            accountInfoWrapper = accountInfoWrapper,
            megaChatApi = megaChatApi,
            dbH = dbH,
            megaApi = megaApi,
            applicationScope = applicationScope,
            getFullAccountInfoUseCase = getFullAccountInfoUseCase,
            broadcastFetchNodesFinishUseCase = broadcastFetchNodesFinishUseCase,
            localLogoutAppUseCase = localLogoutAppUseCase,
            setupDeviceNameUseCase = setupDeviceNameUseCase,
            broadcastBusinessAccountExpiredUseCase = broadcastBusinessAccountExpiredUseCase,
            loginMutex = loginMutex,
            updatePushNotificationSettingsUseCase = updatePushNotificationSettingsUseCase,
            shouldShowRichLinkWarningUseCase = shouldShowRichLinkWarningUseCase,
            isRichPreviewsEnabledUseCase = isRichPreviewsEnabledUseCase,
            isUseHttpsEnabledUseCase = isUseHttpsEnabledUseCase,
            setUseHttpsUseCase = setUseHttpsUseCase,
            resetAccountDetailsTimeStampUseCase = resetAccountDetailsTimeStampUseCase,
            broadcastSslVerificationFailedUseCase = broadcastSslVerificationFailedUseCase,
            setLoggedOutFromAnotherLocationUseCase = setLoggedOutFromAnotherLocationUseCase,
            setIsUnverifiedBusinessAccountUseCase = setIsUnverifiedBusinessAccountUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        reset(
            appEventGateway, setLogoutFlagWrapper, userAttributeDatabaseUpdater,
            setupMegaChatApiWrapper, accountInfoWrapper, megaChatApi, dbH,
            databaseHandler, megaApi, getFullAccountInfoUseCase,
            broadcastFetchNodesFinishUseCase, localLogoutAppUseCase,
            setupDeviceNameUseCase, broadcastBusinessAccountExpiredUseCase,
            loginMutex, updatePushNotificationSettingsUseCase,
            shouldShowRichLinkWarningUseCase, isRichPreviewsEnabledUseCase,
            isUseHttpsEnabledUseCase, setUseHttpsUseCase,
            resetAccountDetailsTimeStampUseCase, broadcastSslVerificationFailedUseCase,
            setLoggedOutFromAnotherLocationUseCase, setIsUnverifiedBusinessAccountUseCase
        )
    }

    @Test
    fun `test onRequestStart logs request string`() = runTest {
        val request = mock<MegaRequest> {
            on { requestString } doReturn "test_request"
        }
        val api = mock<MegaApiJava>()

        underTest.onRequestStart(api, request)

        // Verify logging (Timber calls are not directly verifiable in unit tests)
        // This test ensures the method doesn't crash
    }

    @Test
    fun `test onRequestUpdate logs request string`() = runTest {
        val request = mock<MegaRequest> {
            on { requestString } doReturn "test_request"
        }
        val api = mock<MegaApiJava>()

        underTest.onRequestUpdate(api, request)

        // Verify logging (Timber calls are not directly verifiable in unit tests)
        // This test ensures the method doesn't crash
    }

    @Test
    fun `test onRequestTemporaryError logs request string`() = runTest {
        val request = mock<MegaRequest> {
            on { requestString } doReturn "test_request"
        }
        val api = mock<MegaApiJava>()
        val error = mock<MegaError>()

        underTest.onRequestTemporaryError(api, request, error)

        // Verify logging (Timber calls are not directly verifiable in unit tests)
        // This test ensures the method doesn't crash
    }

    @Test
    fun `test onRequestFinish with API_EPAYWALL broadcasts transfer over quota`() = runTest {
        val request = mock<MegaRequest>()
        val api = mock<MegaApiJava>()
        val error = mock<MegaError> {
            on { errorCode } doReturn MegaError.API_EPAYWALL
        }

        underTest.onRequestFinish(api, request, error)
        advanceUntilIdle()

        verify(appEventGateway).broadcastTransferOverQuota(true)
    }

    @Test
    fun `test onRequestFinish with API_ESUBUSERKEYMISSING sets unverified business account`() =
        runTest {
            val request = mock<MegaRequest>()
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_ESUBUSERKEYMISSING
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(setIsUnverifiedBusinessAccountUseCase).invoke(true)
        }

    @Test
    fun `test onRequestFinish with API_EBUSINESSPASTDUE broadcasts business account expired`() =
        runTest {
            val request = mock<MegaRequest>()
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_EBUSINESSPASTDUE
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(broadcastBusinessAccountExpiredUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with LOGOUT request and API_EINCOMPLETE with SSL error broadcasts SSL verification failed`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_LOGOUT
                on { paramType } doReturn MegaError.API_ESSL
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_EINCOMPLETE
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(broadcastSslVerificationFailedUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with LOGOUT request and API_ESID resets account and logs out`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_LOGOUT
                on { paramType } doReturn MegaError.API_ESID
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_ESID
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(accountInfoWrapper).resetAccountInfo()
            verify(setLoggedOutFromAnotherLocationUseCase).invoke(true)
            verify(localLogoutAppUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with LOGOUT request and API_EBLOCKED calls local logout`() = runTest {
        val request = mock<MegaRequest> {
            on { type } doReturn MegaRequest.TYPE_LOGOUT
            on { paramType } doReturn MegaError.API_EBLOCKED
        }
        val error = mock<MegaError> {
            on { errorCode } doReturn MegaError.API_EBLOCKED
        }

        underTest.onRequestFinish(mock<MegaApiJava>(), request, error)

        verify(megaApi).localLogout()
        verify(megaChatApi).logout()
    }

    @Test
    fun `test onRequestFinish with LOGOUT request and API_OK sets logout flag to false`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_LOGOUT
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }

            underTest.onRequestFinish(api, request, error)

            verify(setLogoutFlagWrapper).invoke(false)
        }

    @Test
    fun `test onRequestFinish with FETCH_NODES request unlocks login mutex and broadcasts finish`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_FETCH_NODES
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(loginMutex).unlock()
            verify(broadcastFetchNodesFinishUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with FETCH_NODES request and API_OK sets up HTTPS and resets timestamp`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_FETCH_NODES
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }
            whenever(isUseHttpsEnabledUseCase.invoke()).doReturn(true)

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(setUseHttpsUseCase).invoke(true)
            verify(resetAccountDetailsTimeStampUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with FETCH_NODES request and API_OK gets full account info`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_FETCH_NODES
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(getFullAccountInfoUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with FETCH_NODES request and API_OK checks rich link settings`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_FETCH_NODES
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(shouldShowRichLinkWarningUseCase).invoke()
            verify(isRichPreviewsEnabledUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with FETCH_NODES request and API_OK sets up device name and push notifications`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_FETCH_NODES
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            verify(setupDeviceNameUseCase).invoke()
            verify(updatePushNotificationSettingsUseCase).invoke()
        }

    @Test
    fun `test onRequestFinish with FETCH_NODES request and API_OK calls setupMegaChatApiWrapper`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_FETCH_NODES
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }

            underTest.onRequestFinish(api, request, error)

            verify(setupMegaChatApiWrapper).invoke()
        }

    @Test
    fun `test onRequestFinish with GET_ATTR_USER request for first name updates non-contact in database`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_GET_ATTR_USER
                on { paramType } doReturn MegaApiJava.USER_ATTR_FIRSTNAME
                on { email } doReturn testEmail
                on { text } doReturn testFirstName
            }
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }
            val user = mock<MegaUser> {
                on { handle } doReturn testUserHandle
                on { visibility } doReturn MegaUser.VISIBILITY_UNKNOWN
            }

            whenever(megaApi.getContact(testEmail)).doReturn(user)

            underTest.onRequestFinish(mock<MegaApiJava>(), request, error)

            verify(databaseHandler).setNonContactEmail(testEmail, testUserHandle.toString())
            verify(databaseHandler).setNonContactFirstName(testFirstName, testUserHandle.toString())
        }

    @Test
    fun `test onRequestFinish with GET_ATTR_USER request for last name updates non-contact in database`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_GET_ATTR_USER
                on { paramType } doReturn MegaApiJava.USER_ATTR_LASTNAME
                on { email } doReturn testEmail
                on { text } doReturn testLastName
            }
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }
            val user = mock<MegaUser> {
                on { handle } doReturn testUserHandle
                on { visibility } doReturn MegaUser.VISIBILITY_UNKNOWN
            }

            whenever(megaApi.getContact(testEmail)).doReturn(user)

            underTest.onRequestFinish(mock<MegaApiJava>(), request, error)

            verify(databaseHandler).setNonContactLastName(testLastName, testUserHandle.toString())
        }

    @Test
    fun `test onRequestFinish with GET_ATTR_USER request for visible contact does not update database`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_GET_ATTR_USER
                on { paramType } doReturn MegaApiJava.USER_ATTR_FIRSTNAME
                on { email } doReturn testEmail
                on { text } doReturn testFirstName
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }
            val user = mock<MegaUser> {
                on { handle } doReturn testUserHandle
                on { visibility } doReturn MegaUser.VISIBILITY_VISIBLE
            }

            whenever(api.getContact(testEmail)).doReturn(user)

            underTest.onRequestFinish(api, request, error)

            verify(databaseHandler, never()).setNonContactEmail(any(), any())
            verify(databaseHandler, never()).setNonContactFirstName(any(), any())
        }

    @Test
    fun `test onRequestFinish with GET_ATTR_USER request when user is null does not update database`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_GET_ATTR_USER
                on { paramType } doReturn MegaApiJava.USER_ATTR_FIRSTNAME
                on { email } doReturn testEmail
                on { text } doReturn testFirstName
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_OK
            }

            whenever(api.getContact(testEmail)).doReturn(null)

            underTest.onRequestFinish(api, request, error)

            verify(databaseHandler, never()).setNonContactEmail(any(), any())
            verify(databaseHandler, never()).setNonContactFirstName(any(), any())
        }

    @Test
    fun `test onRequestFinish with GET_ATTR_USER request when error is not API_OK does not update database`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_GET_ATTR_USER
                on { paramType } doReturn MegaApiJava.USER_ATTR_FIRSTNAME
                on { email } doReturn testEmail
                on { text } doReturn testFirstName
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_EARGS
            }

            underTest.onRequestFinish(api, request, error)

            verify(databaseHandler, never()).setNonContactEmail(any(), any())
            verify(databaseHandler, never()).setNonContactFirstName(any(), any())
        }

    @Test
    fun `test onRequestFinish with unknown request type does nothing`() = runTest {
        val request = mock<MegaRequest> {
            on { type } doReturn 999 // Unknown type
        }
        val api = mock<MegaApiJava>()
        val error = mock<MegaError> {
            on { errorCode } doReturn MegaError.API_OK
        }

        underTest.onRequestFinish(api, request, error)

        // Verify no interactions with any use cases or other dependencies
        verifyNoInteractions(
            appEventGateway, setIsUnverifiedBusinessAccountUseCase,
            broadcastBusinessAccountExpiredUseCase, broadcastSslVerificationFailedUseCase,
            accountInfoWrapper, setLoggedOutFromAnotherLocationUseCase,
            localLogoutAppUseCase, loginMutex, broadcastFetchNodesFinishUseCase,
            getFullAccountInfoUseCase, shouldShowRichLinkWarningUseCase,
            isRichPreviewsEnabledUseCase, setupDeviceNameUseCase,
            updatePushNotificationSettingsUseCase, setupMegaChatApiWrapper
        )
    }

    @Test
    fun `test onRequestFinish with FETCH_NODES request and non-OK error does not execute success logic`() =
        runTest {
            val request = mock<MegaRequest> {
                on { type } doReturn MegaRequest.TYPE_FETCH_NODES
            }
            val api = mock<MegaApiJava>()
            val error = mock<MegaError> {
                on { errorCode } doReturn MegaError.API_EARGS
            }

            underTest.onRequestFinish(api, request, error)
            advanceUntilIdle()

            // Should still unlock mutex and broadcast finish
            verify(loginMutex).unlock()
            verify(broadcastFetchNodesFinishUseCase).invoke()

            // But should not execute success-specific logic
            verify(getFullAccountInfoUseCase, never()).invoke()
            verify(setupMegaChatApiWrapper, never()).invoke()
        }
} 